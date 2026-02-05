package com.quantlab.backend.strategy.impl;

import com.quantlab.backend.domain.TradeSignal;
import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Side;
import com.quantlab.backend.strategy.Strategy;
import com.quantlab.backend.strategy.StrategyContext;
import com.quantlab.backend.strategy.StrategyParams;
import com.quantlab.backend.strategy.StrategyResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Failed Breakdown / Bear Trap Strategy.
 * <p>
 * Strategy Code: FAILED_BREAKDOWN
 * <p>
 * This strategy identifies bear traps - false breakdowns below support that
 * quickly reverse higher. These patterns often mark the beginning of strong
 * uptrends as weak hands are flushed out and smart money accumulates.
 * <p>
 * The pattern consists of two candles:
 * Day 1: Price breaks below support with high volume (appears bearish)
 * Day 2: Price recovers and closes above the broken support level (bullish trap)
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Day 1: Close below support level (previous swing low)</li>
 *   <li>Day 1: High volume (>= 1.5x average volume)</li>
 *   <li>Day 2: Close above the support level (failed breakdown confirmed)</li>
 *   <li>Day 2: Close > Open (bullish candle)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: Below Day 2's low (trap low)</li>
 *   <li>Target: Entry + 2.5x risk (2.5:1 reward-risk ratio)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>swingLookback: Period to identify support/swing low (default: 20)</li>
 *   <li>volumeMultiplier: Minimum volume multiplier for Day 1 (default: 1.5)</li>
 *   <li>volumeLookback: Period for average volume calculation (default: 20)</li>
 *   <li>riskRewardRatio: Target multiplier (default: 2.5)</li>
 * </ul>
 */
@Component
@Qualifier("failedBreakdownStrategy")
public class FailedBreakdownStrategy implements Strategy {

    private static final String CODE = "FAILED_BREAKDOWN";
    private static final String NAME = "Failed Breakdown (Bear Trap)";
    private static final int DEFAULT_SWING_LOOKBACK = 20;
    private static final double DEFAULT_VOLUME_MULTIPLIER = 1.5;
    private static final int DEFAULT_VOLUME_LOOKBACK = 20;
    private static final double DEFAULT_RISK_REWARD_RATIO = 2.5;

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int swingLookback = params.getInt("swingLookback", DEFAULT_SWING_LOOKBACK);
        final double volumeMultiplier = params.getDouble("volumeMultiplier", DEFAULT_VOLUME_MULTIPLIER);
        final int volumeLookback = params.getInt("volumeLookback", DEFAULT_VOLUME_LOOKBACK);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        if (swingLookback < 2) {
            throw new IllegalArgumentException("swingLookback must be at least 2");
        }
        if (volumeMultiplier <= 0) {
            throw new IllegalArgumentException("volumeMultiplier must be positive");
        }
        if (volumeLookback < 1) {
            throw new IllegalArgumentException("volumeLookback must be at least 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least max(swingLookback, volumeLookback) + 2 candles
        int minRequired = Math.max(swingLookback, volumeLookback) + 2;
        if (candles.size() < minRequired) {
            return StrategyResult.empty(context.isScreening());
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            int latestIndex = candles.size() - 1;
            TradeSignal signal = checkFailedBreakdown(candles, latestIndex,
                swingLookback, volumeMultiplier, volumeLookback, riskRewardRatio);
            if (signal != null) {
                return StrategyResult.actionable(List.of(signal));
            }
            return StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = minRequired - 1; i < candles.size(); i++) {
            TradeSignal signal = checkFailedBreakdown(candles, i,
                swingLookback, volumeMultiplier, volumeLookback, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return StrategyResult.nonActionable(signals);
    }

    /**
     * Checks for failed breakdown pattern at the given index.
     * Pattern is confirmed on Day 2, so we check the candle at 'index'
     * and the previous candle at 'index - 1'.
     */
    private TradeSignal checkFailedBreakdown(List<Candle> candles, int day2Index,
                                             int swingLookback, double volumeMultiplier,
                                             int volumeLookback, double riskRewardRatio) {
        if (day2Index < swingLookback + 1) {
            return null;
        }

        int day1Index = day2Index - 1;
        Candle day1 = candles.get(day1Index);
        Candle day2 = candles.get(day2Index);

        if (!isValidCandle(day1) || !isValidCandle(day2)) {
            return null;
        }

        // Find support level (lowest low in swingLookback period before Day 1)
        BigDecimal supportLevel = findSupportLevel(candles, day1Index - swingLookback, day1Index - 1);
        if (supportLevel == null) {
            return null;
        }

        // Calculate average volume
        double avgVolume = calculateAverageVolume(candles, day1Index - volumeLookback, day1Index - 1);

        // DAY 1 CONDITIONS: Breakdown below support

        // Check close below support
        if (day1.getClose().compareTo(supportLevel) >= 0) {
            return null;
        }

        // Check high volume on breakdown
        long day1Volume = day1.getVolume() != null ? day1.getVolume() : 0;
        if (day1Volume < avgVolume * volumeMultiplier) {
            return null;
        }

        // DAY 2 CONDITIONS: Recovery above support (failed breakdown)

        // Check bullish candle (close > open)
        if (day2.getClose().compareTo(day2.getOpen()) <= 0) {
            return null;
        }

        // Check close above support level (failed breakdown confirmed)
        if (day2.getClose().compareTo(supportLevel) <= 0) {
            return null;
        }

        // All conditions met - create signal
        BigDecimal entryPrice = day2.getClose();
        BigDecimal stopLoss = day2.getLow()
            .multiply(BigDecimal.valueOf(0.98))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

        return TradeSignal.fromCandle(day2, Side.BUY, entryPrice, stopLoss, targetPrice, 1);
    }

    /**
     * Finds the support level (lowest low) in the given range.
     */
    private BigDecimal findSupportLevel(List<Candle> candles, int startIndex, int endIndex) {
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(candles.size() - 1, endIndex);

        BigDecimal lowestLow = null;

        for (int i = startIndex; i <= endIndex; i++) {
            Candle c = candles.get(i);
            if (isValidCandle(c)) {
                if (lowestLow == null || c.getLow().compareTo(lowestLow) < 0) {
                    lowestLow = c.getLow();
                }
            }
        }

        return lowestLow;
    }

    /**
     * Calculates average volume over the given range.
     */
    private double calculateAverageVolume(List<Candle> candles, int startIndex, int endIndex) {
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(candles.size() - 1, endIndex);

        long totalVolume = 0;
        int validCandles = 0;

        for (int i = startIndex; i <= endIndex; i++) {
            Candle c = candles.get(i);
            if (c != null && c.getVolume() != null) {
                totalVolume += c.getVolume();
                validCandles++;
            }
        }

        return validCandles > 0 ? (double) totalVolume / validCandles : 0;
    }

    /**
     * Calculates target price based on risk-reward ratio.
     */
    private BigDecimal calculateTarget(BigDecimal entryPrice, BigDecimal stopLoss, double riskRewardRatio) {
        BigDecimal risk = entryPrice.subtract(stopLoss);
        BigDecimal reward = risk.multiply(BigDecimal.valueOf(riskRewardRatio));
        return entryPrice.add(reward).setScale(2, RoundingMode.HALF_UP);
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
            candle.getHigh().compareTo(candle.getLow()) >= 0;
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
        return Math.max(DEFAULT_SWING_LOOKBACK, DEFAULT_VOLUME_LOOKBACK) + 2;
    }

    @Override
    public String toString() {
        return "FailedBreakdownStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
