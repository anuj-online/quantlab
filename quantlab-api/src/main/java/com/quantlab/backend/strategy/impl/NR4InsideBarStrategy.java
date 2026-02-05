package com.quantlab.backend.strategy.impl;

import com.quantlab.backend.domain.TradeSignal;
import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Side;
import com.quantlab.backend.strategy.ExecutionMode;
import com.quantlab.backend.strategy.Strategy;
import com.quantlab.backend.strategy.StrategyContext;
import com.quantlab.backend.strategy.StrategyParams;
import com.quantlab.backend.strategy.StrategyResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * NR4 + Inside Bar Volatility Contraction Strategy.
 * <p>
 * Strategy Code: NR4_INSIDE_BAR
 * <p>
 * This strategy identifies volatility contraction patterns that often precede
 * significant price breakouts. It combines two powerful price action concepts:
 * <p>
 * 1. NR4 (Narrow Range 4): Today's trading range is the smallest of the last 4 days.
 * This indicates decreasing volatility and potential consolidation before a breakout.
 * <p>
 * 2. Inside Bar: Today's high is <= yesterday's high AND today's low is >= yesterday's low.
 * This shows price is contained within the previous day's range, indicating indecision.
 * <p>
 * When both conditions occur together, they signal a strong volatility squeeze
 * that often leads to an explosive move in either direction.
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Today's range (high - low) is the smallest of the last 4 days (NR4)</li>
 *   <li>Today is an inside bar compared to yesterday</li>
 *   <li>Both conditions must be true simultaneously</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss = Today's low (for long breakout) or Today's high (for short)</li>
 *   <li>Target = Entry + 2x range (for long) or Entry - 2x range (for short)</li>
 * </ul>
 * <p>
 * Signal Type:
 * <ul>
 *   <li>For screening mode: Emits a "VOLATILITY SQUEEZE" watchlist signal</li>
 *   <li>For backtest mode: Emits directional signals based on breakout direction</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>lookbackDays: Number of days to check for NR4 pattern (default: 4)</li>
 *   <li>requireInsideBar: Whether inside bar is required (default: true)</li>
 * </ul>
 * <p>
 * References:
 * <ul>
 *   <li>Linda Bradford Raschke's NR4 pattern</li>
 *   <li>Inside Bar price action setup</li>
 * </ul>
 */
@Component
@Qualifier("nr4InsideBarStrategy")
public class NR4InsideBarStrategy implements Strategy {

    private static final String CODE = "NR4_INSIDE_BAR";
    private static final String NAME = "NR4 + Inside Bar Volatility Squeeze";

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        ExecutionMode mode = context.getMode();
        StrategyParams params = context.getParams();

        // Validate input
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        // Get parameters with defaults
        final int lookbackDays = params.getInt("lookbackDays", 4);
        final boolean requireInsideBar = params.getBoolean("requireInsideBar", true);

        // Validate parameters
        if (lookbackDays < 2) {
            throw new IllegalArgumentException("lookbackDays must be at least 2");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least lookbackDays + 1 candles (to check NR4 and inside bar)
        if (candles.size() < lookbackDays + 1) {
            return StrategyResult.empty(mode == ExecutionMode.SCREEN);
        }

        // In SCREEN mode, only check the latest candle
        // In BACKTEST mode, check all candles for historical signals
        if (mode == ExecutionMode.SCREEN) {
            // Only evaluate the latest candle for screening
            int latestIndex = candles.size() - 1;
            Candle today = candles.get(latestIndex);
            Candle yesterday = candles.get(latestIndex - 1);

            if (isValidCandle(today) && isValidCandle(yesterday)) {
                boolean isNR = checkNRPattern(today, candles, latestIndex, lookbackDays);
                boolean isInsideBar = checkInsideBar(today, yesterday);

                if (isNR && (!requireInsideBar || isInsideBar)) {
                    signals.add(createVolatilitySqueezeSignal(today));
                }
            }
        } else {
            // BACKTEST mode - process all candles
            for (int i = lookbackDays; i < candles.size(); i++) {
                Candle today = candles.get(i);
                Candle yesterday = candles.get(i - 1);

                // Skip if current candle has null values
                if (!isValidCandle(today) || !isValidCandle(yesterday)) {
                    continue;
                }

                // Check NR4 condition: today's range is the smallest of last N days
                boolean isNR = checkNRPattern(today, candles, i, lookbackDays);

                // Check Inside Bar condition
                boolean isInsideBar = checkInsideBar(today, yesterday);

                // Generate signal based on conditions
                if (isNR && (!requireInsideBar || isInsideBar)) {
                    signals.add(createVolatilitySqueezeSignal(today));
                }
            }
        }

        // In SCREEN mode, signals are actionable for next trading day
        // In BACKTEST mode, signals are historical (not actionable)
        return new StrategyResult(signals, mode == ExecutionMode.SCREEN && !signals.isEmpty());
    }

    /**
     * Creates a volatility squeeze signal with appropriate risk levels.
     *
     * @param today The candle that triggered the signal
     * @return A TradeSignal with entry, stop loss, and target
     */
    private TradeSignal createVolatilitySqueezeSignal(Candle today) {
        double todayRange = calculateRange(today);
        BigDecimal closePrice = today.getClose();
        BigDecimal lowPrice = today.getLow();

        // Calculate risk levels
        // Stop loss at today's low (breakdown level)
        BigDecimal stopLoss = lowPrice;
        // Target at close + 2x range (for potential upside breakout)
        BigDecimal range = BigDecimal.valueOf(todayRange);
        BigDecimal targetPrice = closePrice.add(range.multiply(BigDecimal.valueOf(2.0)));

        // Create a BUY signal for the potential upside breakout
        // In screening mode, this acts as a watchlist item
        return TradeSignal.fromCandle(
            today,
            Side.BUY,
            closePrice,
            stopLoss,
            targetPrice,
            1  // Default quantity
        );
    }

    /**
     * Checks if today's range is the smallest of the last N days (NR pattern).
     *
     * @param today   The current candle
     * @param candles List of all candles
     * @param index   Index of today in the candles list
     * @param n       Number of days to check
     * @return true if today's range is the smallest
     */
    private boolean checkNRPattern(Candle today, List<Candle> candles, int index, int n) {
        double todayRange = calculateRange(today);

        // Check lookback window
        for (int j = 1; j <= n; j++) {
            if (index - j < 0) {
                return false; // Not enough historical data
            }

            Candle historicalCandle = candles.get(index - j);
            if (!isValidCandle(historicalCandle)) {
                return false;
            }

            double historicalRange = calculateRange(historicalCandle);

            // Today's range must be strictly smaller
            if (todayRange >= historicalRange) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if today is an inside bar compared to yesterday.
     * Inside Bar: Today's high <= yesterday's high AND today's low >= yesterday's low
     *
     * @param today     Today's candle
     * @param yesterday Yesterday's candle
     * @return true if inside bar pattern exists
     */
    private boolean checkInsideBar(Candle today, Candle yesterday) {
        double todayHigh = today.getHigh().doubleValue();
        double todayLow = today.getLow().doubleValue();
        double yesterdayHigh = yesterday.getHigh().doubleValue();
        double yesterdayLow = yesterday.getLow().doubleValue();

        // Inside bar: today's range is contained within yesterday's range
        return todayHigh <= yesterdayHigh && todayLow >= yesterdayLow;
    }

    /**
     * Calculates the range of a candle (high - low).
     *
     * @param candle The candle to calculate range for
     * @return The range as a double
     */
    private double calculateRange(Candle candle) {
        return candle.getHigh().doubleValue() - candle.getLow().doubleValue();
    }

    /**
     * Checks if a candle has valid price data.
     */
    private boolean isValidCandle(Candle candle) {
        return candle != null &&
            candle.getOpen() != null &&
            candle.getHigh() != null &&
            candle.getLow() != null &&
            candle.getClose() != null &&
            candle.getHigh().doubleValue() > 0 &&
            candle.getLow().doubleValue() > 0 &&
            candle.getClose().doubleValue() > 0 &&
            candle.getHigh().doubleValue() >= candle.getLow().doubleValue();
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getMinCandlesRequired() {
        // Need at least lookbackDays + 1, using default of 5
        return 5;
    }

    @Override
    public String toString() {
        return "NR4InsideBarStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
