package com.quantlab.backend.repository;

import com.quantlab.backend.entity.Instrument;
import com.quantlab.backend.entity.MarketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
