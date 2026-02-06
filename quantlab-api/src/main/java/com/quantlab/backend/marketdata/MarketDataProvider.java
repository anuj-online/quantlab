package com.quantlab.backend.marketdata;

import com.quantlab.backend.entity.Candle;

import java.math.BigDecimal;
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
     * Get the current market price for a given instrument.
     * This returns the latest available close price (today's or most recent).
     *
     * @param symbol The trading symbol (e.g., "RELIANCE.NS" for NSE, "AAPL" for US)
     * @return Current market price (close price of latest candle)
     * @throws RuntimeException if no price data is available
     */
    default BigDecimal getCurrentPrice(String symbol) {
        LocalDate today = LocalDate.now();
        LocalDate lookback = today.minusDays(7); // Look back up to 7 days for latest price

        List<Candle> candles = getDailyCandles(symbol, lookback, today);

        if (candles.isEmpty()) {
            throw new RuntimeException("No price data available for symbol: " + symbol);
        }

        // Return the close price of the most recent candle
        return candles.get(candles.size() - 1).getClose();
    }

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
