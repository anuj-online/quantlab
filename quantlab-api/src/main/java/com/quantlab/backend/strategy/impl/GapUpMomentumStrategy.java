package com.quantlab.backend.strategy.impl;

import com.quantlab.backend.domain.TradeSignal;
import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Side;
import com.quantlab.backend.strategy.Strategy;
import com.quantlab.backend.strategy.StrategyParams;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Gap Up Momentum Strategy.
 * <p>
 * Strategy Code: GAP_UP_MOMENTUM
 * <p>
 * This strategy identifies bullish gap-up openings, which occur when a stock opens
 * significantly higher than the previous day's close. Large gap ups often indicate
 * strong positive sentiment, news-driven buying interest, or institutional accumulation.
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Gap percentage >= gapThreshold (default: 2%)</li>
 *   <li>Gap percentage = (open - previousClose) / previousClose</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss = entry × 0.99 (1% below entry price)</li>
 *   <li>Target: Not set by this strategy (momentum-based exit)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>gapPercent: Minimum gap percentage required (default: 0.02 = 2%)</li>
 * </ul>
 * <p>
 * Trading Notes:
 * <ul>
 *   <li>Gap ups are most effective when accompanied by high volume</li>
 *   <li>Best results in strong market uptrends</li>
 *   <li>Consider filtering for stocks with strong fundamentals</li>
 *   <li>Gap fill risk: price may drop back to previous close</li>
 *   <li>1% stop loss balances risk-reward for typical 2% gap plays</li>
 * </ul>
 */
@Component
@Qualifier("gapUpMomentumStrategy")
public class GapUpMomentumStrategy implements Strategy {

    private static final String CODE = "GAP_UP_MOMENTUM";
    private static final String NAME = "Gap Up Momentum";
    private static final BigDecimal STOP_LOSS_MULTIPLIER = new BigDecimal("0.99");

    @Override
    public List<TradeSignal> generateSignals(List<Candle> candles, StrategyParams params) {
        // Validate input
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        // Get parameters with defaults
        final double gapThreshold = params.getDouble("gapPercent", 0.02);

        // Validate parameters
        if (gapThreshold <= 0) {
            throw new IllegalArgumentException("gapPercent must be positive");
        }
        if (gapThreshold >= 1) {
            throw new IllegalArgumentException("gapPercent should be less than 1 (100%)");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least 2 candles (previous day and current day)
        if (candles.size() < 2) {
            return signals; // Not enough data
        }

        // Start from index 1 to have previous day data
        for (int i = 1; i < candles.size(); i++) {
            Candle today = candles.get(i);
            Candle yesterday = candles.get(i - 1);

            // Skip if candles have null values
            if (!isValidCandle(today) || !isValidCandle(yesterday)) {
                continue;
            }

            // Calculate gap percentage
            BigDecimal prevClose = yesterday.getClose();
            BigDecimal todayOpen = today.getOpen();

            // Gap = (open - prevClose) / prevClose
            BigDecimal gapPercent = todayOpen.subtract(prevClose)
                .divide(prevClose, 4, RoundingMode.HALF_UP);

            // Check if gap meets threshold
            if (gapPercent.doubleValue() >= gapThreshold) {
                // Calculate stop loss (1% below entry/open price)
                BigDecimal entryPrice = todayOpen;
                BigDecimal stopLoss = entryPrice.multiply(STOP_LOSS_MULTIPLIER)
                    .setScale(2, RoundingMode.HALF_UP);

                // Create buy signal
                TradeSignal signal = TradeSignal.fromCandle(
                    today,
                    Side.BUY,
                    entryPrice,
                    stopLoss,
                    null,  // No target set for momentum strategy
                    1      // Default quantity
                );

                signals.add(signal);
            }
        }

        return signals;
    }

    /**
     * Checks if a candle has valid price data.
     * For gap analysis, we need valid open and close prices.
     */
    private boolean isValidCandle(Candle candle) {
        return candle != null &&
            candle.getOpen() != null &&
            candle.getClose() != null &&
            candle.getOpen().doubleValue() > 0 &&
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
        // Need at least 2 candles (previous + current)
        return 2;
    }

    @Override
    public String toString() {
        return "GapUpMomentumStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
