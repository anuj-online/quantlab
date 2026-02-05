package com.quantlab.backend.marketdata;

import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Instrument;
import com.quantlab.backend.repository.CandleRepository;
import com.quantlab.backend.repository.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Database-backed market data provider.
 * Fetches candles from the canonical database (Bhavcopy/Stooq data).
 */
@Component
public class DbMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(DbMarketDataProvider.class);

    private final CandleRepository candleRepository;
    private final InstrumentRepository instrumentRepository;

    public DbMarketDataProvider(CandleRepository candleRepository,
                                InstrumentRepository instrumentRepository) {
        this.candleRepository = candleRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @Override
    public List<Candle> getDailyCandles(String symbol, LocalDate from, LocalDate to) {
        Instrument instrument = instrumentRepository.findBySymbol(symbol);

        if (instrument == null) {
            log.warn("Instrument not found in database for symbol: {}", symbol);
            return Collections.emptyList();
        }

        List<Candle> candles = candleRepository.findByInstrumentIdAndTradeDateBetweenOrderByTradeDateAsc(
                instrument.getId(), from, to);

        MDC.put("dataSource", "DATABASE");
        log.info("Fetched {} candles from DATABASE for {} between {} and {}",
                candles.size(), symbol, from, to);
        MDC.remove("dataSource");

        return candles;
    }

    @Override
    public boolean isAvailable() {
        try {
            // Simple check: can we connect to the database
            instrumentRepository.count();
            return true;
        } catch (Exception e) {
            log.warn("Database provider unavailable: {}", e.getMessage());
            return false;
        }
    }
}
