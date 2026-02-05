package com.quantlab.backend.strategy;

import com.quantlab.backend.domain.TradeSignal;
import com.quantlab.backend.entity.Candle;

import java.util.List;

/**
 * Strategy interface for algorithmic trading strategies.
 * <p>
 * Each strategy implementation takes a list of candles (historical price data)
 * and parameters, then returns a list of trading signals.
 * <p>
 * Implementations must:
 * - Handle edge cases (insufficient data, null values)
 * - Be stateless and thread-safe for concurrent execution
 * - Use type-safe parameter access via StrategyParams
 * - Return domain TradeSignal objects (not JPA entities)
 */
public interface Strategy {

    /**
     * Generates trading signals based on historical candle data and parameters.
     *
     * @param candles List of historical candles sorted by trade_date ascending
     * @param params  Strategy parameters for signal generation
     * @return List of trading signals, empty list if no signals generated
     * @throws IllegalArgumentException if candles is null or empty
     */
    List<TradeSignal> generateSignals(List<Candle> candles, StrategyParams params);

    /**
     * Returns the unique code identifying this strategy.
     * This code should match the strategy entry in the database.
     *
     * @return Strategy code (e.g., "EMA_BREAKOUT", "SMA_CROSSOVER")
     */
    String getCode();

    /**
     * Returns a human-readable name for this strategy.
     *
     * @return Strategy name (e.g., "EOD Breakout with Volume")
     */
    String getName();

    /**
     * Returns the minimum number of candles required for this strategy to generate signals.
     * This is useful for validation before running the strategy.
     *
     * @return Minimum required candles
     */
    int getMinCandlesRequired();
}
