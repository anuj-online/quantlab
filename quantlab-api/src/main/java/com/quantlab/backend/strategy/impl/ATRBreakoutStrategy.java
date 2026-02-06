package com.quantlab.backend.strategy.impl;

import com.quantlab.backend.domain.TradeSignal;
import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Side;
import com.quantlab.backend.strategy.ExecutionMode;
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

@Component
@Qualifier("atrBreakoutStrategy")
public class ATRBreakoutStrategy implements Strategy {

    private static final String CODE = "ATR_BREAKOUT";
    private static final String NAME = "ATR Volatility Breakout";

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        ExecutionMode mode = context.getMode();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int atrPeriod = params.getInt("atrPeriod", 14);
        final double atrMultiplier = params.getDouble("atrMultiplier", 1.5);
        final int volumeLookback = params.getInt("volumeLookback", 20);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", 2.0);

        if (atrPeriod < 2) {
            throw new IllegalArgumentException("atrPeriod must be at least 2");
        }
        if (atrMultiplier <= 0) {
            throw new IllegalArgumentException("atrMultiplier must be positive");
        }
        if (volumeLookback < 1) {
            throw new IllegalArgumentException("volumeLookback must be at least 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        int minCandles = atrPeriod + volumeLookback + 1;
        if (candles.size() < minCandles) {
            return StrategyResult.empty(mode == ExecutionMode.SCREEN);
        }

        List<TradeSignal> signals = new ArrayList<>();

        if (mode == ExecutionMode.SCREEN) {
            return evaluateScreeningMode(candles, atrPeriod, atrMultiplier, volumeLookback, riskRewardRatio);
        }

        for (int i = minCandles; i < candles.size(); i++) {
            Candle today = candles.get(i);

            if (!isValidCandle(today)) {
                continue;
            }

            Double atr = calculateATR(candles, i, atrPeriod);
            Double avgVolume = calculateAverageVolume(candles, i - volumeLookback, i);

            if (atr == null || avgVolume == null) {
                continue;
            }

            double todayRange = today.getHigh().subtract(today.getLow()).doubleValue();
            double expectedRange = atrMultiplier * atr;

            if (todayRange > expectedRange) {
                boolean bullish = today.getClose().compareTo(today.getOpen()) > 0;
                boolean highVolume = today.getVolume().compareTo(Long.valueOf(Math.round(avgVolume))) > 0;

                if (bullish && highVolume) {
                    BigDecimal entryPrice = today.getClose();
                    BigDecimal stopLoss = today.getLow();
                    BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

                    TradeSignal signal = new TradeSignal(
                        today.getInstrument(),
                        today.getTradeDate(),
                        bullish ? Side.BUY : Side.SELL,
                        entryPrice,
                        stopLoss,
                        targetPrice,
                        1
                    );
                    signals.add(signal);
                }
            }
        }

        return new StrategyResult(signals, false);
    }

    private StrategyResult evaluateScreeningMode(List<Candle> candles, int atrPeriod,
            double atrMultiplier, int volumeLookback, double riskRewardRatio) {
        int latestIndex = candles.size() - 1;
        Candle today = candles.get(latestIndex);

        if (!isValidCandle(today)) {
            return StrategyResult.empty(false);
        }

        Double atr = calculateATR(candles, latestIndex, atrPeriod);
        Double avgVolume = calculateAverageVolume(candles, candles.size() - volumeLookback, latestIndex);

        if (atr == null || avgVolume == null) {
            return StrategyResult.empty(false);
        }

        double todayRange = today.getHigh().subtract(today.getLow()).doubleValue();
        double expectedRange = atrMultiplier * atr;
        boolean bullish = today.getClose().compareTo(today.getOpen()) > 0;
        boolean highVolume = today.getVolume().compareTo(Long.valueOf(Math.round(avgVolume))) > 0;

        if (todayRange > expectedRange && bullish && highVolume) {
            BigDecimal entryPrice = today.getClose();
            BigDecimal stopLoss = today.getLow();
            BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

            TradeSignal signal = new TradeSignal(
                today.getInstrument(),
                today.getTradeDate(),
                Side.BUY,
                entryPrice,
                stopLoss,
                targetPrice,
                1
            );
            return StrategyResult.actionable(List.of(signal));
        }

        return StrategyResult.empty(false);
    }

    private Double calculateATR(List<Candle> candles, int endIndex, int period) {
        if (candles.size() < period + 1) {
            return null;
        }

        double[] trValues = new double[Math.min(period, endIndex)];

        for (int i = Math.max(1, endIndex - period + 1); i <= endIndex; i++) {
            Candle prev = candles.get(i - 1);
            Candle curr = candles.get(i);

            if (!isValidCandle(prev) || !isValidCandle(curr)) {
                continue;
            }

            double high = curr.getHigh().subtract(prev.getHigh()).max(BigDecimal.ZERO).doubleValue();
            double low = prev.getLow().subtract(curr.getLow()).max(BigDecimal.ZERO).doubleValue();
            double closeDiff = curr.getClose().subtract(prev.getClose()).abs().doubleValue();

            double tr = Math.max(Math.max(high, low), closeDiff);
            trValues[Math.min(i - Math.max(1, endIndex - period), period - 1)] = tr;
        }

        double sum = 0;
        int count = 0;
        for (double tr : trValues) {
            if (tr > 0) {
                sum += tr;
                count++;
            }
        }

        return count > 0 ? sum / count : null;
    }

    private Double calculateAverageVolume(List<Candle> candles, int startIndex, int endIndex) {
        double sum = 0.0;
        int count = 0;

        for (int i = startIndex; i <= endIndex && i < candles.size(); i++) {
            Candle c = candles.get(i);
            if (isValidCandle(c) && c.getVolume() != null) {
                sum += c.getVolume().doubleValue();
                count++;
            }
        }

        return count > 0 ? sum / count : null;
    }

    private BigDecimal calculateTarget(BigDecimal entry, BigDecimal stopLoss, double ratio) {
        BigDecimal risk = entry.subtract(stopLoss);
        return entry.add(risk.multiply(BigDecimal.valueOf(ratio)));
    }

    private boolean isValidCandle(Candle candle) {
        return candle != null &&
            candle.getOpen() != null &&
            candle.getHigh() != null &&
            candle.getLow() != null &&
            candle.getClose() != null &&
            candle.getVolume() != null;
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
        return 35;
    }
}
