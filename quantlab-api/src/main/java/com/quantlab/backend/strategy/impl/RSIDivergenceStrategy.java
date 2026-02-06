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
@Qualifier("rsiDivergenceStrategy")
public class RSIDivergenceStrategy implements Strategy {

    private static final String CODE = "RSI_DIVERGENCE";
    private static final String NAME = "RSI Divergence Detector";

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        ExecutionMode mode = context.getMode();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final int rsiPeriod = params.getInt("rsiPeriod", 14);
        final int lookbackForHighLow = params.getInt("lookbackForHighLow", 5);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", 2.0);

        if (rsiPeriod < 2) {
            throw new IllegalArgumentException("rsiPeriod must be at least 2");
        }
        if (lookbackForHighLow < 2) {
            throw new IllegalArgumentException("lookbackForHighLow must be at least 2");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        int minCandles = rsiPeriod + lookbackForHighLow + 1;
        if (candles.size() < minCandles) {
            return StrategyResult.empty(mode == ExecutionMode.SCREEN);
        }

        List<TradeSignal> signals = new ArrayList<>();

        if (mode == ExecutionMode.SCREEN) {
            return evaluateScreeningMode(candles, rsiPeriod, lookbackForHighLow, riskRewardRatio);
        }

        for (int i = minCandles; i < candles.size(); i++) {
            Candle today = candles.get(i);

            if (!isValidCandle(today)) {
                continue;
            }

            List<Candle> history = candles.subList(0, i + 1);

            DivergenceResult bullishDivergence = checkBullishDivergence(history, i, lookbackForHighLow, rsiPeriod);
            DivergenceResult bearishDivergence = checkBearishDivergence(history, i, lookbackForHighLow, rsiPeriod);

            if (bullishDivergence != null && bullishDivergence.confidence > 0.6) {
                TradeSignal signal = createDivergenceSignal(today, Side.BUY, bullishDivergence.stopLoss, riskRewardRatio);
                signals.add(signal);
            } else if (bearishDivergence != null && bearishDivergence.confidence > 0.6) {
                TradeSignal signal = createDivergenceSignal(today, Side.SELL, bearishDivergence.stopLoss, riskRewardRatio);
                signals.add(signal);
            }
        }

        return new StrategyResult(signals, false);
    }

    private StrategyResult evaluateScreeningMode(List<Candle> candles, int rsiPeriod,
            int lookbackForHighLow, double riskRewardRatio) {
        int latestIndex = candles.size() - 1;
        Candle today = candles.get(latestIndex);

        if (!isValidCandle(today)) {
            return StrategyResult.empty(false);
        }

        List<Candle> history = candles;

        DivergenceResult bullishDivergence = checkBullishDivergence(history, latestIndex, lookbackForHighLow, rsiPeriod);
        DivergenceResult bearishDivergence = checkBearishDivergence(history, latestIndex, lookbackForHighLow, rsiPeriod);

        if (bullishDivergence != null && bullishDivergence.confidence > 0.6) {
            TradeSignal signal = createDivergenceSignal(today, Side.BUY, bullishDivergence.stopLoss, riskRewardRatio);
            return StrategyResult.actionable(List.of(signal));
        } else if (bearishDivergence != null && bearishDivergence.confidence > 0.6) {
            TradeSignal signal = createDivergenceSignal(today, Side.SELL, bearishDivergence.stopLoss, riskRewardRatio);
            return StrategyResult.actionable(List.of(signal));
        }

        return StrategyResult.empty(false);
    }

    private DivergenceResult checkBullishDivergence(List<Candle> candles, int endIndex, int lookback, int rsiPeriod) {
        if (candles.size() < lookback + 2) {
            return null;
        }

        double[] priceLows = new double[lookback];
        double[] rsiLows = new double[lookback];

        for (int i = 0; i < lookback; i++) {
            int idx = endIndex - lookback + i;
            if (idx < 0) continue;

            Candle c = candles.get(idx);
            if (!isValidCandle(c)) continue;

            Double rsi = calculateRSI(candles, Math.max(idx, rsiPeriod), rsiPeriod);
            if (rsi == null) continue;

            priceLows[i] = c.getLow().doubleValue();
            rsiLows[i] = rsi;
        }

        boolean priceLowerLows = checkTrend(priceLows, false);
        boolean rsiHigherLows = checkTrend(rsiLows, true);

        if (priceLowerLows && rsiHigherLows) {
            double confidence = calculateConfidence(priceLows, rsiLows, false, true);
            Candle today = candles.get(endIndex);
            return new DivergenceResult(today.getLow(), confidence);
        }

        return null;
    }

    private DivergenceResult checkBearishDivergence(List<Candle> candles, int endIndex, int lookback, int rsiPeriod) {
        if (candles.size() < lookback + 2) {
            return null;
        }

        double[] priceHighs = new double[lookback];
        double[] rsiHighs = new double[lookback];

        for (int i = 0; i < lookback; i++) {
            int idx = endIndex - lookback + i;
            if (idx < 0) continue;

            Candle c = candles.get(idx);
            if (!isValidCandle(c)) continue;

            Double rsi = calculateRSI(candles, Math.max(idx, rsiPeriod), rsiPeriod);
            if (rsi == null) continue;

            priceHighs[i] = c.getHigh().doubleValue();
            rsiHighs[i] = rsi;
        }

        boolean priceHigherHighs = checkTrend(priceHighs, true);
        boolean rsiLowerHighs = checkTrend(rsiHighs, false);

        if (priceHigherHighs && rsiLowerHighs) {
            double confidence = calculateConfidence(priceHighs, rsiHighs, true, false);
            Candle today = candles.get(endIndex);
            return new DivergenceResult(today.getHigh(), confidence);
        }

        return null;
    }

    private boolean checkTrend(double[] values, boolean ascending) {
        if (values.length < 2) return false;

        int trendCount = 0;
        for (int i = 1; i < values.length; i++) {
            if (ascending && values[i] > values[i - 1]) trendCount++;
            else if (!ascending && values[i] < values[i - 1]) trendCount++;
        }

        return trendCount >= values.length / 2;
    }

    private double calculateConfidence(double[] priceValues, double[] rsiValues,
            boolean priceDirection, boolean rsiDirection) {
        double priceChange = Math.abs(priceValues[priceValues.length - 1] - priceValues[0]);
        double rsiChange = Math.abs(rsiValues[rsiValues.length - 1] - rsiValues[0]);

        if (priceChange == 0 || rsiChange == 0) return 0.5;

        return Math.min(1.0, rsiChange / priceChange);
    }

    private TradeSignal createDivergenceSignal(Candle candle, Side side,
            BigDecimal stopLoss, double riskRewardRatio) {
        BigDecimal entryPrice = candle.getClose();
        BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, side, riskRewardRatio);

        return new TradeSignal(
            candle.getInstrument(),
            candle.getTradeDate(),
            side,
            entryPrice,
            stopLoss,
            targetPrice,
            1
        );
    }

    private BigDecimal calculateTarget(BigDecimal entry, BigDecimal stopLoss, Side side, double ratio) {
        BigDecimal risk = side == Side.BUY ? entry.subtract(stopLoss) : stopLoss.subtract(entry);
        return side == Side.BUY ? entry.add(risk.multiply(BigDecimal.valueOf(ratio)))
                                      : entry.subtract(risk.multiply(BigDecimal.valueOf(ratio)));
    }

    private Double calculateRSI(List<Candle> candles, int endIndex, int period) {
        if (candles.size() < period + 1) return null;

        List<Double> gains = new ArrayList<>();
        List<Double> losses = new ArrayList<>();

        for (int i = 1; i <= endIndex && i < candles.size(); i++) {
            Candle prev = candles.get(i - 1);
            Candle curr = candles.get(i);

            if (!isValidCandle(prev) || !isValidCandle(curr)) continue;

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
                return 100 - (100 / (1 + rs));
            }
        }

        return null;
    }

    private record DivergenceResult(BigDecimal stopLoss, double confidence) {}

    private boolean isValidCandle(Candle candle) {
        return candle != null &&
            candle.getOpen() != null &&
            candle.getHigh() != null &&
            candle.getLow() != null &&
            candle.getClose() != null;
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
        return 30;
    }
}
