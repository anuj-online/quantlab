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
import java.util.OptionalDouble;

/**
 * Bollinger Band Squeeze Strategy.
 * <p>
 * Strategy Code: BB_SQUEEZE
 * <p>
 * This strategy identifies volatility contraction patterns using Bollinger Bands.
 * When Bollinger Bands reach their narrowest point in a specified period (squeeze),
 * it often precedes a significant breakout in either direction.
 * <p>
 * The strategy works by:
 * 1. Calculating Bollinger Band width (upper - lower bands)
 * 2. Detecting when width is at its lowest in N days (squeeze)
 * 3. Entering when price breaks out outside the bands
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Bollinger Band width is the lowest in 'squeezePeriod' days</li>
 *   <li>Price breaks above upper band (bullish breakout)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: Below the lower Bollinger Band</li>
 *   <li>Target: Entry + 2x the band width (measured move)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>period: Period for Bollinger Band calculation (default: 20)</li>
 *   <li>stdDeviations: Standard deviations for bands (default: 2.0)</li>
 *   <li>squeezePeriod: Period to check for minimum width (default: 20)</li>
 *   <li>riskRewardRatio: Target multiplier relative to band width (default: 2.0)</li>
 * </ul>
 */
@Component
@Qualifier("bollingerBandSqueezeStrategy")
public class BollingerBandSqueezeStrategy implements Strategy {

    private static final String CODE = "BB_SQUEEZE";
    private static final String NAME = "Bollinger Band Squeeze";
    private static final int DEFAULT_PERIOD = 20;
    private static final double DEFAULT_STD_DEVIATIONS = 2.0;
    private static final int DEFAULT_SQUEEZE_PERIOD = 20;
    private static final double DEFAULT_RISK_REWARD_RATIO = 2.0;

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int period = params.getInt("period", DEFAULT_PERIOD);
        final double stdDeviations = params.getDouble("stdDeviations", DEFAULT_STD_DEVIATIONS);
        final int squeezePeriod = params.getInt("squeezePeriod", DEFAULT_SQUEEZE_PERIOD);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        if (period < 2) {
            throw new IllegalArgumentException("period must be at least 2");
        }
        if (stdDeviations <= 0) {
            throw new IllegalArgumentException("stdDeviations must be positive");
        }
        if (squeezePeriod < period) {
            throw new IllegalArgumentException("squeezePeriod must be at least period");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least squeezePeriod + 1 candles
        int minRequired = squeezePeriod + 1;
        if (candles.size() < minRequired) {
            return StrategyResult.empty(context.isScreening());
        }

        // Pre-calculate Bollinger Bands for all candles
        BBData[] bbData = calculateBollingerBands(candles, period, stdDeviations);

        if (bbData == null) {
            return StrategyResult.empty(context.isScreening());
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            int latestIndex = candles.size() - 1;
            TradeSignal signal = checkBBSqueeze(candles, latestIndex, bbData,
                squeezePeriod, riskRewardRatio);
            if (signal != null) {
                return StrategyResult.actionable(List.of(signal));
            }
            return StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = squeezePeriod; i < candles.size(); i++) {
            TradeSignal signal = checkBBSqueeze(candles, i, bbData,
                squeezePeriod, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return StrategyResult.nonActionable(signals);
    }

    /**
     * Checks for Bollinger Band squeeze breakout at the given index.
     */
    private TradeSignal checkBBSqueeze(List<Candle> candles, int index,
                                       BBData[] bbData, int squeezePeriod,
                                       double riskRewardRatio) {
        if (index < squeezePeriod || index >= bbData.length) {
            return null;
        }

        Candle today = candles.get(index);
        if (!isValidCandle(today)) {
            return null;
        }

        BBData currentBB = bbData[index];
        if (currentBB == null || !currentBB.isValid()) {
            return null;
        }

        // Check if current BB width is the lowest in squeezePeriod
        double currentWidth = currentBB.getWidth();
        boolean isLowestWidth = true;

        for (int i = index - squeezePeriod; i < index; i++) {
            if (i >= 0 && i < bbData.length && bbData[i] != null && bbData[i].isValid()) {
                if (bbData[i].getWidth() <= currentWidth) {
                    isLowestWidth = false;
                    break;
                }
            }
        }

        if (!isLowestWidth) {
            return null;
        }

        // Check for breakout above upper band
        double close = today.getClose().doubleValue();
        if (close <= currentBB.upperBand) {
            return null;
        }

        // Calculate stop loss (below lower band)
        BigDecimal entryPrice = today.getClose();
        BigDecimal stopLoss = BigDecimal.valueOf(currentBB.lowerBand)
            .multiply(BigDecimal.valueOf(0.98))
            .setScale(2, RoundingMode.HALF_UP);

        // Target based on band width
        BigDecimal bandWidth = BigDecimal.valueOf(currentWidth);
        BigDecimal targetMove = bandWidth.multiply(BigDecimal.valueOf(riskRewardRatio));
        BigDecimal targetPrice = entryPrice.add(targetMove)
            .setScale(2, RoundingMode.HALF_UP);

        return TradeSignal.fromCandle(today, Side.BUY, entryPrice, stopLoss, targetPrice, 1);
    }

    /**
     * Calculates Bollinger Bands for all candles.
     */
    private BBData[] calculateBollingerBands(List<Candle> candles, int period, double stdDeviations) {
        BBData[] bbData = new BBData[candles.size()];

        for (int i = 0; i < candles.size(); i++) {
            if (i < period - 1) {
                bbData[i] = null;
                continue;
            }

            // Calculate SMA and standard deviation
            OptionalDouble smaOpt = calculateSMA(candles, i, period);
            if (!smaOpt.isPresent()) {
                bbData[i] = null;
                continue;
            }

            double sma = smaOpt.getAsDouble();
            double stdDev = calculateStandardDeviation(candles, i, period, sma);

            bbData[i] = new BBData(
                sma,
                sma + (stdDeviations * stdDev),
                sma - (stdDeviations * stdDev)
            );
        }

        return bbData;
    }

    /**
     * Calculates Simple Moving Average.
     */
    private OptionalDouble calculateSMA(List<Candle> candles, int endIndex, int period) {
        int startIndex = endIndex - period + 1;

        return candles.subList(startIndex, endIndex + 1).stream()
            .filter(this::isValidCandle)
            .mapToDouble(c -> c.getClose().doubleValue())
            .average();
    }

    /**
     * Calculates standard deviation for the period.
     */
    private double calculateStandardDeviation(List<Candle> candles, int endIndex, int period, double sma) {
        int startIndex = endIndex - period + 1;

        double sumSquaredDiff = 0;
        int count = 0;

        for (int i = startIndex; i <= endIndex; i++) {
            Candle c = candles.get(i);
            if (isValidCandle(c)) {
                double diff = c.getClose().doubleValue() - sma;
                sumSquaredDiff += diff * diff;
                count++;
            }
        }

        if (count == 0) {
            return 0;
        }

        return Math.sqrt(sumSquaredDiff / count);
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
        return DEFAULT_SQUEEZE_PERIOD + 1;
    }

    @Override
    public String toString() {
        return "BollingerBandSqueezeStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }

    /**
     * Data class to hold Bollinger Band values.
     */
    private static class BBData {
        final double middleBand;
        final double upperBand;
        final double lowerBand;

        BBData(double middleBand, double upperBand, double lowerBand) {
            this.middleBand = middleBand;
            this.upperBand = upperBand;
            this.lowerBand = lowerBand;
        }

        double getWidth() {
            return upperBand - lowerBand;
        }

        boolean isValid() {
            return !Double.isNaN(middleBand) && !Double.isNaN(upperBand) &&
                !Double.isNaN(lowerBand) && getWidth() > 0;
        }
    }
}
