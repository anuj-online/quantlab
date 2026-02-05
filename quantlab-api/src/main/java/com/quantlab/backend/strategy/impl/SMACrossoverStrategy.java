package com.quantlab.backend.strategy.impl;

import com.quantlab.backend.domain.TradeSignal;
import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Side;
import com.quantlab.backend.strategy.Strategy;
import com.quantlab.backend.strategy.StrategyParams;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Simple Moving Average (SMA) Crossover Strategy.
 * <p>
 * Strategy Code: SMA_CROSSOVER
 * <p>
 * This strategy generates buy signals when a faster-moving average crosses above
 * a slower-moving average, indicating potential upward momentum. This is a classic
 * trend-following strategy that aims to capture the beginning of sustained uptrends.
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Previous day: fast SMA <= slow SMA (no crossover yet)</li>
 *   <li>Current day: fast SMA > slow SMA (crossover occurred)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: Not set by this strategy (trend following without fixed stop)</li>
 *   <li>Target: Not set by this strategy (exit on opposite crossover)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>fastSMA: Period for fast moving average (default: 5)</li>
 *   <li>slowSMA: Period for slow moving average (default: 20)</li>
 * </ul>
 * <p>
 * Notes:
 * <ul>
 *   <li>Fast SMA should be less than slow SMA for meaningful crossovers</li>
 *   <li>This strategy only generates BUY signals (bullish crossovers)</li>
 *   <li>Consider implementing exit logic on bearish crossovers for complete system</li>
 * </ul>
 */
@Component
@Qualifier("smaCrossoverStrategy")
public class SMACrossoverStrategy implements Strategy {

    private static final String CODE = "SMA_CROSSOVER";
    private static final String NAME = "SMA Crossover";

    @Override
    public List<TradeSignal> generateSignals(List<Candle> candles, StrategyParams params) {
        // Validate input
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        // Get parameters with defaults
        final int fastPeriod = params.getInt("fastSMA", 5);
        final int slowPeriod = params.getInt("slowSMA", 20);

        // Validate parameters
        if (fastPeriod < 1) {
            throw new IllegalArgumentException("fastSMA must be at least 1");
        }
        if (slowPeriod < 1) {
            throw new IllegalArgumentException("slowSMA must be at least 1");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException("fastSMA must be less than slowSMA for meaningful crossovers");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least slowPeriod + 1 candles to detect crossover
        if (candles.size() < slowPeriod + 1) {
            return signals; // Not enough data
        }

        // Calculate SMAs for each position and detect crossovers
        for (int i = slowPeriod; i < candles.size(); i++) {
            Candle today = candles.get(i);
            Candle yesterday = candles.get(i - 1);

            // Skip if candles have null values
            if (!isValidCandle(today) || !isValidCandle(yesterday)) {
                continue;
            }

            // Calculate SMAs for current and previous day
            OptionalDouble currentFastSMA = calculateSMA(candles, i, fastPeriod);
            OptionalDouble currentSlowSMA = calculateSMA(candles, i, slowPeriod);
            OptionalDouble prevFastSMA = calculateSMA(candles, i - 1, fastPeriod);
            OptionalDouble prevSlowSMA = calculateSMA(candles, i - 1, slowPeriod);

            // Skip if we couldn't calculate SMAs
            if (!currentFastSMA.isPresent() || !currentSlowSMA.isPresent() ||
                !prevFastSMA.isPresent() || !prevSlowSMA.isPresent()) {
                continue;
            }

            // Check for bullish crossover
            // Previous: fast <= slow (no uptrend)
            // Current: fast > slow (uptrend started)
            boolean wasBelowOrEqual = prevFastSMA.getAsDouble() <= prevSlowSMA.getAsDouble();
            boolean isNowAbove = currentFastSMA.getAsDouble() > currentSlowSMA.getAsDouble();

            if (wasBelowOrEqual && isNowAbove) {
                // Generate buy signal at close price
                TradeSignal signal = TradeSignal.simpleBuy(
                    today.getInstrument(),
                    today.getTradeDate(),
                    today.getClose(),
                    1  // Default quantity
                );

                signals.add(signal);
            }
        }

        return signals;
    }

    /**
     * Calculates Simple Moving Average for the candle at position endIndex,
     * looking back 'period' candles.
     *
     * @param candles    List of candles
     * @param endIndex   Index of current candle
     * @param period     Number of periods for SMA
     * @return Optional containing SMA value, or empty if calculation fails
     */
    private OptionalDouble calculateSMA(List<Candle> candles, int endIndex, int period) {
        if (endIndex < period - 1) {
            return OptionalDouble.empty();
        }

        int startIndex = endIndex - period + 1;

        return candles.subList(startIndex, endIndex + 1).stream()
            .filter(this::isValidCandle)
            .mapToDouble(c -> c.getClose().doubleValue())
            .average();
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
            candle.getClose().doubleValue() > 0;
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
        // Need at least slowPeriod + 1, using default of 21
        return 21;
    }

    @Override
    public String toString() {
        return "SMACrossoverStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
