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
 * 3-Day Momentum Burst Strategy.
 * <p>
 * Strategy Code: MOMENTUM_3D
 * <p>
 * This strategy identifies strong bullish momentum bursts characterized by:
 * 1. Three consecutive bullish candles (close > open each day)
 * 2. Increasing volume over the 3-day period
 * 3. Close price exceeds the 10-day high
 * <p>
 * This pattern indicates strong buying pressure and potential continuation
 * of the uptrend. The increasing volume confirms institutional participation,
 * while the break above the 10-day high signals a breakout from consolidation.
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Three consecutive bullish candles (close > open)</li>
 *   <li>Volume trend: day 3 > day 2 > day 1 (or at least non-decreasing)</li>
 *   <li>Current close > highest high in last 10 days</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: Below the 3-day low (support level)</li>
 *   <li>Target: Entry + 2x risk (2:1 reward-risk ratio)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>lookbackDays: Period for breakout confirmation (default: 10)</li>
 *   <li>requireStrictVolumeIncrease: Require strictly increasing volume (default: false)</li>
 *   <li>riskRewardRatio: Target multiplier (default: 2.0)</li>
 * </ul>
 */
@Component
@Qualifier("momentum3DayStrategy")
public class Momentum3DayStrategy implements Strategy {

    private static final String CODE = "MOMENTUM_3D";
    private static final String NAME = "3-Day Momentum Burst";
    private static final int DEFAULT_LOOKBACK_DAYS = 10;
    private static final boolean DEFAULT_STRICT_VOLUME = false;
    private static final double DEFAULT_RISK_REWARD_RATIO = 2.0;

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int lookbackDays = params.getInt("lookbackDays", DEFAULT_LOOKBACK_DAYS);
        final boolean strictVolumeIncrease = params.getBoolean("requireStrictVolumeIncrease", DEFAULT_STRICT_VOLUME);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        if (lookbackDays < 3) {
            throw new IllegalArgumentException("lookbackDays must be at least 3");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least lookbackDays + 3 candles
        int minRequired = lookbackDays + 3;
        if (candles.size() < minRequired) {
            return StrategyResult.empty(context.isScreening());
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            int latestIndex = candles.size() - 1;
            if (latestIndex >= 3) {
                TradeSignal signal = checkMomentumBurst(candles, latestIndex, lookbackDays,
                    strictVolumeIncrease, riskRewardRatio);
                if (signal != null) {
                    return StrategyResult.actionable(List.of(signal));
                }
            }
            return StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = lookbackDays + 2; i < candles.size(); i++) {
            TradeSignal signal = checkMomentumBurst(candles, i, lookbackDays,
                strictVolumeIncrease, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return StrategyResult.nonActionable(signals);
    }

    /**
     * Checks for 3-day momentum burst pattern at the given index.
     *
     * @param candles List of candles
     * @param index Current candle index
     * @param lookbackDays Lookback period for breakout confirmation
     * @param strictVolumeIncrease Whether to require strictly increasing volume
     * @param riskRewardRatio Risk-reward ratio for target calculation
     * @return TradeSignal if pattern found, null otherwise
     */
    private TradeSignal checkMomentumBurst(List<Candle> candles, int index,
                                           int lookbackDays, boolean strictVolumeIncrease,
                                           double riskRewardRatio) {
        if (index < 3) {
            return null;
        }

        Candle day1 = candles.get(index - 2);
        Candle day2 = candles.get(index - 1);
        Candle day3 = candles.get(index);

        // Validate candles
        if (!isValidCandle(day1) || !isValidCandle(day2) || !isValidCandle(day3)) {
            return null;
        }

        // Check for 3 consecutive bullish candles
        boolean day1Bullish = day1.getClose().compareTo(day1.getOpen()) > 0;
        boolean day2Bullish = day2.getClose().compareTo(day2.getOpen()) > 0;
        boolean day3Bullish = day3.getClose().compareTo(day3.getOpen()) > 0;

        if (!day1Bullish || !day2Bullish || !day3Bullish) {
            return null;
        }

        // Check volume trend
        long vol1 = day1.getVolume() != null ? day1.getVolume() : 0;
        long vol2 = day2.getVolume() != null ? day2.getVolume() : 0;
        long vol3 = day3.getVolume() != null ? day3.getVolume() : 0;

        boolean volumeIncreasing;
        if (strictVolumeIncrease) {
            volumeIncreasing = vol3 > vol2 && vol2 > vol1;
        } else {
            // At least non-decreasing (allow equal volume)
            volumeIncreasing = vol3 >= vol2 && vol2 >= vol1;
        }

        if (!volumeIncreasing) {
            return null;
        }

        // Check if close exceeds 10-day high
        BigDecimal highestHigh = findHighestHigh(candles, index - lookbackDays, index - 1);
        if (day3.getClose().compareTo(highestHigh) <= 0) {
            return null;
        }

        // Calculate stop loss (below 3-day low)
        BigDecimal threeDayLow = findLowestLow(candles, index - 2, index);
        BigDecimal stopLoss = threeDayLow.multiply(BigDecimal.valueOf(0.98))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal entryPrice = day3.getClose();
        BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

        return TradeSignal.fromCandle(day3, Side.BUY, entryPrice, stopLoss, targetPrice, 1);
    }

    /**
     * Finds the highest high in the given range.
     */
    private BigDecimal findHighestHigh(List<Candle> candles, int startIndex, int endIndex) {
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(candles.size() - 1, endIndex);

        BigDecimal highest = BigDecimal.ZERO;
        for (int i = startIndex; i <= endIndex; i++) {
            Candle c = candles.get(i);
            if (c.getHigh() != null && c.getHigh().compareTo(highest) > 0) {
                highest = c.getHigh();
            }
        }
        return highest;
    }

    /**
     * Finds the lowest low in the given range.
     */
    private BigDecimal findLowestLow(List<Candle> candles, int startIndex, int endIndex) {
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(candles.size() - 1, endIndex);

        BigDecimal lowest = BigDecimal.valueOf(Double.MAX_VALUE);
        for (int i = startIndex; i <= endIndex; i++) {
            Candle c = candles.get(i);
            if (c.getLow() != null && c.getLow().compareTo(lowest) < 0) {
                lowest = c.getLow();
            }
        }
        return lowest;
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
        return DEFAULT_LOOKBACK_DAYS + 3;
    }

    @Override
    public String toString() {
        return "Momentum3DayStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
