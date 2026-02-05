package com.quantlab.backend.repository;

import com.quantlab.backend.entity.ScreeningResult;
import com.quantlab.backend.entity.SignalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for ScreeningResult entity.
 * Provides data access methods for daily screening results.
 */
@Repository
public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, Long> {

    /**
     * Find all screening results for a specific run date, ordered by symbol and strategy code.
     * Primary query for displaying today's screening results.
     *
     * @param runDate the screening run date
     * @return list of screening results
     */
    List<ScreeningResult> findByRunDateOrderBySymbolAscStrategyCodeAsc(LocalDate runDate);

    /**
     * Find screening results for a specific run date and signal type.
     * Useful for filtering only BUY or SELL signals.
     *
     * @param runDate    the screening run date
     * @param signalType the signal type (BUY, SELL, HOLD)
     * @return list of screening results
     */
    List<ScreeningResult> findByRunDateAndSignalTypeOrderBySymbolAsc(LocalDate runDate, SignalType signalType);

    /**
     * Find all screening results for a specific symbol, ordered by run date descending (most recent first).
     * Useful for viewing historical signals for a particular stock.
     *
     * @param symbol the trading symbol
     * @return list of screening results
     */
    List<ScreeningResult> findBySymbolOrderByRunDateDesc(String symbol);

    /**
     * Find all screening results for a specific strategy code, ordered by run date descending.
     * Useful for analyzing a strategy's historical performance.
     *
     * @param strategyCode the strategy code
     * @return list of screening results
     */
    List<ScreeningResult> findByStrategyCodeOrderByRunDateDesc(String strategyCode);

    /**
     * Find screening results within a date range for a specific strategy, ordered by run date and symbol.
     *
     * @param strategyCode the strategy code
     * @param startDate    the start date (inclusive)
     * @param endDate      the end date (inclusive)
     * @return list of screening results
     */
    List<ScreeningResult> findByStrategyCodeAndRunDateBetweenOrderByRunDateDescSymbolAsc(
            String strategyCode, LocalDate startDate, LocalDate endDate);

    /**
     * Find the most recent screening result for a specific symbol and strategy.
     * Returns the latest signal generated for the symbol-strategy combination.
     *
     * @param symbol       the trading symbol
     * @param strategyCode the strategy code
     * @return the most recent screening result, or null if none found
     */
    @Query("SELECT sr FROM ScreeningResult sr WHERE sr.symbol = :symbol AND sr.strategyCode = :strategyCode ORDER BY sr.runDate DESC LIMIT 1")
    ScreeningResult findMostRecentBySymbolAndStrategyCode(
            @Param("symbol") String symbol,
            @Param("strategyCode") String strategyCode);

    /**
     * Find all BUY signals for a specific run date.
     * Common query for actionable trading opportunities.
     *
     * @param runDate the screening run date
     * @return list of screening results with BUY signals
     */
    @Query("SELECT sr FROM ScreeningResult sr WHERE sr.runDate = :runDate AND sr.signalType = 'BUY' ORDER BY sr.symbol ASC")
    List<ScreeningResult> findBuySignalsByRunDate(@Param("runDate") LocalDate runDate);

    /**
     * Find all screening results for a specific run date and list of symbols.
     * Useful for monitoring a watchlist.
     *
     * @param runDate the screening run date
     * @param symbols list of trading symbols
     * @return list of screening results
     */
    @Query("SELECT sr FROM ScreeningResult sr WHERE sr.runDate = :runDate AND sr.symbol IN :symbols ORDER BY sr.symbol ASC")
    List<ScreeningResult> findByRunDateAndSymbolIn(
            @Param("runDate") LocalDate runDate,
            @Param("symbols") List<String> symbols);

    /**
     * Count the number of signals generated for a specific run date and strategy.
     *
     * @param runDate      the screening run date
     * @param strategyCode the strategy code
     * @return count of screening results
     */
    long countByRunDateAndStrategyCode(LocalDate runDate, String strategyCode);

    /**
     * Delete all screening results older than a specified date.
     * Useful for data retention policies.
     *
     * @param date the cutoff date
     */
    void deleteByRunDateBefore(LocalDate date);

    /**
     * Check if a screening result exists for a specific date, symbol, and strategy.
     * Useful for avoiding duplicate entries.
     *
     * @param runDate      the screening run date
     * @param symbol       the trading symbol
     * @param strategyCode the strategy code
     * @return true if a result exists, false otherwise
     */
    boolean existsByRunDateAndSymbolAndStrategyCode(LocalDate runDate, String symbol, String strategyCode);
}
