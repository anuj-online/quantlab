package com.quantlab.backend.strategy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Type-safe parameter container for trading strategies.
 * <p>
 * Provides convenient methods for accessing parameters with default values.
 * Strategies define their expected parameters and use this class for
 * type-safe access without casting or null checks.
 * <p>
 * Example usage:
 * <pre>
 * int lookback = params.getInt("lookbackDays", 20);
 * double volumeMultiplier = params.getDouble("volumeMultiplier", 1.5);
 * </pre>
 */
public class StrategyParams {

    private final Map<String, Object> params;

    public StrategyParams() {
        this.params = new HashMap<>();
    }

    public StrategyParams(Map<String, Object> params) {
        this.params = params != null ? new HashMap<>(params) : new HashMap<>();
    }

    /**
     * Builder pattern for fluent parameter construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets an integer parameter value.
     *
     * @param key         Parameter key
     * @param defaultValue Default value if key not found or invalid type
     * @return Integer value or default
     */
    public int getInt(String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a double parameter value.
     *
     * @param key         Parameter key
     * @param defaultValue Default value if key not found or invalid type
     * @return Double value or default
     */
    public double getDouble(String key, double defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a long parameter value.
     *
     * @param key         Parameter key
     * @param defaultValue Default value if key not found or invalid type
     * @return Long value or default
     */
    public long getLong(String key, long defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Gets a boolean parameter value.
     *
     * @param key         Parameter key
     * @param defaultValue Default value if key not found or invalid type
     * @return Boolean value or default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Gets a String parameter value.
     *
     * @param key         Parameter key
     * @param defaultValue Default value if key not found
     * @return String value or default
     */
    public String getString(String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * Checks if a parameter exists.
     */
    public boolean contains(String key) {
        return params.containsKey(key);
    }

    /**
     * Returns the number of parameters.
     */
    public int size() {
        return params.size();
    }

    /**
     * Returns the underlying parameter map (unmodifiable).
     */
    public Map<String, Object> toMap() {
        return Map.copyOf(params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrategyParams that = (StrategyParams) o;
        return Objects.equals(params, that.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(params);
    }

    @Override
    public String toString() {
        return "StrategyParams{" + params + "}";
    }

    /**
     * Builder for fluent StrategyParams construction.
     */
    public static class Builder {
        private final Map<String, Object> params = new HashMap<>();

        public Builder put(String key, int value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, double value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, long value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, boolean value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, String value) {
            params.put(key, value);
            return this;
        }

        public Builder put(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public StrategyParams build() {
            return new StrategyParams(params);
        }
    }
}
