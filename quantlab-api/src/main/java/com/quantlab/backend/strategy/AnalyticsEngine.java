package com.quantlab.backend.strategy;

import com.quantlab.backend.dto.AnalyticsResponse;
import com.quantlab.backend.dto.EquityCurvePoint;
import com.quantlab.backend.entity.PaperTrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Analytics engine for calculating performance metrics from paper trades.
 * <p>
 * This component provides core analytics calculations for trading strategy evaluation,
 * including win rate, total PnL, maximum drawdown, and equity curve generation.
 * All calculations use BigDecimal with DECIMAL128 precision for accurate financial computations.
 * <p>
 * Key metrics:
 * <ul>
 *   <li>Total Trades: Count of all completed trades</li>
 *   <li>Win Rate: Percentage of trades with positive PnL</li>
 *   <li>Total PnL: Sum of all trade profits/losses</li>
 *   <li>Max Drawdown: Maximum peak-to-trough decline in portfolio value</li>
 * </ul>
 */
@Component
public class AnalyticsEngine {

    /**
     * Default initial capital for strategy runs when not explicitly provided.
     * Represents a standard starting portfolio value of $100,000.
     */
    private static final BigDecimal DEFAULT_INITIAL_CAPITAL = new BigDecimal("100000");

    /**
     * Math context for high-precision financial calculations.
     * DECIMAL128 provides 34 digits of precision, sufficient for all trading calculations.
     */
    private static final MathContext MC = MathContext.DECIMAL128;

    /**
     * Scale for percentage calculations (4 decimal places).
     */
    private static final int SCALE = 4;

    /**
     * Calculate comprehensive analytics summary for a list of paper trades.
     * <p>
     * This method computes the core performance metrics used in strategy evaluation:
     * <ul>
     *   <li>Total number of trades executed</li>
     *   <li>Win rate (percentage of profitable trades)</li>
     *   <li>Total PnL across all trades</li>
     *   <li>Maximum drawdown from equity curve</li>
     * </ul>
     *
     * @param trades         List of completed paper trades to analyze
     * @param initialCapital Starting capital for the strategy run (null defaults to $100,000)
     * @return AnalyticsResponse containing calculated metrics
     * @throws IllegalArgumentException if trades list is null
     */
    public AnalyticsResponse calculate(List<PaperTrade> trades, BigDecimal initialCapital) {
        if (trades == null) {
            throw new IllegalArgumentException("Trades list cannot be null");
        }

        // Use default initial capital if not provided
        BigDecimal capital = initialCapital != null ? initialCapital : DEFAULT_INITIAL_CAPITAL;

        // Calculate total trades
        int totalTrades = trades.size();

        // Calculate win rate: count trades with PnL > 0
        long winningTrades = trades.stream()
                .filter(t -> t.getPnl() != null && t.getPnl().compareTo(BigDecimal.ZERO) > 0)
                .count();

        BigDecimal winRate = BigDecimal.ZERO;
        if (totalTrades > 0) {
            winRate = new BigDecimal(winningTrades)
                    .divide(new BigDecimal(totalTrades), SCALE, RoundingMode.HALF_UP);
        }

        // Calculate total PnL
        BigDecimal totalPnl = trades.stream()
                .map(PaperTrade::getPnl)
                .filter(pnl -> pnl != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate max drawdown from equity curve
        List<EquityCurvePoint> equityCurve = calculateEquityCurve(trades, capital);
        BigDecimal maxDrawdown = calculateMaxDrawdownFromEquityCurve(equityCurve, capital);

        return new AnalyticsResponse(totalTrades, winRate, totalPnl, maxDrawdown);
    }

    /**
     * Calculate the equity curve for a series of trades.
     * <p>
     * The equity curve represents the portfolio value over time as trades are closed.
     * Each point on the curve represents the portfolio value after a trade's exit.
     * <p>
     * Algorithm:
     * <pre>
     * capital = initialCapital
     * For each trade (ordered by exitDate):
     *     capital += trade.pnl
     *     Record (exitDate, capital)
     * </pre>
     *
     * @param trades         List of completed paper trades (must not be null)
     * @param initialCapital Starting portfolio value
     * @return List of equity curve points ordered by exit date
     * @throws IllegalArgumentException if trades list is null
     */
    public List<EquityCurvePoint> calculateEquityCurve(List<PaperTrade> trades, BigDecimal initialCapital) {
        if (trades == null) {
            throw new IllegalArgumentException("Trades list cannot be null");
        }

        // Use default initial capital if not provided
        BigDecimal capital = initialCapital != null ? initialCapital : DEFAULT_INITIAL_CAPITAL;

        // Sort trades by exit date for chronological processing
        List<PaperTrade> sortedTrades = trades.stream()
                .filter(t -> t.getExitDate() != null)
                .sorted(Comparator.comparing(PaperTrade::getExitDate))
                .toList();

        List<EquityCurvePoint> equityCurve = new ArrayList<>();
        BigDecimal currentEquity = capital;

        // Build equity curve: for each trade exit, add the PnL to running capital
        for (PaperTrade trade : sortedTrades) {
            BigDecimal pnl = trade.getPnl();
            if (pnl != null) {
                currentEquity = currentEquity.add(pnl, MC);
                equityCurve.add(new EquityCurvePoint(
                        trade.getExitDate().toString(),
                        currentEquity.setScale(SCALE, RoundingMode.HALF_UP)
                ));
            }
        }

        return equityCurve;
    }

    /**
     * Calculate maximum drawdown from an equity curve.
     * <p>
     * Maximum drawdown measures the largest peak-to-trough decline in portfolio value,
     * expressed as a percentage of the peak value. This is a critical risk metric
     * that indicates the worst-case loss experienced during the trading period.
     * <p>
     * Algorithm:
     * <pre>
     * peak = initialCapital
     * maxDrawdown = 0
     * For each equity point:
     *     if equity > peak: peak = equity
     *     drawdown = (peak - equity) / peak
     *     if drawdown > maxDrawdown: maxDrawdown = drawdown
     * </pre>
     *
     * @param equityCurve    List of equity points ordered chronologically
     * @param initialCapital Starting portfolio value (initial peak)
     * @return Maximum drawdown as a decimal (e.g., 0.18 for 18% drawdown)
     */
    private BigDecimal calculateMaxDrawdownFromEquityCurve(
            List<EquityCurvePoint> equityCurve,
            BigDecimal initialCapital) {

        if (equityCurve.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal peak = initialCapital;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (EquityCurvePoint point : equityCurve) {
            BigDecimal equity = point.getEquity();

            // Update peak if we've reached a new high
            if (equity.compareTo(peak) > 0) {
                peak = equity;
            }

            // Calculate drawdown from current peak
            BigDecimal drawdown = peak.subtract(equity, MC)
                    .divide(peak, SCALE, RoundingMode.HALF_UP);

            // Track maximum drawdown
            if (drawdown.compareTo(maxDrawdown) > 0) {
                maxDrawdown = drawdown;
            }
        }

        return maxDrawdown.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Get the default initial capital constant.
     * Used by services that need to reference the standard starting value.
     *
     * @return Default initial capital value ($100,000)
     */
    public BigDecimal getDefaultInitialCapital() {
        return DEFAULT_INITIAL_CAPITAL;
    }
}
