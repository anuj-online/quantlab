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
 * Gap-Up Hold Continuation Strategy.
 * <p>
 * Strategy Code: GAP_HOLD
 * <p>
 * This strategy identifies strong gap-up openings that are sustained throughout
 * the trading session, indicating genuine bullish momentum rather than a false
 * breakout that would fill the gap.
 * <p>
 * The key insight is that sustainable gap-ups show:
 * 1. Strong opening gap (typically > 1.5%)
 * 2. Price holds within the gap range (doesn't fall back to previous close)
 * 3. Closing near the high of the day (showing sustained buying interest)
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Gap up >= gapThreshold (default: 1.5%)</li>
 *   <li>Low of the day > previous close (gap holds, doesn't fill)</li>
 *   <li>Close in upper portion of range (close position > 0.6)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: At the day's low (gap fill level)</li>
 *   <li>Target: Entry + 2x risk (2:1 reward-risk ratio)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>gapThreshold: Minimum gap percentage (default: 0.015 = 1.5%)</li>
 *   <li>closePositionThreshold: Minimum close position in range (default: 0.6 = 60%)</li>
 *   <li>riskRewardRatio: Target multiplier (default: 2.0)</li>
 * </ul>
 */
@Component
@Qualifier("gapHoldStrategy")
public class GapHoldStrategy implements Strategy {

    private static final String CODE = "GAP_HOLD";
    private static final String NAME = "Gap-Up Hold Continuation";
    private static final double DEFAULT_GAP_THRESHOLD = 0.015; // 1.5%
    private static final double DEFAULT_CLOSE_POSITION_THRESHOLD = 0.6; // 60%
    private static final double DEFAULT_RISK_REWARD_RATIO = 2.0;

    @Override
    public StrategyResult evaluate(StrategyContext context) {
        List<Candle> candles = context.getCandles();
        StrategyParams params = context.getParams();

        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        final double gapThreshold = params.getDouble("gapThreshold", DEFAULT_GAP_THRESHOLD);
        final double closePositionThreshold = params.getDouble("closePositionThreshold", DEFAULT_CLOSE_POSITION_THRESHOLD);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        if (gapThreshold <= 0 || gapThreshold >= 1) {
            throw new IllegalArgumentException("gapThreshold must be between 0 and 1 (exclusive)");
        }
        if (closePositionThreshold <= 0 || closePositionThreshold > 1) {
            throw new IllegalArgumentException("closePositionThreshold must be between 0 and 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least 2 candles
        if (candles.size() < 2) {
            return StrategyResult.empty(context.isScreening());
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            int latestIndex = candles.size() - 1;
            TradeSignal signal = checkGapHold(candles, latestIndex, gapThreshold,
                closePositionThreshold, riskRewardRatio);
            if (signal != null) {
                return StrategyResult.actionable(List.of(signal));
            }
            return StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = 1; i < candles.size(); i++) {
            TradeSignal signal = checkGapHold(candles, i, gapThreshold,
                closePositionThreshold, riskRewardRatio);
            if (signal != null) {
                signals.add(signal);
            }
        }

        return StrategyResult.nonActionable(signals);
    }

    /**
     * Checks for gap-hold pattern at the given index.
     *
     * @param candles List of candles
     * @param index Current candle index
     * @param gapThreshold Minimum gap percentage required
     * @param closePositionThreshold Minimum close position in range
     * @param riskRewardRatio Risk-reward ratio for target calculation
     * @return TradeSignal if pattern found, null otherwise
     */
    private TradeSignal checkGapHold(List<Candle> candles, int index,
                                     double gapThreshold, double closePositionThreshold,
                                     double riskRewardRatio) {
        if (index < 1) {
            return null;
        }

        Candle today = candles.get(index);
        Candle yesterday = candles.get(index - 1);

        if (!isValidCandle(today) || !isValidCandle(yesterday)) {
            return null;
        }

        BigDecimal prevClose = yesterday.getClose();
        BigDecimal todayOpen = today.getOpen();
        BigDecimal todayLow = today.getLow();
        BigDecimal todayHigh = today.getHigh();
        BigDecimal todayClose = today.getClose();

        // Calculate gap percentage
        BigDecimal gapPercent = todayOpen.subtract(prevClose)
            .divide(prevClose, 4, RoundingMode.HALF_UP);

        // Check gap threshold
        if (gapPercent.doubleValue() < gapThreshold) {
            return null;
        }

        // Check if gap holds (low doesn't fall back to previous close)
        if (todayLow.compareTo(prevClose) <= 0) {
            return null;
        }

        // Calculate close position in today's range
        double todayRange = todayHigh.subtract(todayLow).doubleValue();
        if (todayRange <= 0) {
            return null;
        }

        double closePosition = (todayClose.subtract(todayLow).doubleValue()) / todayRange;

        // Check if close is in upper portion of range
        if (closePosition < closePositionThreshold) {
            return null;
        }

        // All conditions met - create signal
        BigDecimal entryPrice = todayClose;
        BigDecimal stopLoss = todayLow; // Stop at gap fill level
        BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

        return TradeSignal.fromCandle(today, Side.BUY, entryPrice, stopLoss, targetPrice, 1);
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
            candle.getOpen().doubleValue() > 0 &&
            candle.getClose().doubleValue() > 0 &&
            candle.getHigh().doubleValue() > 0 &&
            candle.getLow().doubleValue() > 0;
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

    @Override
    public String toString() {
        return "GapHoldStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
