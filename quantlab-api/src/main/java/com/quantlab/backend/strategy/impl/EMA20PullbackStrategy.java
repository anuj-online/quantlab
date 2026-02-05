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
 * Trend Pullback to EMA20 Strategy.
 * <p>
 * Strategy Code: EMA20_PULLBACK
 * <p>
 * This strategy identifies buying opportunities in established uptrends when price
 * pulls back to the 20-period Exponential Moving Average (EMA20). This is a classic
 * trend-following setup that buys on dips within a broader uptrend.
 * <p>
 * The strategy uses two EMAs:
 * - EMA50: Confirms the broader trend (price should be above EMA50)
 * - EMA20: Acts as dynamic support for pullback entries
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Price is above EMA50 (confirms uptrend)</li>
 *   <li>Price pulls back to touch or slightly penetrate EMA20</li>
 *   <li>Candle closes bullish (close > open) showing rejection of lower prices</li>
 *   <li>Close is near the day's high (shows buying interest)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: Below the recent swing low (lowest low in lookback period)</li>
 *   <li>Target: Entry + 2x risk (2:1 reward-risk ratio)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>fastEMA: Fast EMA period for pullback detection (default: 20)</li>
 *   <li>slowEMA: Slow EMA period for trend confirmation (default: 50)</li>
 *   <li>pullbackThreshold: Max distance from EMA20 for pullback (default: 0.02 = 2%)</li>
 *   <li>closePositionThreshold: Min close position in range (default: 0.5 = 50%)</li>
 *   <li>lookbackDays: Period for stop loss calculation (default: 5)</li>
 *   <li>riskRewardRatio: Target multiplier (default: 2.0)</li>
 * </ul>
 */
@Component
@Qualifier("ema20PullbackStrategy")
public class EMA20PullbackStrategy implements Strategy {

    private static final String CODE = "EMA20_PULLBACK";
    private static final String NAME = "Trend Pullback to EMA20";
    private static final int DEFAULT_FAST_EMA = 20;
    private static final int DEFAULT_SLOW_EMA = 50;
    private static final double DEFAULT_PULLBACK_THRESHOLD = 0.02; // 2%
    private static final double DEFAULT_CLOSE_POSITION_THRESHOLD = 0.5; // 50%
    private static final int DEFAULT_LOOKBACK_DAYS = 5;
    private static final double DEFAULT_RISK_REWARD_RATIO = 2.0;

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int fastEMA = params.getInt("fastEMA", DEFAULT_FAST_EMA);
        final int slowEMA = params.getInt("slowEMA", DEFAULT_SLOW_EMA);
        final double pullbackThreshold = params.getDouble("pullbackThreshold", DEFAULT_PULLBACK_THRESHOLD);
        final double closePositionThreshold = params.getDouble("closePositionThreshold", DEFAULT_CLOSE_POSITION_THRESHOLD);
        final int lookbackDays = params.getInt("lookbackDays", DEFAULT_LOOKBACK_DAYS);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        if (fastEMA < 1 || slowEMA < 1) {
            throw new IllegalArgumentException("EMA periods must be at least 1");
        }
        if (fastEMA >= slowEMA) {
            throw new IllegalArgumentException("fastEMA must be less than slowEMA");
        }
        if (pullbackThreshold <= 0 || pullbackThreshold >= 1) {
            throw new IllegalArgumentException("pullbackThreshold must be between 0 and 1");
        }
        if (closePositionThreshold <= 0 || closePositionThreshold > 1) {
            throw new IllegalArgumentException("closePositionThreshold must be between 0 and 1");
        }
        if (lookbackDays < 1) {
            throw new IllegalArgumentException("lookbackDays must be at least 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least slowEMA candles
        if (candles.size() < slowEMA) {
            return StrategyResult.empty(context.isScreening());
        }

        // Pre-calculate EMAs for all candles
        double[] ema20Values = calculateEMAArray(candles, fastEMA);
        double[] ema50Values = calculateEMAArray(candles, slowEMA);

        if (ema20Values == null || ema50Values == null) {
            return StrategyResult.empty(context.isScreening());
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            int latestIndex = candles.size() - 1;
            if (latestIndex >= slowEMA) {
                TradeSignal signal = checkPullback(candles, latestIndex, ema20Values, ema50Values,
                    pullbackThreshold, closePositionThreshold, lookbackDays, riskRewardRatio);
                if (signal != null) {
                    return StrategyResult.actionable(List.of(signal));
                }
            }
            return StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = slowEMA; i < candles.size(); i++) {
            TradeSignal signal = checkPullback(candles, i, ema20Values, ema50Values,
                pullbackThreshold, closePositionThreshold, lookbackDays, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return StrategyResult.nonActionable(signals);
    }

    /**
     * Checks for EMA20 pullback pattern at the given index.
     */
    private TradeSignal checkPullback(List<Candle> candles, int index,
                                      double[] ema20Values, double[] ema50Values,
                                      double pullbackThreshold, double closePositionThreshold,
                                      int lookbackDays, double riskRewardRatio) {
        Candle today = candles.get(index);

        if (!isValidCandle(today)) {
            return null;
        }

        double ema20 = ema20Values[index];
        double ema50 = ema50Values[index];
        double close = today.getClose().doubleValue();

        // Check if price is above EMA50 (uptrend confirmation)
        if (close <= ema50) {
            return null;
        }

        // Check if price has pulled back to EMA20 (within threshold)
        double pullbackDistance = Math.abs(close - ema20) / ema20;
        if (pullbackDistance > pullbackThreshold) {
            return null;
        }

        // Check for bullish close (close > open)
        if (today.getClose().compareTo(today.getOpen()) <= 0) {
            return null;
        }

        // Check if close is in upper portion of range (shows buying interest)
        double range = today.getHigh().subtract(today.getLow()).doubleValue();
        if (range <= 0) {
            return null;
        }

        double closePosition = (today.getClose().subtract(today.getLow()).doubleValue()) / range;
        if (closePosition < closePositionThreshold) {
            return null;
        }

        // Calculate stop loss (below recent low)
        BigDecimal stopLoss = calculateStopLoss(candles, index, lookbackDays);
        BigDecimal entryPrice = today.getClose();
        BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

        return TradeSignal.fromCandle(today, Side.BUY, entryPrice, stopLoss, targetPrice, 1);
    }

    /**
     * Calculates EMA values for all candles.
     * Returns array where index corresponds to candle index.
     * Returns null for indices where EMA cannot be calculated.
     */
    private double[] calculateEMAArray(List<Candle> candles, int period) {
        double[] emaValues = new double[candles.size()];

        // Initialize with null (0.0 indicates not calculated)
        for (int i = 0; i < candles.size(); i++) {
            emaValues[i] = Double.NaN;
        }

        // Calculate SMA for first EMA value
        double sum = 0;
        int count = 0;
        for (int i = 0; i < Math.min(period, candles.size()); i++) {
            if (isValidCandle(candles.get(i))) {
                sum += candles.get(i).getClose().doubleValue();
                count++;
            }
        }

        if (count < period) {
            return emaValues; // Not enough valid candles
        }

        double ema = sum / period;
        emaValues[period - 1] = ema;

        // Calculate EMA for remaining candles
        double multiplier = 2.0 / (period + 1);
        for (int i = period; i < candles.size(); i++) {
            if (isValidCandle(candles.get(i))) {
                double close = candles.get(i).getClose().doubleValue();
                ema = (close - ema) * multiplier + ema;
                emaValues[i] = ema;
            }
        }

        return emaValues;
    }

    /**
     * Calculates stop loss based on recent low in the lookback period.
     */
    private BigDecimal calculateStopLoss(List<Candle> candles, int endIndex, int lookbackDays) {
        int startIndex = Math.max(0, endIndex - lookbackDays);

        BigDecimal lowestLow = BigDecimal.valueOf(Double.MAX_VALUE);
        for (int i = startIndex; i <= endIndex; i++) {
            Candle c = candles.get(i);
            if (c.getLow() != null && c.getLow().compareTo(lowestLow) < 0) {
                lowestLow = c.getLow();
            }
        }

        // Place stop loss slightly below the recent low (1% below)
        return lowestLow.multiply(BigDecimal.valueOf(0.99))
            .setScale(2, RoundingMode.HALF_UP);
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
        return DEFAULT_SLOW_EMA;
    }

    @Override
    public String toString() {
        return "EMA20PullbackStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
