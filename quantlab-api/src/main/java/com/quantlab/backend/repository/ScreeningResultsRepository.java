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
 * Provides database operations for screening results.
 */
@Repository
public interface ScreeningResultsRepository extends JpaRepository<ScreeningResult, Long> {

    /**
     * Find all screening results for a specific date, ordered by symbol and strategy code.
     *
     * @param runDate the screening run date
     * @return list of screening results
     */
    List<ScreeningResult> findByRunDateOrderBySymbolAscStrategyCodeAsc(LocalDate runDate);

    /**
     * Find all screening results for a specific date and strategy.
     *
     * @param runDate the screening run date
     * @param strategyCode the strategy code
     * @return list of screening results
     */
    List<ScreeningResult> findByRunDateAndStrategyCode(LocalDate runDate, String strategyCode);

    /**
     * Find all screening results for a specific date, strategy, and signal type.
     *
     * @param runDate the screening run date
     * @param strategyCode the strategy code
     * @param signalType the signal type (BUY, SELL, HOLD)
     * @return list of screening results
     */
    List<ScreeningResult> findByRunDateAndStrategyCodeAndSignalType(
            LocalDate runDate, String strategyCode, SignalType signalType);

    /**
     * Find all screening results for a specific date and signal type.
     * Used to get all actionable (BUY/SELL) signals for a date.
     *
     * @param runDate the screening run date
     * @param signalType the signal type
     * @return list of screening results
     */
    List<ScreeningResult> findByRunDateAndSignalTypeOrderBySymbolAsc(LocalDate runDate, SignalType signalType);

    /**
     * Find all screening results for a specific symbol within a date range.
     * Used for historical analysis of a particular stock.
     *
     * @param symbol the trading symbol
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of screening results
     */
    List<ScreeningResult> findBySymbolAndRunDateBetweenOrderByRunDateDesc(
            String symbol, LocalDate startDate, LocalDate endDate);

    /**
     * Find the most recent screening date available.
     * Useful for determining the latest screening run.
     *
     * @return the most recent run date, or null if no results exist
     */
    @Query("SELECT MAX(sr.runDate) FROM ScreeningResult sr")
    LocalDate findMaxRunDate();

    /**
     * Count screening results by strategy code for a specific date.
     * Used for summary statistics.
     *
     * @param runDate the screening run date
     * @param strategyCode the strategy code
     * @return count of screening results
     */
    long countByRunDateAndStrategyCode(LocalDate runDate, String strategyCode);

    /**
     * Count screening results by date and signal type.
     * Used to get total actionable signals (BUY/SELL) for a date.
     *
     * @param runDate the screening run date
     * @param signalType the signal type
     * @return count of screening results
     */
    long countByRunDateAndSignalType(LocalDate runDate, SignalType signalType);

    /**
     * Delete all screening results for a specific date.
     * Useful for re-running screening for the same date.
     *
     * @param runDate the screening run date
     */
    void deleteByRunDate(LocalDate runDate);

    /**
     * Find all distinct run dates in descending order.
     * Used for populating date filters in the UI.
     *
     * @return list of unique run dates
     */
    @Query("SELECT DISTINCT sr.runDate FROM ScreeningResult sr ORDER BY sr.runDate DESC")
    List<LocalDate> findDistinctRunDatesOrderByRunDateDesc();
}
