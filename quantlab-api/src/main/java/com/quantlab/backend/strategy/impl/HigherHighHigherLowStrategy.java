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
 * Higher High Higher Low Structure Strategy.
 * <p>
 * Strategy Code: HH_HL_STRUCTURE
 * <p>
 * This strategy identifies stocks with established uptrend structure characterized
 * by higher highs and higher lows. This is the defining characteristic of an uptrend
 * and indicates sustained buying interest over time.
 * <p>
 * The strategy detects swing highs and swing lows using a pivot point algorithm,
 * then verifies that the most recent 3 swing highs and 3 swing lows are in an
 * ascending sequence.
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>At least 3 swing highs detected, each higher than the previous</li>
 *   <li>At least 3 swing lows detected, each higher than the previous</li>
 *   <li>Latest close is above the most recent swing low</li>
 *   <li>Latest candle is bullish (close > open)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: Below the most recent swing low</li>
 *   <li>Target: Entry + 2x risk (2:1 reward-risk ratio)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>pivotPeriod: Period for swing detection (default: 5)</li>
 *   <li>minSwingHighs: Minimum number of swing highs required (default: 3)</li>
 *   <li>minSwingLows: Minimum number of swing lows required (default: 3)</li>
 *   <li>riskRewardRatio: Target multiplier (default: 2.0)</li>
 * </ul>
 * <p>
 * Swing Detection:
 * <br>A swing high is when the middle candle's high is the highest of pivotPeriod*2+1 candles.
 * <br>A swing low is when the middle candle's low is the lowest of pivotPeriod*2+1 candles.
 */
@Component
@Qualifier("higherHighHigherLowStrategy")
public class HigherHighHigherLowStrategy implements Strategy {

    private static final String CODE = "HH_HL_STRUCTURE";
    private static final String NAME = "Higher High Higher Low Structure";
    private static final int DEFAULT_PIVOT_PERIOD = 5;
    private static final int DEFAULT_MIN_SWING_HIGHS = 3;
    private static final int DEFAULT_MIN_SWING_LOWS = 3;
    private static final double DEFAULT_RISK_REWARD_RATIO = 2.0;

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int pivotPeriod = params.getInt("pivotPeriod", DEFAULT_PIVOT_PERIOD);
        final int minSwingHighs = params.getInt("minSwingHighs", DEFAULT_MIN_SWING_HIGHS);
        final int minSwingLows = params.getInt("minSwingLows", DEFAULT_MIN_SWING_LOWS);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        if (pivotPeriod < 2) {
            throw new IllegalArgumentException("pivotPeriod must be at least 2");
        }
        if (minSwingHighs < 2) {
            throw new IllegalArgumentException("minSwingHighs must be at least 2");
        }
        if (minSwingLows < 2) {
            throw new IllegalArgumentException("minSwingLows must be at least 2");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need sufficient candles for swing detection
        int minRequired = (pivotPeriod * 2 + 1) * Math.max(minSwingHighs, minSwingLows) + pivotPeriod;
        if (candles.size() < minRequired) {
            return StrategyResult.empty(context.isScreening());
        }

        // Detect all swing points
        List<SwingPoint> swingHighs = detectSwingHighs(candles, pivotPeriod);
        List<SwingPoint> swingLows = detectSwingLows(candles, pivotPeriod);

        if (swingHighs.size() < minSwingHighs || swingLows.size() < minSwingLows) {
            return StrategyResult.empty(context.isScreening());
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            int latestIndex = candles.size() - 1;
            TradeSignal signal = checkHHLStructure(candles, latestIndex, swingHighs, swingLows,
                minSwingHighs, minSwingLows, riskRewardRatio);
            if (signal != null) {
                return StrategyResult.actionable(List.of(signal));
            }
            return StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = minRequired - 1; i < candles.size(); i++) {
            TradeSignal signal = checkHHLStructure(candles, i, swingHighs, swingLows,
                minSwingHighs, minSwingLows, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return StrategyResult.nonActionable(signals);
    }

    /**
     * Checks for HH HL structure at the given index.
     */
    private TradeSignal checkHHLStructure(List<Candle> candles, int index,
                                          List<SwingPoint> swingHighs, List<SwingPoint> swingLows,
                                          int minSwingHighs, int minSwingLows,
                                          double riskRewardRatio) {
        Candle today = candles.get(index);
        if (!isValidCandle(today)) {
            return null;
        }

        // Check for bullish candle
        if (today.getClose().compareTo(today.getOpen()) <= 0) {
            return null;
        }

        // Get recent swing highs and lows that occurred before today
        List<SwingPoint> recentHighs = getRecentSwingsBeforeIndex(swingHighs, index, minSwingHighs);
        List<SwingPoint> recentLows = getRecentSwingsBeforeIndex(swingLows, index, minSwingLows);

        if (recentHighs.size() < minSwingHighs || recentLows.size() < minSwingLows) {
            return null;
        }

        // Check if swing highs are making higher highs
        if (!areHigherHighs(recentHighs)) {
            return null;
        }

        // Check if swing lows are making higher lows
        if (!areHigherLows(recentLows)) {
            return null;
        }

        // Check if close is above the most recent swing low
        SwingPoint lastSwingLow = recentLows.get(recentLows.size() - 1);
        if (today.getClose().compareTo(BigDecimal.valueOf(lastSwingLow.value)) <= 0) {
            return null;
        }

        // All conditions met - create signal
        BigDecimal entryPrice = today.getClose();
        BigDecimal stopLoss = BigDecimal.valueOf(lastSwingLow.value)
            .multiply(BigDecimal.valueOf(0.98))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

        return TradeSignal.fromCandle(today, Side.BUY, entryPrice, stopLoss, targetPrice, 1);
    }

    /**
     * Detects swing highs using pivot point algorithm.
     */
    private List<SwingPoint> detectSwingHighs(List<Candle> candles, int pivotPeriod) {
        List<SwingPoint> swingHighs = new ArrayList<>();

        for (int i = pivotPeriod; i < candles.size() - pivotPeriod; i++) {
            Candle current = candles.get(i);
            if (!isValidCandle(current)) {
                continue;
            }

            double currentHigh = current.getHigh().doubleValue();
            boolean isSwingHigh = true;

            // Check if current high is the highest in the neighborhood
            for (int j = i - pivotPeriod; j <= i + pivotPeriod; j++) {
                if (j == i) continue;
                if (j < 0 || j >= candles.size()) continue;

                Candle c = candles.get(j);
                if (isValidCandle(c) && c.getHigh().doubleValue() >= currentHigh) {
                    isSwingHigh = false;
                    break;
                }
            }

            if (isSwingHigh) {
                swingHighs.add(new SwingPoint(i, currentHigh));
            }
        }

        return swingHighs;
    }

    /**
     * Detects swing lows using pivot point algorithm.
     */
    private List<SwingPoint> detectSwingLows(List<Candle> candles, int pivotPeriod) {
        List<SwingPoint> swingLows = new ArrayList<>();

        for (int i = pivotPeriod; i < candles.size() - pivotPeriod; i++) {
            Candle current = candles.get(i);
            if (!isValidCandle(current)) {
                continue;
            }

            double currentLow = current.getLow().doubleValue();
            boolean isSwingLow = true;

            // Check if current low is the lowest in the neighborhood
            for (int j = i - pivotPeriod; j <= i + pivotPeriod; j++) {
                if (j == i) continue;
                if (j < 0 || j >= candles.size()) continue;

                Candle c = candles.get(j);
                if (isValidCandle(c) && c.getLow().doubleValue() <= currentLow) {
                    isSwingLow = false;
                    break;
                }
            }

            if (isSwingLow) {
                swingLows.add(new SwingPoint(i, currentLow));
            }
        }

        return swingLows;
    }

    /**
     * Gets the most recent N swing points that occurred before the given index.
     */
    private List<SwingPoint> getRecentSwingsBeforeIndex(List<SwingPoint> swings, int index, int count) {
        List<SwingPoint> recent = new ArrayList<>();
        for (int i = swings.size() - 1; i >= 0 && recent.size() < count; i--) {
            if (swings.get(i).index < index) {
                recent.add(0, swings.get(i));
            }
        }
        return recent;
    }

    /**
     * Checks if swing highs are making higher highs.
     */
    private boolean areHigherHighs(List<SwingPoint> swingHighs) {
        for (int i = 1; i < swingHighs.size(); i++) {
            if (swingHighs.get(i).value <= swingHighs.get(i - 1).value) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if swing lows are making higher lows.
     */
    private boolean areHigherLows(List<SwingPoint> swingLows) {
        for (int i = 1; i < swingLows.size(); i++) {
            if (swingLows.get(i).value <= swingLows.get(i - 1).value) {
                return false;
            }
        }
        return true;
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
        return (DEFAULT_PIVOT_PERIOD * 2 + 1) * 3 + DEFAULT_PIVOT_PERIOD;
    }

    @Override
    public String toString() {
        return "HigherHighHigherLowStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }

    /**
     * Data class to represent a swing point.
     */
    private static class SwingPoint {
        final int index;
        final double value;

        SwingPoint(int index, double value) {
            this.index = index;
            this.value = value;
        }
    }
}
