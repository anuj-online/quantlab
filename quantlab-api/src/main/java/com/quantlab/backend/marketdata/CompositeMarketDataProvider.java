package com.quantlab.backend.marketdata;

import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.config.YahooFinanceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Composite market data provider that merges data from database and Yahoo Finance.
 * <p>
 * Data Source Hierarchy:
 * 1. Database (Bhavcopy/Stooq) - canonical source
 * 2. Yahoo Finance - fills gaps only, never overwrites database data
 * <p>
 * Merge Rules:
 * - Database candles take precedence on date overlap
 * - Yahoo Finance fills missing dates at the end of the range
 * - No automatic persistence to database
 * - Graceful degradation (returns available data, never throws)
 */
@Component
public class CompositeMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(CompositeMarketDataProvider.class);

    private final DbMarketDataProvider dbProvider;
    private final YahooMarketDataProvider yahooProvider;
    private final YahooFinanceConfiguration config;

    @Value("${marketdata.backtest.allowYahooFallback:false}")
    private boolean allowYahooFallbackForBacktest;

    @Value("${marketdata.screening.requireLatestData:true}")
    private boolean requireLatestDataForScreening;

    private final CircuitBreaker circuitBreaker;

    public CompositeMarketDataProvider(DbMarketDataProvider dbProvider,
                                       YahooMarketDataProvider yahooProvider,
                                       YahooFinanceConfiguration config) {
        this.dbProvider = dbProvider;
        this.yahooProvider = yahooProvider;
        this.config = config;
        this.circuitBreaker = new CircuitBreaker();
    }

    /**
     * Get daily candles with automatic gap filling from Yahoo Finance.
     *
     * @param symbol The trading symbol
     * @param from   Start date (inclusive)
     * @param to     End date (inclusive)
     * @return Merged list of candles from database and Yahoo Finance
     */
    @Override
    public List<Candle> getDailyCandles(String symbol, LocalDate from, LocalDate to) {
        MDC.put("dataSource", "COMPOSITE");
        MDC.put("symbol", symbol);

        try {
            // Step 1: Fetch from database (canonical source)
            List<Candle> dbCandles = dbProvider.getDailyCandles(symbol, from, to);
            LocalDate lastDbDate = getLastDate(dbCandles);

            log.debug("DB returned {} candles, last date: {}", dbCandles.size(), lastDbDate);

            // Step 2: Check if we need Yahoo Finance data
            boolean needYahooData = lastDbDate == null || lastDbDate.isBefore(to);

            if (!needYahooData) {
                log.debug("Database has complete coverage for {} ({} to {})", symbol, from, to);
                return dbCandles;
            }

            // Step 3: Fetch gap from Yahoo Finance
            LocalDate yahooFrom = lastDbDate == null ? from : lastDbDate.plusDays(1);
            List<Candle> yahooCandles = fetchFromYahooWithCircuitBreaker(symbol, yahooFrom, to);

            if (yahooCandles.isEmpty()) {
                log.info("Yahoo Finance returned no data for {} ({} to {}), using DB only",
                        symbol, yahooFrom, to);
                return dbCandles;
            }

            // Step 4: Merge and return
            List<Candle> merged = mergeAndSort(dbCandles, yahooCandles);
            log.info("Merged {} candles (DB: {}, Yahoo: {}) for {} ({} to {})",
                    merged.size(), dbCandles.size(), yahooCandles.size(), symbol, from, to);

            return merged;

        } finally {
            MDC.remove("dataSource");
            MDC.remove("symbol");
        }
    }

    /**
     * Get candles for screening mode.
     * In screening mode, we always try to get the latest data.
     */
    public List<Candle> getDailyCandlesForScreening(String symbol, LocalDate from, LocalDate to) {
        List<Candle> candles = getDailyCandles(symbol, from, to);

        if (requireLatestDataForScreening && !candles.isEmpty()) {
            LocalDate lastDate = getLastDate(candles);
            if (lastDate.isBefore(to)) {
                log.warn("Screening data for {} is stale: latest is {}, requested {}",
                        symbol, lastDate, to);
            }
        }

        return candles;
    }

    /**
     * Get candles for backtest mode.
     * In backtest mode, Yahoo Finance fallback is optional (configurable).
     */
    public List<Candle> getDailyCandlesForBacktest(String symbol, LocalDate from, LocalDate to) {
        if (!allowYahooFallbackForBacktest) {
            log.debug("Yahoo Finance fallback disabled for backtest, using DB only");
            return dbProvider.getDailyCandles(symbol, from, to);
        }

        return getDailyCandles(symbol, from, to);
    }

    @Override
    public boolean isAvailable() {
        return dbProvider.isAvailable();
    }

    /**
     * Fetch from Yahoo Finance with circuit breaker protection.
     */
    private List<Candle> fetchFromYahooWithCircuitBreaker(String symbol, LocalDate from, LocalDate to) {
        if (circuitBreaker.isOpen(symbol)) {
            log.warn("Circuit breaker open for {}, skipping Yahoo Finance request", symbol);
            return Collections.emptyList();
        }

        try {
            List<Candle> candles = yahooProvider.getDailyCandles(symbol, from, to);

            if (candles.isEmpty()) {
                circuitBreaker.recordFailure(symbol);
            } else {
                circuitBreaker.recordSuccess(symbol);
            }

            return candles;

        } catch (Exception e) {
            circuitBreaker.recordFailure(symbol);
            log.warn("Yahoo Finance fetch failed for {}: {}", symbol, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Merge candles from database and Yahoo Finance.
     * Database candles take precedence on overlap.
     */
    private List<Candle> mergeAndSort(List<Candle> dbCandles, List<Candle> yahooCandles) {
        // Create a map for quick lookup by date
        Map<LocalDate, Candle> merged = new TreeMap<>();

        // Add database candles first (they take precedence)
        for (Candle candle : dbCandles) {
            merged.put(candle.getTradeDate(), candle);
        }

        // Add Yahoo candles only for missing dates
        for (Candle candle : yahooCandles) {
            merged.putIfAbsent(candle.getTradeDate(), candle);
        }

        return new ArrayList<>(merged.values());
    }

    /**
     * Get the last trade date from a list of candles.
     */
    private LocalDate getLastDate(List<Candle> candles) {
        return candles.stream()
                .map(Candle::getTradeDate)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    /**
     * Simple circuit breaker to prevent cascade failures.
     * Tracks consecutive failures per symbol.
     */
    public static class CircuitBreaker {
        private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);
        private static final int FAILURE_THRESHOLD = 3;
        private static final long HALF_OPEN_AFTER_MS = 60_000; // 1 minute

        private final Map<String, CircuitBreakerState> states = new HashMap<>();

        public boolean isOpen(String symbol) {
            CircuitBreakerState state = states.get(symbol);
            if (state == null) {
                return false;
            }

            if (state.open && (System.currentTimeMillis() - state.lastFailureTime > HALF_OPEN_AFTER_MS)) {
                // Transition to half-open
                log.debug("Circuit breaker transitioning to half-open for {}", symbol);
                state.open = false;
                return false;
            }

            return state.open;
        }

        public void recordFailure(String symbol) {
            CircuitBreakerState state = states.computeIfAbsent(
                    symbol, k -> new CircuitBreakerState());
            state.consecutiveFailures++;

            if (state.consecutiveFailures >= FAILURE_THRESHOLD) {
                log.warn("Circuit breaker opened for {} after {} failures",
                        symbol, state.consecutiveFailures);
                state.open = true;
            }

            state.lastFailureTime = System.currentTimeMillis();
        }

        public void recordSuccess(String symbol) {
            CircuitBreakerState state = states.get(symbol);
            if (state != null) {
                state.consecutiveFailures = 0;
                state.open = false;
            }
        }

        private static class CircuitBreakerState {
            boolean open = false;
            int consecutiveFailures = 0;
            long lastFailureTime = 0;
        }
    }
}
