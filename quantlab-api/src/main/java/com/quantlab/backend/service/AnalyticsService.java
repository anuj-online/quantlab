package com.quantlab.backend.service;

import com.quantlab.backend.dto.AnalyticsResponse;
import com.quantlab.backend.dto.EquityCurvePoint;
import com.quantlab.backend.entity.StrategyRun;
import com.quantlab.backend.repository.PaperTradeRepository;
import com.quantlab.backend.repository.StrategyRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for analytics calculations.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final PaperTradeRepository paperTradeRepository;
    private final StrategyRunRepository strategyRunRepository;

    public AnalyticsService(
            PaperTradeRepository paperTradeRepository,
            StrategyRunRepository strategyRunRepository) {
        this.paperTradeRepository = paperTradeRepository;
        this.strategyRunRepository = strategyRunRepository;
    }

    /**
     * Get analytics summary for a strategy run.
     * Calculates total trades, win rate, total PnL, and max drawdown.
     *
     * @param runId the strategy run ID
     * @return the analytics response DTO
     */
    public AnalyticsResponse getAnalytics(Long runId) {
        // Verify the strategy run exists
        StrategyRun strategyRun = strategyRunRepository.findById(runId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Strategy run not found with ID: " + runId));

        // Get total trades count
        long totalTrades = paperTradeRepository.countByStrategyRunId(runId);

        // Get winning trades count
        long winningTrades = paperTradeRepository.countWinningTradesByStrategyRun(runId);

        // Calculate win rate
        BigDecimal winRate = BigDecimal.ZERO;
        if (totalTrades > 0) {
            winRate = BigDecimal.valueOf(winningTrades)
                    .divide(BigDecimal.valueOf(totalTrades), 4, RoundingMode.HALF_UP);
        }

        // Get total PnL
        BigDecimal totalPnl = paperTradeRepository.calculateTotalPnlByStrategyRun(runId);
        if (totalPnl == null) {
            totalPnl = BigDecimal.ZERO;
        }

        // Calculate max drawdown (placeholder implementation)
        // TODO: Implement proper max drawdown calculation based on equity curve
        BigDecimal maxDrawdown = calculateMaxDrawdown(runId);

        return new AnalyticsResponse(
                (int) totalTrades,
                winRate,
                totalPnl,
                maxDrawdown
        );
    }

    /**
     * Get the equity curve for a strategy run.
     * Returns a list of date-equity pairs representing the portfolio value over time.
     * For the initial implementation, this returns a placeholder.
     *
     * @param runId the strategy run ID
     * @return list of equity curve points
     */
    public List<EquityCurvePoint> getEquityCurve(Long runId) {
        // Verify the strategy run exists
        StrategyRun strategyRun = strategyRunRepository.findById(runId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Strategy run not found with ID: " + runId));

        // TODO: Implement proper equity curve calculation
        // This would involve:
        // 1. Starting with initial capital
        // 2. Processing trades in chronological order
        // 3. Calculating running portfolio value at each trade
        // 4. Returning date/value pairs for significant points

        // Placeholder: Return empty list
        return new ArrayList<>();

        // Example implementation structure:
        // List<EquityCurvePoint> equityCurve = new ArrayList<>();
        // BigDecimal initialCapital = new BigDecimal("100000");
        // BigDecimal currentEquity = initialCapital;
        // equityCurve.add(new EquityCurvePoint(strategyRun.getStartDate().toString(), initialCapital));
        //
        // List<PaperTrade> trades = paperTradeRepository.findByStrategyRunIdOrderByEntryDateAsc(runId);
        // for (PaperTrade trade : trades) {
        //     if (trade.getPnl() != null) {
        //         currentEquity = currentEquity.add(trade.getPnl());
        //         equityCurve.add(new EquityCurvePoint(trade.getExitDate().toString(), currentEquity));
        //     }
        // }
        // return equityCurve;
    }

    /**
     * Calculate the maximum drawdown for a strategy run.
     * This is a placeholder implementation that returns zero.
     * A proper implementation would calculate the maximum peak-to-trough decline.
     *
     * @param runId the strategy run ID
     * @return the maximum drawdown as a decimal (e.g., -0.18 for -18%)
     */
    private BigDecimal calculateMaxDrawdown(Long runId) {
        // TODO: Implement proper max drawdown calculation
        // This would require:
        // 1. Building the equity curve
        // 2. Tracking the running maximum (peak)
        // 3. Finding the maximum decline from any peak
        // 4. Returning as a percentage of the peak

        return BigDecimal.ZERO;
    }
}
