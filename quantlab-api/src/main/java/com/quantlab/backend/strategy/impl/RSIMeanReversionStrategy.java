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
@Qualifier("rsiMeanReversionStrategy")
public class RSIMeanReversionStrategy implements Strategy {

    private static final String CODE = "RSI_MEAN_REVERSION";
    private static final String NAME = "RSI Mean Reversion";

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        ExecutionMode mode = context.getMode();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int rsiPeriod = params.getInt("rsiPeriod", 14);
        final double oversoldThreshold = params.getDouble("oversoldThreshold", 30.0);
        final int volumeLookback = params.getInt("volumeLookback", 20);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", 1.5);

        if (rsiPeriod < 2) {
            throw new IllegalArgumentException("rsiPeriod must be at least 2");
        }
        if (oversoldThreshold <= 0 || oversoldThreshold >= 100) {
            throw new IllegalArgumentException("oversoldThreshold must be between 0 and 100");
        }
        if (volumeLookback < 1) {
            throw new IllegalArgumentException("volumeLookback must be at least 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        int minCandles = rsiPeriod + volumeLookback + 1;
        if (candles.size() < minCandles) {
            return StrategyResult.empty(mode == ExecutionMode.SCREEN);
        }

        List<TradeSignal> signals = new ArrayList<>();

        if (mode == ExecutionMode.SCREEN) {
            return evaluateScreeningMode(candles, rsiPeriod, oversoldThreshold, volumeLookback, riskRewardRatio);
        }

        for (int i = minCandles; i < candles.size(); i++) {
            Candle today = candles.get(i);

            if (!isValidCandle(today)) {
                continue;
            }

            List<Candle> history = candles.subList(0, i + 1);
            Double rsi = calculateRSI(history, rsiPeriod);
            Double avgVolume = calculateAverageVolume(candles, i - volumeLookback, i);

            if (rsi == null || avgVolume == null) {
                continue;
            }

            boolean prevRSIOversold = i > rsiPeriod && 
                getPreviousRSI(candles, i - 1, rsiPeriod) <= oversoldThreshold;
            boolean currentRSIOversold = rsi <= oversoldThreshold;

            if (!prevRSIOversold && currentRSIOversold) {
                if (today.getClose().compareTo(today.getOpen()) > 0 && 
                    today.getVolume().compareTo(Long.valueOf(Math.round(avgVolume))) > 0) {
                    
                    BigDecimal entryPrice = today.getClose();
                    BigDecimal stopLoss = calculateLowestLow(candles, i, volumeLookback);
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
                    signals.add(signal);
                }
            }
        }

        return new StrategyResult(signals, false);
    }

    private StrategyResult evaluateScreeningMode(List<Candle> candles, int rsiPeriod, 
            double oversoldThreshold, int volumeLookback, double riskRewardRatio) {
        int latestIndex = candles.size() - 1;
        Candle today = candles.get(latestIndex);

        if (!isValidCandle(today)) {
            return StrategyResult.empty(false);
        }

        List<Candle> history = candles;
        Double rsi = calculateRSI(history, rsiPeriod);
        Double avgVolume = calculateAverageVolume(candles, candles.size() - volumeLookback, latestIndex);

        if (rsi == null || avgVolume == null) {
            return StrategyResult.empty(false);
        }

        boolean currentRSIOversold = rsi <= oversoldThreshold;
        boolean bullishCandle = today.getClose().compareTo(today.getOpen()) > 0;
        boolean highVolume = today.getVolume().compareTo(Long.valueOf(Math.round(avgVolume))) > 0;

        if (currentRSIOversold && bullishCandle && highVolume) {
            BigDecimal entryPrice = today.getClose();
            BigDecimal stopLoss = calculateLowestLow(candles, latestIndex, volumeLookback);
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

    private Double calculateRSI(List<Candle> candles, int period) {
        if (candles.size() < period + 1) {
            return null;
        }

        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();

        for (int i = 1; i < candles.size(); i++) {
            Candle prev = candles.get(i - 1);
            Candle curr = candles.get(i);

            if (!isValidCandle(prev) || !isValidCandle(curr)) {
                continue;
            }

            double change = curr.getClose().subtract(prev.getClose()).doubleValue();
            if (change > 0) {
                gains.add(change);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(Math.abs(change));
            }

            if (gains.size() >= period) {
                List<Double> recentGains = gains.subList(gains.size() - period, gains.size());
                List<Double> recentLosses = losses.subList(losses.size() - period, losses.size());

                double avgGain = recentGains.stream().mapToDouble(d -> d).average().orElse(0.0);
                double avgLoss = recentLosses.stream().mapToDouble(d -> d).average().orElse(0.0);

                double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
                double rsi = 100 - (100 / (1 + rs));
                return rsi;
            }
        }

        return null;
    }

    private Double getPreviousRSI(List<Candle> candles, int index, int period) {
        if (index < period) {
            return null;
        }
        List<Candle> history = candles.subList(0, index + 1);
        return calculateRSI(history, period);
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

    private BigDecimal calculateLowestLow(List<Candle> candles, int endIndex, int lookback) {
        int startIndex = Math.max(0, endIndex - lookback);
        BigDecimal lowest = candles.get(startIndex).getLow();

        for (int i = startIndex + 1; i <= endIndex && i < candles.size(); i++) {
            Candle c = candles.get(i);
            if (isValidCandle(c) && c.getLow().compareTo(lowest) < 0) {
                lowest = c.getLow();
            }
        }

        return lowest;
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
