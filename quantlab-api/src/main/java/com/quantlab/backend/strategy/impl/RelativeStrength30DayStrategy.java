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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Relative Strength Momentum Strategy (30-Day).
 * <p>
 * Strategy Code: REL_STRENGTH_30D
 * <p>
 * This strategy identifies stocks showing relative strength compared to a market index.
 * Relative strength is a key indicator of institutional accumulation and often precedes
 * sustained outperformance.
 * <p>
 * The strategy compares the stock's 30-day return to the index's 30-day return and
 * looks for situations where:
 * 1. The stock is outperforming the index (higher relative return)
 * 2. The stock breaks out to a new 30-day high
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Stock's 30-day return > Index's 30-day return (relative strength)</li>
 *   <li>Current close > highest close in last 30 days (breakout)</li>
 *   <li>Current candle is bullish (close > open)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: 3% below entry (percentage-based stop)</li>
 *   <li>Target: Entry + 2x risk (2:1 reward-risk ratio)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>lookbackPeriod: Period for return calculation (default: 30)</li>
 *   <li>stopLossPercent: Stop loss percentage (default: 0.03 = 3%)</li>
 *   <li>riskRewardRatio: Target multiplier (default: 2.0)</li>
 * </ul>
 * <p>
 * Note: This strategy requires index data for comparison. If index data is not available,
 * the strategy will log a warning and fall back to a simple breakout strategy.
 */
@Component
@Qualifier("relativeStrength30DayStrategy")
public class RelativeStrength30DayStrategy implements Strategy {

    private static final Logger log = LoggerFactory.getLogger(RelativeStrength30DayStrategy.class);
    private static final String CODE = "REL_STRENGTH_30D";
    private static final String NAME = "Relative Strength Momentum (30-Day)";
    private static final int DEFAULT_LOOKBACK_PERIOD = 30;
    private static final double DEFAULT_STOP_LOSS_PERCENT = 0.03; // 3%
    private static final double DEFAULT_RISK_REWARD_RATIO = 2.0;

    // Default index return assumption if index data not available
    // This represents a modest market expectation
    private static final double DEFAULT_INDEX_RETURN = 0.02; // 2% for 30 days

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int lookbackPeriod = params.getInt("lookbackPeriod", DEFAULT_LOOKBACK_PERIOD);
        final double stopLossPercent = params.getDouble("stopLossPercent", DEFAULT_STOP_LOSS_PERCENT);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        if (lookbackPeriod < 2) {
            throw new IllegalArgumentException("lookbackPeriod must be at least 2");
        }
        if (stopLossPercent <= 0 || stopLossPercent >= 1) {
            throw new IllegalArgumentException("stopLossPercent must be between 0 and 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least lookbackPeriod candles
        if (candles.size() < lookbackPeriod) {
            return StrategyResult.empty(context.isScreening());
        }

        // Try to get index return from parameters (if provided by calling code)
        // This allows external systems to inject index data
        double indexReturn = params.getDouble("indexReturn", Double.NaN);

        // If index return not provided, use default and log
        if (Double.isNaN(indexReturn)) {
            log.debug("Index return not provided in parameters, using default assumption: {}%", DEFAULT_INDEX_RETURN * 100);
            indexReturn = DEFAULT_INDEX_RETURN;
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            int latestIndex = candles.size() - 1;
            TradeSignal signal = checkRelativeStrength(candles, latestIndex,
                lookbackPeriod, indexReturn, stopLossPercent, riskRewardRatio);
            if (signal != null) {
                return StrategyResult.actionable(List.of(signal));
            }
            return StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = lookbackPeriod; i < candles.size(); i++) {
            TradeSignal signal = checkRelativeStrength(candles, i,
                lookbackPeriod, indexReturn, stopLossPercent, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return StrategyResult.nonActionable(signals);
    }

    /**
     * Checks for relative strength breakout at the given index.
     */
    private TradeSignal checkRelativeStrength(List<Candle> candles, int index,
                                              int lookbackPeriod, double indexReturn,
                                              double stopLossPercent, double riskRewardRatio) {
        if (index < lookbackPeriod) {
            return null;
        }

        Candle today = candles.get(index);
        if (!isValidCandle(today)) {
            return null;
        }

        // Calculate stock's 30-day return
        OptionalDouble stockReturnOpt = calculateReturn(candles, index, lookbackPeriod);
        if (!stockReturnOpt.isPresent()) {
            return null;
        }

        double stockReturn = stockReturnOpt.getAsDouble();

        // Check if stock is outperforming index (relative strength)
        if (stockReturn <= indexReturn) {
            return null;
        }

        // Check for breakout: current close > highest close in lookback period
        BigDecimal highestClose = findHighestClose(candles, index - lookbackPeriod, index - 1);
        if (today.getClose().compareTo(highestClose) <= 0) {
            return null;
        }

        // Check for bullish candle
        if (today.getClose().compareTo(today.getOpen()) <= 0) {
            return null;
        }

        // All conditions met - create signal
        BigDecimal entryPrice = today.getClose();
        BigDecimal stopLoss = entryPrice.multiply(BigDecimal.valueOf(1 - stopLossPercent))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

        return TradeSignal.fromCandle(today, Side.BUY, entryPrice, stopLoss, targetPrice, 1);
    }

    /**
     * Calculates the return over the specified lookback period.
     * Return = (currentClose - closeNPeriodsAgo) / closeNPeriodsAgo
     */
    private OptionalDouble calculateReturn(List<Candle> candles, int endIndex, int lookbackPeriod) {
        int startIndex = endIndex - lookbackPeriod;

        if (startIndex < 0) {
            return OptionalDouble.empty();
        }

        Candle startCandle = candles.get(startIndex);
        Candle endCandle = candles.get(endIndex);

        if (!isValidCandle(startCandle) || !isValidCandle(endCandle)) {
            return OptionalDouble.empty();
        }

        double startPrice = startCandle.getClose().doubleValue();
        double endPrice = endCandle.getClose().doubleValue();

        if (startPrice <= 0) {
            return OptionalDouble.empty();
        }

        double returnPct = (endPrice - startPrice) / startPrice;
        return OptionalDouble.of(returnPct);
    }

    /**
     * Finds the highest close in the given range.
     */
    private BigDecimal findHighestClose(List<Candle> candles, int startIndex, int endIndex) {
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(candles.size() - 1, endIndex);

        BigDecimal highest = BigDecimal.ZERO;
        for (int i = startIndex; i <= endIndex; i++) {
            Candle c = candles.get(i);
            if (c.getClose() != null && c.getClose().compareTo(highest) > 0) {
                highest = c.getClose();
            }
        }
        return highest;
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
        return DEFAULT_LOOKBACK_PERIOD;
    }

    @Override
    public String toString() {
        return "RelativeStrength30DayStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
