package com.quantlab.backend.strategy;

import com.quantlab.backend.domain.TradeSignal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result of strategy evaluation containing generated signals and actionable status.
 * <p>
 * This class wraps the output of strategy execution, providing both the signals
 * and metadata about whether those signals are actionable for the next trading day.
 * <p>
 * In BACKTEST mode, isActionable is typically false as signals are historical.
 * In SCREEN mode, isActionable indicates whether the signal should be acted upon
 * in the next trading session.
 *
 * @author QuantLab Team
 * @version 1.0
 */
public class StrategyResult {

    private final List<TradeSignal> signals;
    private final boolean isActionable;

    /**
     * Creates a strategy result with signals and actionable status.
     *
     * @param signals      List of generated trading signals (empty list if none)
     * @param isActionable True if signals are actionable for next trading day
     */
    public StrategyResult(List<TradeSignal> signals, boolean isActionable) {
        this.signals = signals != null ? List.copyOf(signals) : Collections.emptyList();
        this.isActionable = isActionable;
    }

    /**
     * Creates a result with no signals.
     *
     * @param isActionable True if this state is actionable (e.g., no signals but strategy ran)
     * @return Empty strategy result
     */
    public static StrategyResult empty(boolean isActionable) {
        return new StrategyResult(Collections.emptyList(), isActionable);
    }

    /**
     * Creates a result with actionable signals for screening.
     *
     * @param signals List of signals generated
     * @return Strategy result with actionable signals
     */
    public static StrategyResult actionable(List<TradeSignal> signals) {
        return new StrategyResult(signals, true);
    }

    /**
     * Creates a result with non-actionable signals (e.g., backtest signals).
     *
     * @param signals List of signals generated
     * @return Strategy result with non-actionable signals
     */
    public static StrategyResult nonActionable(List<TradeSignal> signals) {
        return new StrategyResult(signals, false);
    }

    /**
     * Returns the list of generated signals.
     * <p>
     * Returns an unmodifiable empty list if no signals were generated.
     *
     * @return List of trading signals (never null)
     */
    public List<TradeSignal> getSignals() {
        return signals;
    }

    /**
     * Returns whether these signals are actionable for the next trading day.
     * <p>
     * In SCREEN mode, this indicates the signals should be traded tomorrow.
     * In BACKTEST mode, this is typically false as signals are historical.
     *
     * @return true if signals are actionable for next trading session
     */
    public boolean isActionable() {
        return isActionable;
    }

    /**
     * Returns the number of signals generated.
     *
     * @return Signal count
     */
    public int getSignalCount() {
        return signals.size();
    }

    /**
     * Returns true if no signals were generated.
     *
     * @return true if signal list is empty
     */
    public boolean isEmpty() {
        return signals.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrategyResult that = (StrategyResult) o;
        return isActionable == that.isActionable && Objects.equals(signals, that.signals);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signals, isActionable);
    }

    @Override
    public String toString() {
        return "StrategyResult{" +
                "signalCount=" + signals.size() +
                ", isActionable=" + isActionable +
                '}';
    }
}
