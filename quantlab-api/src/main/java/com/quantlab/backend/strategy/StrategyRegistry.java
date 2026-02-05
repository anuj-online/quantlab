package com.quantlab.backend.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for managing all trading strategies in the QuantLab platform.
 * <p>
 * This Spring service acts as a strategy factory and lookup mechanism, allowing
 * strategies to be registered at startup and retrieved by their unique code.
 * It maintains thread-safe access to all registered strategies.
 * <p>
 * Strategies can be registered:
 * <ul>
 *   <li>Automatically via Spring dependency injection</li>
 *   <li>Manually via {@link #register(String, Strategy)} method</li>
 * </ul>
 *
 * @author QuantLab Team
 * @version 1.0
 */
@Service
public class StrategyRegistry {

    private static final Logger logger = LoggerFactory.getLogger(StrategyRegistry.class);

    /**
     * Thread-safe map of registered strategies.
     * Key: Strategy code (unique identifier)
     * Value: Strategy instance
     */
    private final Map<String, Strategy> strategies = new ConcurrentHashMap<>();

    /**
     * Registers a strategy with the given code.
     * <p>
     * If a strategy with the same code already exists, it will be replaced
     * with a warning log. Strategy codes should follow naming conventions:
     * lowercase, hyphen-separated (e.g., "sma-crossover", "rsi-mean-reversion").
     *
     * @param code     Unique identifier for this strategy (must not be null/empty)
     * @param strategy Strategy instance to register (must not be null)
     * @throws IllegalArgumentException if code or strategy is null/empty
     */
    public void register(String code, Strategy strategy) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Strategy code cannot be null or empty");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }

        String normalizedCode = code.toLowerCase().trim();

        if (strategies.containsKey(normalizedCode)) {
            logger.warn("Strategy '{}' already registered. Replacing with new instance.", normalizedCode);
        }

        strategies.put(normalizedCode, strategy);
        logger.info("Registered strategy: '{}' - {}", normalizedCode, strategy.getName());
    }

    /**
     * Retrieves a strategy by its code.
     *
     * @param code Unique identifier of the strategy
     * @return Strategy instance
     * @throws IllegalArgumentException if code is null/empty
     * @throws StrategyNotFoundException if no strategy found with given code
     */
    public Strategy getStrategy(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Strategy code cannot be null or empty");
        }

        String normalizedCode = code.toLowerCase().trim();
        Strategy strategy = strategies.get(normalizedCode);

        if (strategy == null) {
            throw new StrategyNotFoundException(
                    "No strategy found with code: '" + code + "'. " +
                            "Available strategies: " + getRegisteredCodes()
            );
        }

        return strategy;
    }

    /**
     * Retrieves a strategy by its code, returning empty Optional if not found.
     * <p>
     * This is a safer alternative to {@link #getStrategy(String)} when you
     * want to handle missing strategies gracefully.
     *
     * @param code Unique identifier of the strategy
     * @return Optional containing the strategy, or empty if not found
     */
    public Optional<Strategy> findStrategy(String code) {
        if (code == null || code.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalizedCode = code.toLowerCase().trim();
        return Optional.ofNullable(strategies.get(normalizedCode));
    }

    /**
     * Returns metadata for all registered strategies.
     * <p>
     * The returned list is sorted by strategy code for consistent ordering.
     *
     * @return List of strategy metadata (never null, may be empty)
     */
    public List<StrategyMetadata> getAllStrategies() {
        List<StrategyMetadata> metadataList = new ArrayList<>();

        for (Map.Entry<String, Strategy> entry : strategies.entrySet()) {
            String code = entry.getKey();
            Strategy strategy = entry.getValue();

            metadataList.add(new StrategyMetadata(
                    code,
                    strategy.getName(),
                    "",  // No separate description field in Strategy interface
                    strategy.getMinCandlesRequired()
            ));
        }

        // Sort by code for consistent ordering
        metadataList.sort(Comparator.comparing(StrategyMetadata::getCode));

        return Collections.unmodifiableList(metadataList);
    }

    /**
     * Returns all registered strategy codes.
     *
     * @return Set of strategy codes (unmodifiable)
     */
    public Set<String> getRegisteredCodes() {
        return Collections.unmodifiableSet(strategies.keySet());
    }

    /**
     * Returns the total number of registered strategies.
     *
     * @return Strategy count
     */
    public int getStrategyCount() {
        return strategies.size();
    }

    /**
     * Checks if a strategy is registered with the given code.
     *
     * @param code Strategy code to check
     * @return true if registered, false otherwise
     */
    public boolean isRegistered(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return strategies.containsKey(code.toLowerCase().trim());
    }

    /**
     * Unregisters a strategy by its code.
     * <p>
     * Use with caution - this will make the strategy unavailable for future use.
     *
     * @param code Strategy code to unregister
     * @return true if strategy was removed, false if it didn't exist
     */
    public boolean unregister(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        String normalizedCode = code.toLowerCase().trim();
        Strategy removed = strategies.remove(normalizedCode);

        if (removed != null) {
            logger.info("Unregistered strategy: '{}'", normalizedCode);
            return true;
        }

        return false;
    }

    /**
     * Clears all registered strategies.
     * <p>
     * Primarily used for testing purposes.
     */
    public void clear() {
        logger.warn("Clearing all registered strategies");
        strategies.clear();
    }

    /**
     * Converts a strategy code to a readable name.
     * <p>
     * Example: "sma-crossover" -> "SMA Crossover"
     *
     * @param code Strategy code
     * @return Formatted strategy name
     */
    private String formatStrategyName(String code) {
        if (code == null || code.isEmpty()) {
            return "";
        }

        // Capitalize first letter of each word
        return Arrays.stream(code.split("-"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .reduce((a, b) -> a + " " + b)
                .orElse(code);
    }

    /**
     * Custom exception thrown when a requested strategy is not found.
     */
    public static class StrategyNotFoundException extends RuntimeException {
        public StrategyNotFoundException(String message) {
            super(message);
        }

        public StrategyNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
