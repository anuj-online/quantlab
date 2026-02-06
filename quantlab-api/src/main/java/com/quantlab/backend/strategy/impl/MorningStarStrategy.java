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
@Qualifier("morningStarStrategy")
public class MorningStarStrategy implements Strategy {

    private static final String CODE = "MORNING_STAR";
    private static final String NAME = "Morning Star Reversal";

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        ExecutionMode mode = context.getMode();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final double riskRewardRatio = params.getDouble("riskRewardRatio", 2.0);

        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        if (candles.size() < 3) {
            return StrategyResult.empty(mode == ExecutionMode.SCREEN);
        }

        if (mode == ExecutionMode.SCREEN) {
            return evaluateScreeningMode(candles, riskRewardRatio);
        }

        for (int i = 3; i < candles.size(); i++) {
            TradeSignal signal = checkMorningStarPattern(candles, i, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return new StrategyResult(signals, false);
    }

    private StrategyResult evaluateScreeningMode(List<Candle> candles, double riskRewardRatio) {
        if (candles.size() < 3) {
            return StrategyResult.empty(false);
        }

        int latestIndex = candles.size() - 1;
        TradeSignal signal = checkMorningStarPattern(candles, latestIndex, riskRewardRatio);

        if (signal != null) {
            return StrategyResult.actionable(List.of(signal));
        }

        return StrategyResult.empty(false);
    }

    private TradeSignal checkMorningStarPattern(List<Candle> candles, int endIndex, double riskRewardRatio) {
        Candle day1 = candles.get(endIndex - 2);
        Candle day2 = candles.get(endIndex - 1);
        Candle day3 = candles.get(endIndex);

        if (!isValidCandle(day1) || !isValidCandle(day2) || !isValidCandle(day3)) {
            return null;
        }

        double range1 = day1.getHigh().subtract(day1.getLow()).doubleValue();
        double range2 = day2.getHigh().subtract(day2.getLow()).doubleValue();
        double range3 = day3.getHigh().subtract(day3.getLow()).doubleValue();

        double body1 = Math.abs(day1.getClose().subtract(day1.getOpen()).doubleValue());
        double body2 = Math.abs(day2.getClose().subtract(day2.getOpen()).doubleValue());
        double body3 = Math.abs(day3.getClose().subtract(day3.getOpen()).doubleValue());

        boolean day1Bearish = day1.getClose().compareTo(day1.getOpen()) < 0;
        boolean day2Small = body2 < 0.3 * range2;
        boolean day3Bullish = day3.getClose().compareTo(day3.getOpen()) > 0;

        double midPointDay1 = day1.getLow().add(day1.getHigh()).divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP).doubleValue();
        boolean day3AboveMid = day3.getClose().compareTo(BigDecimal.valueOf(midPointDay1)) > 0;

        if (day1Bearish && day2Small && day3Bullish && day3AboveMid) {
            BigDecimal entryPrice = day3.getClose();
            BigDecimal stopLoss = day1.getLow();
            BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

            return new TradeSignal(
                day3.getInstrument(),
                day3.getTradeDate(),
                Side.BUY,
                entryPrice,
                stopLoss,
                targetPrice,
                1
            );
        }

        return null;
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
        return 3;
    }
}
