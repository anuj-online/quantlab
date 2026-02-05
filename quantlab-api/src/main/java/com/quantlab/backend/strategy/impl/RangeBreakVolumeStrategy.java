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
 * Volume Expansion Breakout Strategy.
 * <p>
 * Strategy Code: RANGE_BREAK_VOL
 * <p>
 * This strategy identifies breakouts from tight trading ranges with strong
 * volume confirmation. Tight ranges followed by volume expansion often
 * precede significant directional moves.
 * <p>
 * The strategy works by:
 * 1. Detecting a tight trading range over N days (low volatility)
 * 2. Identifying breakout above the range high
 * 3. Confirming with volume expansion (>= 2x average volume)
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Tight range defined over 'rangePeriod' days (high - low is small)</li>
 *   <li>Price breaks above the range high</li>
 *   <li>Volume is >= 'volumeMultiplier' times average volume (default: 2x)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: Below the range low (breakdown level)</li>
 *   <li>Target: Entry + 2x range width (measured move)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>rangePeriod: Period to define the tight range (default: 10)</li>
 *   <li>volumeMultiplier: Minimum volume multiplier for confirmation (default: 2.0)</li>
 *   <li>rangeWidthThreshold: Max range width as % of price (default: 0.05 = 5%)</li>
 *   <li>riskRewardRatio: Target multiplier relative to range width (default: 2.0)</li>
 * </ul>
 */
@Component
@Qualifier("rangeBreakVolumeStrategy")
public class RangeBreakVolumeStrategy implements Strategy {

    private static final String CODE = "RANGE_BREAK_VOL";
    private static final String NAME = "Volume Expansion Breakout";
    private static final int DEFAULT_RANGE_PERIOD = 10;
    private static final double DEFAULT_VOLUME_MULTIPLIER = 2.0;
    private static final double DEFAULT_RANGE_WIDTH_THRESHOLD = 0.05; // 5%
    private static final double DEFAULT_RISK_REWARD_RATIO = 2.0;

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int rangePeriod = params.getInt("rangePeriod", DEFAULT_RANGE_PERIOD);
        final double volumeMultiplier = params.getDouble("volumeMultiplier", DEFAULT_VOLUME_MULTIPLIER);
        final double rangeWidthThreshold = params.getDouble("rangeWidthThreshold", DEFAULT_RANGE_WIDTH_THRESHOLD);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        if (rangePeriod < 2) {
            throw new IllegalArgumentException("rangePeriod must be at least 2");
        }
        if (volumeMultiplier <= 0) {
            throw new IllegalArgumentException("volumeMultiplier must be positive");
        }
        if (rangeWidthThreshold <= 0 || rangeWidthThreshold >= 1) {
            throw new IllegalArgumentException("rangeWidthThreshold must be between 0 and 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least rangePeriod + 1 candles
        int minRequired = rangePeriod + 1;
        if (candles.size() < minRequired) {
            return StrategyResult.empty(context.isScreening());
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            int latestIndex = candles.size() - 1;
            TradeSignal signal = checkRangeBreakout(candles, latestIndex, rangePeriod,
                volumeMultiplier, rangeWidthThreshold, riskRewardRatio);
            if (signal != null) {
                return StrategyResult.actionable(List.of(signal));
            }
            return StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = rangePeriod; i < candles.size(); i++) {
            TradeSignal signal = checkRangeBreakout(candles, i, rangePeriod,
                volumeMultiplier, rangeWidthThreshold, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return StrategyResult.nonActionable(signals);
    }

    /**
     * Checks for range breakout with volume confirmation at the given index.
     */
    private TradeSignal checkRangeBreakout(List<Candle> candles, int index,
                                           int rangePeriod, double volumeMultiplier,
                                           double rangeWidthThreshold, double riskRewardRatio) {
        if (index < rangePeriod) {
            return null;
        }

        Candle today = candles.get(index);
        if (!isValidCandle(today)) {
            return null;
        }

        // Calculate range statistics over the range period
        RangeStats rangeStats = calculateRangeStats(candles, index - rangePeriod, index - 1);
        if (rangeStats == null) {
            return null;
        }

        // Check if range is tight enough
        double avgPrice = (rangeStats.highestHigh + rangeStats.lowestLow) / 2;
        double rangeWidthPercent = (rangeStats.highestHigh - rangeStats.lowestLow) / avgPrice;

        if (rangeWidthPercent > rangeWidthThreshold) {
            return null; // Range is too wide
        }

        // Check for breakout above range high
        if (today.getClose().compareTo(BigDecimal.valueOf(rangeStats.highestHigh)) <= 0) {
            return null;
        }

        // Check volume confirmation
        long currentVolume = today.getVolume() != null ? today.getVolume() : 0;
        if (currentVolume < rangeStats.avgVolume * volumeMultiplier) {
            return null;
        }

        // All conditions met - create signal
        BigDecimal entryPrice = today.getClose();
        BigDecimal stopLoss = BigDecimal.valueOf(rangeStats.lowestLow)
            .multiply(BigDecimal.valueOf(0.98))
            .setScale(2, RoundingMode.HALF_UP);

        // Target based on range width
        BigDecimal rangeWidth = BigDecimal.valueOf(rangeStats.highestHigh - rangeStats.lowestLow);
        BigDecimal targetMove = rangeWidth.multiply(BigDecimal.valueOf(riskRewardRatio));
        BigDecimal targetPrice = entryPrice.add(targetMove)
            .setScale(2, RoundingMode.HALF_UP);

        return TradeSignal.fromCandle(today, Side.BUY, entryPrice, stopLoss, targetPrice, 1);
    }

    /**
     * Calculates range statistics over the given period.
     */
    private RangeStats calculateRangeStats(List<Candle> candles, int startIndex, int endIndex) {
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(candles.size() - 1, endIndex);

        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        long totalVolume = 0;
        int validCandles = 0;

        for (int i = startIndex; i <= endIndex; i++) {
            Candle c = candles.get(i);
            if (!isValidCandle(c)) {
                continue;
            }

            if (c.getHigh() != null && c.getHigh().doubleValue() > highestHigh) {
                highestHigh = c.getHigh().doubleValue();
            }
            if (c.getLow() != null && c.getLow().doubleValue() < lowestLow) {
                lowestLow = c.getLow().doubleValue();
            }
            if (c.getVolume() != null) {
                totalVolume += c.getVolume();
            }
            validCandles++;
        }

        if (validCandles == 0) {
            return null;
        }

        double avgVolume = (double) totalVolume / validCandles;
        return new RangeStats(highestHigh, lowestLow, avgVolume);
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
            candle.getClose().doubleValue() > 0 &&
            candle.getHigh().doubleValue() > 0 &&
            candle.getLow().doubleValue() > 0;
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
        return DEFAULT_RANGE_PERIOD + 1;
    }

    @Override
    public String toString() {
        return "RangeBreakVolumeStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }

    /**
     * Data class to hold range statistics.
     */
    private static class RangeStats {
        final double highestHigh;
        final double lowestLow;
        final double avgVolume;

        RangeStats(double highestHigh, double lowestLow, double avgVolume) {
            this.highestHigh = highestHigh;
            this.lowestLow = lowestLow;
            this.avgVolume = avgVolume;
        }
    }
}
