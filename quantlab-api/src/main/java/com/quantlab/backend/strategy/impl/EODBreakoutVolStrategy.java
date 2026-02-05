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
 * End-of-Day Breakout with Volume Confirmation Strategy.
 * <p>
 * Strategy Code: EMA_BREAKOUT
 * <p>
 * This strategy identifies breakouts from recent price highs with volume confirmation.
 * It's designed to capture momentum moves when price breaks out of consolidation
 * with strong volume support.
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Close price > highest high of lookback period</li>
 *   <li>Volume > volumeMultiplier × average volume of lookback period</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss = lowest low of lookback period</li>
 *   <li>Target = entry + 2 × (entry - stop) [2:1 reward-risk ratio]</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>lookbackDays: Number of days for high/low calculation (default: 20)</li>
 *   <li>volumeMultiplier: Minimum volume ratio (default: 1.5)</li>
 * </ul>
 */
@Component
@Qualifier("eodBreakoutVolStrategy")
public class EODBreakoutVolStrategy implements Strategy {

    private static final String CODE = "EMA_BREAKOUT";
    private static final String NAME = "EOD Breakout with Volume";

    @Override
    public List<TradeSignal> generateSignals(List<Candle> candles, StrategyParams params) {
        // Validate input
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        // Get parameters with defaults
        final int lookbackDays = params.getInt("lookbackDays", 20);
        final double volumeMultiplier = params.getDouble("volumeMultiplier", 1.5);

        // Validate parameters
        if (lookbackDays < 1) {
            throw new IllegalArgumentException("lookbackDays must be at least 1");
        }
        if (volumeMultiplier < 0) {
            throw new IllegalArgumentException("volumeMultiplier must be non-negative");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least lookbackDays + 1 candles (history + current day)
        if (candles.size() < lookbackDays + 1) {
            return signals; // Not enough data
        }

        // Start from index lookbackDays to have sufficient history
        for (int i = lookbackDays; i < candles.size(); i++) {
            Candle today = candles.get(i);

            // Skip if current candle has null values
            if (!isValidCandle(today)) {
                continue;
            }

            // Get historical window (excluding current candle)
            List<Candle> history = candles.subList(i - lookbackDays, i);

            // Calculate indicators
            OptionalDouble highestHigh = history.stream()
                .filter(this::isValidCandle)
                .mapToDouble(c -> c.getHigh().doubleValue())
                .max();

            OptionalDouble lowestLow = history.stream()
                .filter(this::isValidCandle)
                .mapToDouble(c -> c.getLow().doubleValue())
                .min();

            OptionalDouble avgVolume = history.stream()
                .filter(this::isValidCandle)
                .mapToLong(Candle::getVolume)
                .average();

            // Skip if we couldn't calculate indicators
            if (!highestHigh.isPresent() || !lowestLow.isPresent() || !avgVolume.isPresent()) {
                continue;
            }

            double currentClose = today.getClose().doubleValue();
            long currentVolume = today.getVolume();
            double requiredVolume = avgVolume.getAsDouble() * volumeMultiplier;

            // Check entry conditions
            boolean isBreakout = currentClose > highestHigh.getAsDouble();
            boolean hasVolumeConfirmation = currentVolume > requiredVolume;

            if (isBreakout && hasVolumeConfirmation) {
                // Calculate stop loss and target
                BigDecimal stopLoss = BigDecimal.valueOf(lowestLow.getAsDouble());
                BigDecimal entryPrice = today.getClose();
                BigDecimal risk = entryPrice.subtract(stopLoss);
                BigDecimal targetPrice = entryPrice.add(risk.multiply(BigDecimal.valueOf(2.0)));

                // Create signal
                TradeSignal signal = TradeSignal.fromCandle(
                    today,
                    Side.BUY,
                    entryPrice,
                    stopLoss,
                    targetPrice,
                    1  // Default quantity
                );

                signals.add(signal);
            }
        }

        return signals;
    }

    /**
     * Checks if a candle has valid price and volume data.
     */
    private boolean isValidCandle(Candle candle) {
        return candle != null &&
            candle.getOpen() != null &&
            candle.getHigh() != null &&
            candle.getLow() != null &&
            candle.getClose() != null &&
            candle.getVolume() != null &&
            candle.getVolume() > 0 &&
            candle.getHigh().doubleValue() > 0 &&
            candle.getLow().doubleValue() > 0 &&
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
        // Need at least lookbackDays + 1, using default of 21
        return 21;
    }

    @Override
    public String toString() {
        return "EODBreakoutVolStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
