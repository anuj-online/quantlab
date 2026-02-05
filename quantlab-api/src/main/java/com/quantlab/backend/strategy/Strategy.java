package com.quantlab.backend.strategy;

import com.quantlab.backend.domain.TradeSignal;
import com.quantlab.backend.entity.Candle;

import java.util.List;

/**
 * Strategy interface for algorithmic trading strategies.
 * <p>
 * Each strategy implementation takes a strategy context containing historical price data,
 * execution mode (BACKTEST or SCREEN), run date, and parameters, then returns a strategy
 * result containing trading signals and actionable status.
 * <p>
 * Implementations must:
 * - Handle edge cases (insufficient data, null values)
 * - Be stateless and thread-safe for concurrent execution
 * - Use type-safe parameter access via StrategyParams
 * - Return domain TradeSignal objects (not JPA entities)
 * - Respect the ExecutionMode in the context (BACKTEST vs SCREEN behavior)
 * <p>
 * Execution Modes:
 * <ul>
 *   <li>BACKTEST - Generate signals across full historical date range</li>
 *   <li>SCREEN - Generate signals only for the latest candle if conditions are met</li>
 * </ul>
 */
public interface Strategy {

    /**
     * Evaluates the strategy with the given context and returns trading signals.
     * <p>
     * The context contains candles sorted by trade_date ascending, the execution mode,
     * run date, and strategy parameters. Strategies should adjust their behavior based
     * on the execution mode:
     * <ul>
     *   <li>BACKTEST: Process all candles and generate signals at each valid point</li>
     *   <li>SCREEN: Only evaluate the latest candle and generate signals if conditions are met</li>
     * </ul>
     *
     * @param context Strategy context containing candles, mode, run date, and parameters
     * @return StrategyResult containing signals and actionable status
     * @throws IllegalArgumentException if context is null or candles are invalid
     */
    StrategyResult evaluate(StrategyContext context);

    /**
     * Legacy method for backward compatibility.
     * Generates trading signals based on historical candle data and parameters.
     * <p>
     * This method is deprecated in favor of {@link #evaluate(StrategyContext)}.
     * Implementations should delegate to evaluate() with a BACKTEST context.
     *
     * @param candles List of historical candles sorted by trade_date ascending
     * @param params  Strategy parameters for signal generation
     * @return List of trading signals, empty list if no signals generated
     * @throws IllegalArgumentException if candles is null or empty
     * @deprecated Use {@link #evaluate(StrategyContext)} instead
     */
    @Deprecated
    default List<TradeSignal> generateSignals(List<Candle> candles, StrategyParams params) {
        // Default implementation delegates to evaluate for backward compatibility
        StrategyContext context = new StrategyContext(
            candles,
            ExecutionMode.BACKTEST,
            candles.isEmpty() ? null : candles.get(candles.size() - 1).getTradeDate(),
            params
        );
        return evaluate(context).getSignals();
    }

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
