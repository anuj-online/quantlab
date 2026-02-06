package com.quantlab.backend.marketdata;

import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Instrument;
import com.quantlab.backend.repository.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Yahoo Finance market data provider.
 * Fetches live/recent candles from Yahoo Finance API.
 * <p>
 * Usage:
 * - Primary for screening (current/recent data)
 * - Fallback for backtesting when database has gaps
 * - Data is NOT persisted to database
 * - Returns empty list on failure (graceful degradation)
 */
@Component
public class YahooMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(YahooMarketDataProvider.class);

    private final YahooFinanceApiService yahooApiService;
    private final InstrumentRepository instrumentRepository;

    public YahooMarketDataProvider(YahooFinanceApiService yahooApiService,
                                   InstrumentRepository instrumentRepository) {
        this.yahooApiService = yahooApiService;
        this.instrumentRepository = instrumentRepository;
    }

    @Override
    public List<Candle> getDailyCandles(String symbol, LocalDate from, LocalDate to) {
        Instrument instrument = instrumentRepository.findBySymbol(symbol);

        if (instrument == null) {
            log.warn("Instrument not found in database for symbol: {}", symbol);
            return Collections.emptyList();
        }

        // Normalize symbol for Yahoo Finance
        String yahooSymbol = toYahooSymbol(symbol, instrument.getMarket());

        try {
            List<YahooFinanceApiService.YahooCandleData> yahooCandles =
//                    yahooApiService.fetchDailyCandles(yahooSymbol, from, to);
            new ArrayList<>();

            if (yahooCandles.isEmpty()) {
                log.debug("No candles returned from Yahoo Finance for {}", symbol);
                return Collections.emptyList();
            }

            List<Candle> candles = yahooCandles.stream()
                    .map(yahooData -> toCandle(yahooData, instrument))
                    .collect(Collectors.toList());

            MDC.put("dataSource", "YAHOO_FINANCE");
            log.info("Retrieved {} candles from YAHOO FINANCE for {} ({} to {})",
                    candles.size(), symbol, from, to);
            MDC.remove("dataSource");

            return candles;

        } catch (Exception e) {
            MDC.put("dataSource", "YAHOO_FINANCE");
            MDC.put("error", e.getClass().getSimpleName());
            log.warn("Failed to fetch candles from Yahoo Finance for {}: {}", symbol, e.getMessage());
            MDC.remove("dataSource");
            MDC.remove("error");
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isAvailable() {
        return yahooApiService.isAvailable();
    }

    /**
     * Convert internal symbol to Yahoo Finance symbol format.
     * <p>
     * Rules:
     * - INDIA stocks: Add ".NS" suffix (e.g., "RELIANCE" -> "RELIANCE.NS")
     * - US stocks: Use as-is (e.g., "AAPL" -> "AAPL")
     * <p>
     * Note: Database stores symbols without ".NS" suffix for consistency.
     */
    private String toYahooSymbol(String symbol, com.quantlab.backend.entity.MarketType market) {
        if (market == com.quantlab.backend.entity.MarketType.INDIA) {
            // Check if symbol already has the suffix
            if (!symbol.endsWith(".NS")) {
                return symbol + ".NS";
            }
        }
        return symbol;
    }

    /**
     * Convert Yahoo Finance candle data to Candle entity.
     * Note: These are transient entities (not persisted to database).
     */
    private Candle toCandle(YahooFinanceApiService.YahooCandleData yahooData, Instrument instrument) {
        Candle candle = new Candle();
        candle.setInstrument(instrument);
        candle.setTradeDate(yahooData.tradeDate());
        candle.setOpen(yahooData.open());
        candle.setHigh(yahooData.high());
        candle.setLow(yahooData.low());
        candle.setClose(yahooData.close());
        candle.setVolume(yahooData.volume());
        return candle;
    }
}
