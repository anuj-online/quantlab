package com.quantlab.backend.strategy;

import java.util.Objects;

/**
 * Data Transfer Object (DTO) representing metadata about a registered strategy.
 * <p>
 * This class is used to provide summary information about strategies without
 * exposing the full strategy implementation. It's returned by the StrategyRegistry
 * for API responses and UI displays.
 * <p>
 * This is an immutable value object - once created, it cannot be modified.
 *
 * @author QuantLab Team
 * @version 1.0
 */
public class StrategyMetadata {

    private final String code;
    private final String name;
    private final String description;
    private final int minimumCandlesRequired;

    /**
     * Creates a new StrategyMetadata instance.
     *
     * @param code                   Unique identifier for the strategy (lowercase, hyphen-separated)
     * @param name                   Human-readable display name
     * @param description            Brief description of the strategy logic
     * @param minimumCandlesRequired Minimum candles needed to generate signals
     */
    public StrategyMetadata(String code, String name, String description, int minimumCandlesRequired) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.minimumCandlesRequired = minimumCandlesRequired;
    }

    /**
     * Creates a new StrategyMetadata with default minimum candles (1).
     *
     * @param code        Unique identifier for the strategy
     * @param name        Human-readable display name
     * @param description Brief description of the strategy logic
     */
    public StrategyMetadata(String code, String name, String description) {
        this(code, name, description, 1);
    }

    /**
     * Returns the unique code identifier for this strategy.
     * <p>
     * Codes are lowercase and hyphen-separated (e.g., "sma-crossover").
     *
     * @return Strategy code (never null)
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the human-readable display name.
     * <p>
     * Names are title-cased for display (e.g., "SMA Crossover").
     *
     * @return Strategy name (never null)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a brief description of what this strategy does.
     * <p>
     * Should explain the basic logic and entry/exit conditions.
     *
     * @return Strategy description (never null)
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the minimum number of candles required for signal generation.
     * <p>
     * For example, a 20-period SMA strategy requires at least 20 candles.
     *
     * @return Minimum candles required (>= 1)
     */
    public int getMinimumCandlesRequired() {
        return minimumCandlesRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrategyMetadata that = (StrategyMetadata) o;
        return minimumCandlesRequired == that.minimumCandlesRequired &&
                Objects.equals(code, that.code) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, name, description, minimumCandlesRequired);
    }

    @Override
    public String toString() {
        return "StrategyMetadata{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", minimumCandlesRequired=" + minimumCandlesRequired +
                '}';
    }
}
