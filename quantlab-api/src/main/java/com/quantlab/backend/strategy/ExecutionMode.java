package com.quantlab.backend.strategy;

/**
 * Execution mode for strategy runs.
 * <p>
 * Defines how a strategy should be executed:
 * <ul>
 *   <li><b>BACKTEST</b>: Full historical backtesting with paper trade execution,
 *       analytics, equity curve, and performance metrics. This mode processes
 *       all historical data and simulates complete trade lifecycles.</li>
 *   <li><b>SCREEN</b>: Real-time screening mode that identifies current trading
 *       opportunities without executing trades. This mode analyzes the latest
 *       market data and returns signals for potential entry points.</li>
 * </ul>
 * <p>
 * The execution mode affects:
 * <ul>
 *   <li>Whether paper trades are executed</li>
 *   <li>Whether analytics are calculated</li>
 *   <li>Whether signals are persisted to database</li>
 *   <li>The scope of data processed (historical vs. latest)</li>
 * </ul>
 *
 * @author QuantLab Team
 * @version 1.0
 */
public enum ExecutionMode {

    /**
     * Backtesting mode - executes full historical simulation.
     * <p>
     * In this mode:
     * <ul>
     *   <li>All signals are generated for the historical date range</li>
     *   <li>Paper trades are executed with entry/exit logic</li>
     *   <li>Analytics (Sharpe ratio, max drawdown, win rate) are calculated</li>
     *   <li>Equity curve data is generated</li>
     *   <li>All signals, trades, and analytics are persisted</li>
     * </ul>
     */
    BACKTEST,

    /**
     * Screening mode - identifies current opportunities without execution.
     * <p>
     * In this mode:
     * <ul>
     *   <li>Only the latest signals are generated (current day)</li>
     *   <li>No paper trades are executed</li>
     *   <li>No analytics are calculated</li>
     *   <li>Signals may or may not be persisted (configurable)</li>
     *   <li>Results are returned as a screening snapshot</li>
     * </ul>
     */
    SCREEN
}
