package com.quantlab.backend.repository;

import com.quantlab.backend.entity.Instrument;
import com.quantlab.backend.entity.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Instrument entity.
 */
@Repository
public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    /**
     * Find all active instruments for a specific market, ordered by symbol ascending.
     *
     * @param market the market type (INDIA or US)
     * @param active whether the instrument is active
     * @return list of instruments matching the criteria
     */
    List<Instrument> findByMarketAndActiveOrderBySymbolAsc(MarketType market, boolean active);

    /**
     * Find an instrument by its symbol.
     *
     * @param symbol the trading symbol
     * @return the instrument if found
     */
    Instrument findBySymbol(String symbol);

    /**
     * Find all instruments for a specific market.
     *
     * @param market the market type (INDIA or US)
     * @return list of instruments in the market
     */
    List<Instrument> findByMarket(MarketType market);

    /**
     * Find all instruments by active status, ordered by symbol ascending.
     *
     * @param active whether the instrument is active
     * @return list of instruments matching the active status
     */
    List<Instrument> findByActiveOrderBySymbolAsc(boolean active);

    /**
     * Find an instrument by symbol and market.
     * Used for idempotent instrument lookup during Bhavcopy loading.
     *
     * @param symbol the trading symbol
     * @param market the market type
     * @return the instrument if found
     */
    Instrument findBySymbolAndMarket(String symbol, MarketType market);

    /**
     * Find all instruments by symbols and market.
     * Used for batch instrument lookup during parallel Bhavcopy loading.
     *
     * @param symbols list of trading symbols
     * @param market  the market type
     * @return list of instruments that exist for the given symbols and market
     */
    List<Instrument> findBySymbolInAndMarket(List<String> symbols, MarketType market);

    /**
     * Insert an instrument if it doesn't already exist (idempotent upsert).
     * Uses ON CONFLICT DO NOTHING to handle race conditions in parallel bhavcopy loading.
     *
     * @param symbol the trading symbol
     * @param name   the instrument name
     * @param market the market type
     * @return the instrument (either existing or newly created), or null if conflict occurred
     */
    @Query(value = """
        INSERT INTO instrument (symbol, name, market, active, created_at)
        VALUES (:symbol, :name, :market, true, CURRENT_DATE)
        ON CONFLICT (symbol, market) DO NOTHING
        RETURNING id, symbol, name, market, active, created_at
        """, nativeQuery = true)
    Optional<Instrument> insertIfNotExists(@Param("symbol") String symbol,
                                           @Param("name") String name,
                                           @Param("market") String market);
}
