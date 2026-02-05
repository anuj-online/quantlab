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
 * High Volume Reversal Strategy.
 * <p>
 * Strategy Code: HV_REVERSAL
 * <p>
 * This strategy identifies potential bullish reversals after a high-volume
 * sell-off. The pattern consists of two candles:
 * <p>
 * Day 1: High volume selling climaxes with close near the low
 * Day 2: Bullish response showing rejection of lower prices
 * <p>
 * This pattern often marks exhaustion of selling pressure and the beginning
 * of a new uptrend. The high volume on Day 1 represents capitulation, while
 * the bullish close on Day 2 shows buyers stepping in.
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Day 1: Volume >= 2x average volume (selling climax)</li>
 *   <li>Day 1: Close in lower portion of range (close position < 0.3)</li>
 *   <li>Day 2: Close > Open (bullish candle)</li>
 *   <li>Day 2: Close higher than Day 1's open (recovery begun)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: Below Day 1's low (selling climax low)</li>
 *   <li>Target: Entry + 2x risk (2:1 reward-risk ratio)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>volumeMultiplier: Minimum volume multiplier for Day 1 (default: 2.0)</li>
 *   <li>volumeLookback: Period for average volume calculation (default: 20)</li>
 *   <li>closePositionThreshold: Max close position for Day 1 (default: 0.3 = 30%)</li>
 *   <li>riskRewardRatio: Target multiplier (default: 2.0)</li>
 * </ul>
 */
@Component
@Qualifier("highVolumeReversalStrategy")
public class HighVolumeReversalStrategy implements Strategy {

    private static final String CODE = "HV_REVERSAL";
    private static final String NAME = "High Volume Reversal";
    private static final double DEFAULT_VOLUME_MULTIPLIER = 2.0;
    private static final int DEFAULT_VOLUME_LOOKBACK = 20;
    private static final double DEFAULT_CLOSE_POSITION_THRESHOLD = 0.3; // 30%
    private static final double DEFAULT_RISK_REWARD_RATIO = 2.0;

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final double volumeMultiplier = params.getDouble("volumeMultiplier", DEFAULT_VOLUME_MULTIPLIER);
        final int volumeLookback = params.getInt("volumeLookback", DEFAULT_VOLUME_LOOKBACK);
        final double closePositionThreshold = params.getDouble("closePositionThreshold", DEFAULT_CLOSE_POSITION_THRESHOLD);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        if (volumeMultiplier <= 0) {
            throw new IllegalArgumentException("volumeMultiplier must be positive");
        }
        if (volumeLookback < 1) {
            throw new IllegalArgumentException("volumeLookback must be at least 1");
        }
        if (closePositionThreshold <= 0 || closePositionThreshold > 1) {
            throw new IllegalArgumentException("closePositionThreshold must be between 0 and 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least volumeLookback + 2 candles
        int minRequired = volumeLookback + 2;
        if (candles.size() < minRequired) {
            return StrategyResult.empty(context.isScreening());
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            int latestIndex = candles.size() - 1;
            TradeSignal signal = checkReversalPattern(candles, latestIndex,
                volumeMultiplier, volumeLookback, closePositionThreshold, riskRewardRatio);
            if (signal != null) {
                return StrategyResult.actionable(List.of(signal));
            }
            return StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = volumeLookback + 1; i < candles.size(); i++) {
            TradeSignal signal = checkReversalPattern(candles, i,
                volumeMultiplier, volumeLookback, closePositionThreshold, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return StrategyResult.nonActionable(signals);
    }

    /**
     * Checks for high volume reversal pattern at the given index.
     * Pattern is confirmed on Day 2, so we check the candle at 'index'
     * and the previous candle at 'index - 1'.
     */
    private TradeSignal checkReversalPattern(List<Candle> candles, int day2Index,
                                             double volumeMultiplier, int volumeLookback,
                                             double closePositionThreshold, double riskRewardRatio) {
        if (day2Index < volumeLookback + 1) {
            return null;
        }

        int day1Index = day2Index - 1;
        Candle day1 = candles.get(day1Index);
        Candle day2 = candles.get(day2Index);

        if (!isValidCandle(day1) || !isValidCandle(day2)) {
            return null;
        }

        // Calculate average volume
        double avgVolume = calculateAverageVolume(candles, day1Index - volumeLookback, day1Index - 1);

        // DAY 1 CONDITIONS: Selling climax

        // Check high volume (>= volumeMultiplier * average)
        long day1Volume = day1.getVolume() != null ? day1.getVolume() : 0;
        if (day1Volume < avgVolume * volumeMultiplier) {
            return null;
        }

        // Check close in lower portion of range
        double day1Range = day1.getHigh().subtract(day1.getLow()).doubleValue();
        if (day1Range <= 0) {
            return null;
        }
        double day1ClosePosition = (day1.getClose().subtract(day1.getLow()).doubleValue()) / day1Range;
        if (day1ClosePosition >= closePositionThreshold) {
            return null;
        }

        // DAY 2 CONDITIONS: Bullish response

        // Check bullish candle (close > open)
        if (day2.getClose().compareTo(day2.getOpen()) <= 0) {
            return null;
        }

        // Check recovery (close higher than day 1's open)
        if (day2.getClose().compareTo(day1.getOpen()) <= 0) {
            return null;
        }

        // All conditions met - create signal
        BigDecimal entryPrice = day2.getClose();
        BigDecimal stopLoss = day1.getLow()
            .multiply(BigDecimal.valueOf(0.98))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

        return TradeSignal.fromCandle(day2, Side.BUY, entryPrice, stopLoss, targetPrice, 1);
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
        return DEFAULT_VOLUME_LOOKBACK + 2;
    }

    @Override
    public String toString() {
        return "HighVolumeReversalStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
