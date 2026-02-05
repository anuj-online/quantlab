package com.quantlab.backend.strategy;

import com.quantlab.backend.entity.Candle;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Context object passed to strategies during signal generation.
 * <p>
 * This class encapsulates all the information a strategy needs to evaluate
 * market conditions and generate trading signals. It supports both backtesting
 * and screening modes through the ExecutionMode enum.
 * <p>
 * Strategies use this context to:
 * <ul>
 *   <li>Access candle data (historical for backtest, recent for screen)</li>
 *   <li>Determine execution mode (affects signal generation behavior)</li>
 *   <li>Access the current run date (anchor for signal generation)</li>
 *   <li>Read strategy parameters</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>
 * {@code
 * public List<TradeSignal> generateSignals(List<Candle> candles, StrategyParams params) {
 *     // Backtest mode - process all candles
 *     for (int i = minRequired; i < candles.size(); i++) {
 *         // Generate signal at each point
 *     }
 * }
 * }
 * </pre>
 * <p>
 * This is an immutable value object - once created, it cannot be modified.
 *
 * @author QuantLab Team
 * @version 1.0
 * @see ExecutionMode
 * @see StrategyParams
 */
public class StrategyContext {

    private final List<Candle> candles;
    private final ExecutionMode mode;
    private final LocalDate runDate;
    private final StrategyParams params;

    /**
     * Creates a new strategy context with all required parameters.
     *
     * @param candles List of candles sorted by trade_date ascending.
     *                For BACKTEST: full historical data
     *                For SCREEN: only recent N candles needed for calculation
     * @param mode    Execution mode (BACKTEST or SCREEN)
     * @param runDate The "current" date for this evaluation run.
     *                In SCREEN mode, this is typically the latest candle date.
     *                In BACKTEST mode, this is the end date of the backtest period.
     * @param params  Strategy parameters for signal generation
     * @throws IllegalArgumentException if candles is null or empty, runDate is null, or params is null
     */
    public StrategyContext(List<Candle> candles, ExecutionMode mode, LocalDate runDate, StrategyParams params) {
        if (candles == null || candles.isEmpty()) {
            throw new IllegalArgumentException("Candles list cannot be null or empty");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Execution mode cannot be null");
        }
        if (runDate == null) {
            throw new IllegalArgumentException("Run date cannot be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("Strategy params cannot be null");
        }

        this.candles = candles;
        this.mode = mode;
        this.runDate = runDate;
        this.params = params;
    }

    /**
     * Returns the candles for this evaluation.
     * <p>
     * The candles are guaranteed to be sorted by trade_date ascending.
     * The number of candles depends on the execution mode:
     * <ul>
     *   <li>BACKTEST: Full historical data for the date range</li>
     *   <li>SCREEN: Last N candles needed for indicator calculation (e.g., 50)</li>
     * </ul>
     *
     * @return Unmodifiable list of candles
     */
    public List<Candle> getCandles() {
        return candles;
    }

    /**
     * Returns the execution mode for this evaluation.
     * <p>
     * Strategies should use this to adjust their behavior:
     * <ul>
     *   <li>BACKTEST: Generate signals for each point in history</li>
     *   <li>SCREEN: Only check if signal exists at latest candle</li>
     * </ul>
     *
     * @return The execution mode
     */
    public ExecutionMode getMode() {
        return mode;
    }

    /**
     * Returns the run date for this evaluation.
     * <p>
     * This represents the "current" date for the strategy run.
     * In screening mode, signals generated on this date are actionable
     * for the next trading day.
     *
     * @return The run date
     */
    public LocalDate getRunDate() {
        return runDate;
    }

    /**
     * Returns the strategy parameters for this evaluation.
     *
     * @return The strategy parameters
     */
    public StrategyParams getParams() {
        return params;
    }

    /**
     * Convenience method to check if this is a screening run.
     *
     * @return true if mode is SCREEN, false otherwise
     */
    public boolean isScreening() {
        return mode == ExecutionMode.SCREEN;
    }

    /**
     * Convenience method to check if this is a backtest run.
     *
     * @return true if mode is BACKTEST, false otherwise
     */
    public boolean isBacktesting() {
        return mode == ExecutionMode.BACKTEST;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrategyContext that = (StrategyContext) o;
        return Objects.equals(candles, that.candles) &&
                mode == that.mode &&
                Objects.equals(runDate, that.runDate) &&
                Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candles, mode, runDate, params);
    }

    @Override
    public String toString() {
        return "StrategyContext{" +
                "candles=" + candles.size() + " candles" +
                ", mode=" + mode +
                ", runDate=" + runDate +
                ", params=" + params +
                '}';
    }
}
