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
@Qualifier("dojiReversalStrategy")
public class DojiReversalStrategy implements Strategy {

    private static final String CODE = "DOJI_REVERSAL";
    private static final String NAME = "Doji Candlestick Reversal";

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        ExecutionMode mode = context.getMode();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final double dojiThreshold = params.getDouble("dojiThreshold", 0.1);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", 1.5);

        if (dojiThreshold <= 0 || dojiThreshold >= 1) {
            throw new IllegalArgumentException("dojiThreshold must be between 0 and 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        if (candles.size() < 2) {
            return StrategyResult.empty(mode == ExecutionMode.SCREEN);
        }

        if (mode == ExecutionMode.SCREEN) {
            return evaluateScreeningMode(candles, dojiThreshold, riskRewardRatio);
        }

        for (int i = 1; i < candles.size(); i++) {
            Candle dojiCandle = candles.get(i - 1);
            Candle nextCandle = candles.get(i);

            if (!isValidCandle(dojiCandle) || !isValidCandle(nextCandle)) {
                continue;
            }

            DojiType dojiType = identifyDojiType(dojiCandle, dojiThreshold);

            if (dojiType != DojiType.NONE) {
                TradeSignal signal = createReversalSignal(dojiType, dojiCandle, nextCandle, riskRewardRatio);
                if (signal != null) {
                    signals.add(signal);
                }
            }
        }

        return new StrategyResult(signals, false);
    }

    private StrategyResult evaluateScreeningMode(List<Candle> candles, double dojiThreshold, double riskRewardRatio) {
        if (candles.size() < 2) {
            return StrategyResult.empty(false);
        }

        int latestIndex = candles.size() - 1;
        Candle dojiCandle = candles.get(latestIndex - 1);
        Candle nextCandle = candles.get(latestIndex);

        if (!isValidCandle(dojiCandle) || !isValidCandle(nextCandle)) {
            return StrategyResult.empty(false);
        }

        DojiType dojiType = identifyDojiType(dojiCandle, dojiThreshold);

        if (dojiType != DojiType.NONE) {
            TradeSignal signal = createReversalSignal(dojiType, dojiCandle, nextCandle, riskRewardRatio);
            if (signal != null) {
                return StrategyResult.actionable(List.of(signal));
            }
        }

        return StrategyResult.empty(false);
    }

    private DojiType identifyDojiType(Candle candle, double threshold) {
        double open = candle.getOpen().doubleValue();
        double close = candle.getClose().doubleValue();
        double high = candle.getHigh().doubleValue();
        double low = candle.getLow().doubleValue();

        double range = high - low;
        double body = Math.abs(close - open);
        double upperShadow = high - Math.max(open, close);
        double lowerShadow = Math.min(open, close) - low;

        if (body / range <= threshold) {
            if (lowerShadow > 2 * upperShadow) {
                return DojiType.DRAGONFLY;
            } else if (upperShadow > 2 * lowerShadow) {
                return DojiType.GRAVESTONE;
            }
            return DojiType.STANDARD;
        }

        return DojiType.NONE;
    }

    private TradeSignal createReversalSignal(DojiType dojiType, Candle dojiCandle, 
            Candle nextCandle, double riskRewardRatio) {
        Side signalSide = null;
        BigDecimal entryPrice = nextCandle.getClose();
        BigDecimal stopLoss = null;

        if (dojiType == DojiType.GRAVESTONE) {
            if (nextCandle.getClose().compareTo(dojiCandle.getOpen()) < 0) {
                signalSide = Side.SELL;
                stopLoss = dojiCandle.getHigh();
            }
        } else if (dojiType == DojiType.DRAGONFLY) {
            if (nextCandle.getClose().compareTo(dojiCandle.getOpen()) > 0) {
                signalSide = Side.BUY;
                stopLoss = dojiCandle.getLow();
            }
        } else if (dojiType == DojiType.STANDARD) {
            if (nextCandle.getClose().compareTo(nextCandle.getOpen()) > 0 && 
                nextCandle.getHigh().compareTo(dojiCandle.getHigh()) > 0) {
                signalSide = Side.BUY;
                stopLoss = dojiCandle.getLow();
            } else if (nextCandle.getClose().compareTo(nextCandle.getOpen()) < 0 && 
                       nextCandle.getLow().compareTo(dojiCandle.getLow()) < 0) {
                signalSide = Side.SELL;
                stopLoss = dojiCandle.getHigh();
            }
        }

        if (signalSide != null && stopLoss != null) {
            BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, signalSide, riskRewardRatio);
            return new TradeSignal(
                nextCandle.getInstrument(),
                nextCandle.getTradeDate(),
                signalSide,
                entryPrice,
                stopLoss,
                targetPrice,
                1
            );
        }

        return null;
    }

    private BigDecimal calculateTarget(BigDecimal entry, BigDecimal stopLoss, Side side, double ratio) {
        BigDecimal risk = side == Side.BUY ? entry.subtract(stopLoss) : stopLoss.subtract(entry);
        return side == Side.BUY ? entry.add(risk.multiply(BigDecimal.valueOf(ratio)))
                                      : entry.subtract(risk.multiply(BigDecimal.valueOf(ratio)));
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
        return 2;
    }

    private enum DojiType {
        NONE,
        STANDARD,
        GRAVESTONE,
        DRAGONFLY
    }
}
