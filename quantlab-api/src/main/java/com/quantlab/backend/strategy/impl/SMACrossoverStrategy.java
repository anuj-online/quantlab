package com.quantlab.backend.strategy.impl;

import com.quantlab.backend.domain.TradeSignal;
import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Side;
import com.quantlab.backend.strategy.ExecutionMode;
import com.quantlab.backend.strategy.Strategy;
import com.quantlab.backend.strategy.StrategyContext;
import com.quantlab.backend.strategy.StrategyParams;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Simple Moving Average (SMA) Crossover Strategy.
 * <p>
 * Strategy Code: SMA_CROSSOVER
 * <p>
 * This strategy generates buy signals when a faster-moving average crosses above
 * a slower-moving average, indicating potential upward momentum. This is a classic
 * trend-following strategy that aims to capture the beginning of sustained uptrends.
 * <p>
 * Supports both BACKTEST and SCREEN execution modes:
 * <ul>
 *   <li><b>BACKTEST</b>: Generates historical signals across entire date range</li>
 *   <li><b>SCREEN</b>: Generates actionable signals for next trading day based on latest crossover</li>
 * </ul>
 * <p>
 * Entry Conditions:
 * <ul>
 *   <li>Previous day: fast SMA <= slow SMA (no crossover yet)</li>
 *   <li>Current day: fast SMA > slow SMA (crossover occurred)</li>
 * </ul>
 * <p>
 * Risk Management:
 * <ul>
 *   <li>Stop Loss: Below the recent low (lowest low in lookback period)</li>
 *   <li>Target: Entry + 1.5x risk (1.5:1 reward-risk ratio)</li>
 * </ul>
 * <p>
 * Parameters:
 * <ul>
 *   <li>fastSMA: Period for fast moving average (default: 9)</li>
 *   <li>slowSMA: Period for slow moving average (default: 21)</li>
 *   <li>lookbackDays: Period for stop loss calculation (default: 14)</li>
 *   <li>riskRewardRatio: Target multiplier (default: 1.5)</li>
 * </ul>
 * <p>
 * Notes:
 * <ul>
 *   <li>Fast SMA should be less than slow SMA for meaningful crossovers</li>
 *   <li>This strategy only generates BUY signals (bullish crossovers)</li>
 *   <li>In SCREEN mode, signals are marked as actionable for next trading day</li>
 * </ul>
 */
@Component
@Qualifier("smaCrossoverStrategy")
public class SMACrossoverStrategy implements Strategy {

    private static final String CODE = "SMA_CROSSOVER";
    private static final String NAME = "SMA Crossover";
    private static final int DEFAULT_FAST_PERIOD = 9;
    private static final int DEFAULT_SLOW_PERIOD = 21;
    private static final int DEFAULT_LOOKBACK_DAYS = 14;
    private static final double DEFAULT_RISK_REWARD_RATIO = 1.5;

    /**
     * Generates trading signals based on SMA crossover logic.
     * <p>
     * This implementation supports both BACKTEST and SCREEN modes through
     * the StrategyContext. In SCREEN mode, it only checks if a crossover
     * occurred on the latest candle for next-day action.
     *
     * @param candles List of historical candles sorted by trade_date ascending
     * @param params  Strategy parameters for signal generation
     * @return List of trading signals, empty list if no signals generated
     * @throws IllegalArgumentException if candles is null or empty
     */
    @Override
    public List<TradeSignal> generateSignals(List<Candle> candles, StrategyParams params) {
        // Validate input
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        // Get parameters with defaults
        final int fastPeriod = params.getInt("fastSMA", DEFAULT_FAST_PERIOD);
        final int slowPeriod = params.getInt("slowSMA", DEFAULT_SLOW_PERIOD);
        final int lookbackDays = params.getInt("lookbackDays", DEFAULT_LOOKBACK_DAYS);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        // Validate parameters
        if (fastPeriod < 1) {
            throw new IllegalArgumentException("fastSMA must be at least 1");
        }
        if (slowPeriod < 1) {
            throw new IllegalArgumentException("slowSMA must be at least 1");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException("fastSMA must be less than slowSMA for meaningful crossovers");
        }
        if (lookbackDays < 1) {
            throw new IllegalArgumentException("lookbackDays must be at least 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least slowPeriod + 1 candles to detect crossover
        if (candles.size() < slowPeriod + 1) {
            return signals; // Not enough data
        }

        // Calculate SMAs for each position and detect crossovers
        for (int i = slowPeriod; i < candles.size(); i++) {
            Candle today = candles.get(i);
            Candle yesterday = candles.get(i - 1);

            // Skip if candles have null values
            if (!isValidCandle(today) || !isValidCandle(yesterday)) {
                continue;
            }

            // Calculate SMAs for current and previous day
            OptionalDouble currentFastSMA = calculateSMA(candles, i, fastPeriod);
            OptionalDouble currentSlowSMA = calculateSMA(candles, i, slowPeriod);
            OptionalDouble prevFastSMA = calculateSMA(candles, i - 1, fastPeriod);
            OptionalDouble prevSlowSMA = calculateSMA(candles, i - 1, slowPeriod);

            // Skip if we couldn't calculate SMAs
            if (!currentFastSMA.isPresent() || !currentSlowSMA.isPresent() ||
                !prevFastSMA.isPresent() || !prevSlowSMA.isPresent()) {
                continue;
            }

            // Check for bullish crossover
            // Previous: fast <= slow (no uptrend)
            // Current: fast > slow (uptrend started)
            boolean wasBelowOrEqual = prevFastSMA.getAsDouble() <= prevSlowSMA.getAsDouble();
            boolean isNowAbove = currentFastSMA.getAsDouble() > currentSlowSMA.getAsDouble();

            if (wasBelowOrEqual && isNowAbove) {
                // Crossover detected - calculate stop loss and target
                BigDecimal entryPrice = today.getClose();
                BigDecimal stopLoss = calculateStopLoss(candles, i, lookbackDays);
                BigDecimal targetPrice = calculateTarget(entryPrice, stopLoss, riskRewardRatio);

                // Generate buy signal with full risk management
                TradeSignal signal = new TradeSignal(
                    today.getInstrument(),
                    today.getTradeDate(),
                    Side.BUY,
                    entryPrice,
                    stopLoss,
                    targetPrice,
                    1  // Default quantity
                );

                signals.add(signal);
            }
        }

        return signals;
    }

    /**
     * Evaluates the strategy using the new context-based API.
     * <p>
     * This method supports both BACKTEST and SCREEN execution modes:
     * <ul>
     *   <li>BACKTEST: Generates signals across all historical candles</li>
     *   <li>SCREEN: Only checks if crossover occurred on latest candle for next-day action</li>
     * </ul>
     *
     * @param context Strategy context containing candles, mode, and parameters
     * @return StrategyResult with signals and actionable status
     */
    @Override
    public com.quantlab.backend.strategy.StrategyResult evaluate(com.quantlab.backend.strategy.StrategyContext context) {
        List<Candle> candles = context.getCandles();
        com.quantlab.backend.strategy.StrategyParams params = context.getParams();

        // Validate input
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }

        // Get parameters with defaults
        final int fastPeriod = params.getInt("fastSMA", DEFAULT_FAST_PERIOD);
        final int slowPeriod = params.getInt("slowSMA", DEFAULT_SLOW_PERIOD);
        final int lookbackDays = params.getInt("lookbackDays", DEFAULT_LOOKBACK_DAYS);
        final double riskRewardRatio = params.getDouble("riskRewardRatio", DEFAULT_RISK_REWARD_RATIO);

        // Validate parameters
        if (fastPeriod < 1) {
            throw new IllegalArgumentException("fastSMA must be at least 1");
        }
        if (slowPeriod < 1) {
            throw new IllegalArgumentException("slowSMA must be at least 1");
        }
        if (fastPeriod >= slowPeriod) {
            throw new IllegalArgumentException("fastSMA must be less than slowSMA for meaningful crossovers");
        }
        if (lookbackDays < 1) {
            throw new IllegalArgumentException("lookbackDays must be at least 1");
        }
        if (riskRewardRatio <= 0) {
            throw new IllegalArgumentException("riskRewardRatio must be positive");
        }

        List<TradeSignal> signals = new ArrayList<>();

        // Need at least slowPeriod + 1 candles to detect crossover
        if (candles.size() < slowPeriod + 1) {
            return com.quantlab.backend.strategy.StrategyResult.empty(context.isScreening());
        }

        // In SCREEN mode, only evaluate the latest candle
        if (context.isScreening()) {
            Candle today = candles.get(candles.size() - 1);
            int todayIndex = candles.size() - 1;

            if (!isValidCandle(today) || todayIndex < slowPeriod) {
                return com.quantlab.backend.strategy.StrategyResult.empty(true);
            }

            // Calculate SMAs for current and previous day
            OptionalDouble currentFastSMA = calculateSMA(candles, todayIndex, fastPeriod);
            OptionalDouble currentSlowSMA = calculateSMA(candles, todayIndex, slowPeriod);
            OptionalDouble prevFastSMA = calculateSMA(candles, todayIndex - 1, fastPeriod);
            OptionalDouble prevSlowSMA = calculateSMA(candles, todayIndex - 1, slowPeriod);

            // Skip if we couldn't calculate SMAs
            if (!currentFastSMA.isPresent() || !currentSlowSMA.isPresent() ||
                !prevFastSMA.isPresent() || !prevSlowSMA.isPresent()) {
                return com.quantlab.backend.strategy.StrategyResult.empty(true);
            }

            // Check for bullish crossover
            boolean wasBelowOrEqual = prevFastSMA.getAsDouble() <= prevSlowSMA.getAsDouble();
            boolean isNowAbove = currentFastSMA.getAsDouble() > currentSlowSMA.getAsDouble();

            if (wasBelowOrEqual && isNowAbove) {
                BigDecimal entryPrice = today.getClose();
                BigDecimal stopLoss = calculateStopLoss(candles, todayIndex, lookbackDays);
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
                return com.quantlab.backend.strategy.StrategyResult.actionable(List.of(signal));
            }

            return com.quantlab.backend.strategy.StrategyResult.empty(true);
        }

        // BACKTEST mode: process all candles
        for (int i = slowPeriod; i < candles.size(); i++) {
            Candle today = candles.get(i);
            Candle yesterday = candles.get(i - 1);

            // Skip if candles have null values
            if (!isValidCandle(today) || !isValidCandle(yesterday)) {
                continue;
            }

            // Calculate SMAs for current and previous day
            OptionalDouble currentFastSMA = calculateSMA(candles, i, fastPeriod);
            OptionalDouble currentSlowSMA = calculateSMA(candles, i, slowPeriod);
            OptionalDouble prevFastSMA = calculateSMA(candles, i - 1, fastPeriod);
            OptionalDouble prevSlowSMA = calculateSMA(candles, i - 1, slowPeriod);

            // Skip if we couldn't calculate SMAs
            if (!currentFastSMA.isPresent() || !currentSlowSMA.isPresent() ||
                !prevFastSMA.isPresent() || !prevSlowSMA.isPresent()) {
                continue;
            }

            // Check for bullish crossover
            boolean wasBelowOrEqual = prevFastSMA.getAsDouble() <= prevSlowSMA.getAsDouble();
            boolean isNowAbove = currentFastSMA.getAsDouble() > currentSlowSMA.getAsDouble();

            if (wasBelowOrEqual && isNowAbove) {
                BigDecimal entryPrice = today.getClose();
                BigDecimal stopLoss = calculateStopLoss(candles, i, lookbackDays);
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

        return com.quantlab.backend.strategy.StrategyResult.nonActionable(signals);
    }

    /**
     * Calculates stop loss based on recent low in the lookback period.
     * <p>
     * Stop loss is placed below the lowest low in the specified lookback period.
     * This provides a buffer below recent support levels.
     *
     * @param candles    List of candles
     * @param endIndex   Index of current candle
     * @param lookbackDays Number of days to look back for stop loss calculation
     * @return Stop loss price
     */
    private BigDecimal calculateStopLoss(List<Candle> candles, int endIndex, int lookbackDays) {
        int startIndex = Math.max(0, endIndex - lookbackDays);

        OptionalDouble lowestLow = candles.subList(startIndex, endIndex + 1).stream()
            .filter(this::isValidCandle)
            .mapToDouble(c -> c.getLow().doubleValue())
            .min();

        if (lowestLow.isPresent()) {
            // Place stop loss slightly below the recent low (2% below)
            return BigDecimal.valueOf(lowestLow.getAsDouble())
                .multiply(BigDecimal.valueOf(0.98))
                .setScale(2, RoundingMode.HALF_UP);
        }

        // Fallback: use current candle's low
        return candles.get(endIndex).getLow()
            .multiply(BigDecimal.valueOf(0.98))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates target price based on risk-reward ratio.
     * <p>
     * Target = Entry + (Entry - StopLoss) * RiskRewardRatio
     *
     * @param entryPrice   Entry price
     * @param stopLoss     Stop loss price
     * @param riskRewardRatio Risk-reward ratio (e.g., 1.5 for 1.5:1)
     * @return Target price
     */
    private BigDecimal calculateTarget(BigDecimal entryPrice, BigDecimal stopLoss, double riskRewardRatio) {
        BigDecimal risk = entryPrice.subtract(stopLoss);
        BigDecimal reward = risk.multiply(BigDecimal.valueOf(riskRewardRatio));
        return entryPrice.add(reward).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates Simple Moving Average for the candle at position endIndex,
     * looking back 'period' candles.
     *
     * @param candles    List of candles
     * @param endIndex   Index of current candle
     * @param period     Number of periods for SMA
     * @return Optional containing SMA value, or empty if calculation fails
     */
    private OptionalDouble calculateSMA(List<Candle> candles, int endIndex, int period) {
        if (endIndex < period - 1) {
            return OptionalDouble.empty();
        }

        int startIndex = endIndex - period + 1;

        return candles.subList(startIndex, endIndex + 1).stream()
            .filter(this::isValidCandle)
            .mapToDouble(c -> c.getClose().doubleValue())
            .average();
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
        // Need at least slowPeriod + 1, using default slowPeriod of 21
        // Plus lookbackDays for stop loss calculation
        return DEFAULT_SLOW_PERIOD + 1;
    }

    @Override
    public String toString() {
        return "SMACrossoverStrategy{code='" + CODE + "', name='" + NAME + "'}";
    }
}
