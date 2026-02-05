package com.quantlab.backend.marketdata;

import com.quantlab.backend.entity.Candle;

import java.time.LocalDate;
import java.util.List;

/**
 * Unified market data provider interface.
 * All market data sources (database, Yahoo Finance, etc.) implement this interface.
 * This abstraction allows strategies to be source-agnostic.
 */
public interface MarketDataProvider {

    /**
     * Get daily candles for a given instrument within a date range.
     *
     * @param symbol The trading symbol (e.g., "RELIANCE.NS" for NSE, "AAPL" for US)
     * @param from   Start date (inclusive)
     * @param to     End date (inclusive)
     * @return List of candles ordered by trade date ascending, empty list if no data found
     */
    List<Candle> getDailyCandles(String symbol, LocalDate from, LocalDate to);

    /**
     * Check if this provider is available for use.
     * Some providers may be temporarily unavailable due to rate limits, network issues, etc.
     *
     * @return true if the provider is available, false otherwise
     */
    default boolean isAvailable() {
        return true;
    }
}
