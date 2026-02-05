package com.quantlab.backend.repository;

import com.quantlab.backend.entity.TradeSignal;
import com.quantlab.backend.entity.TradeSignalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for TradeSignal entity.
 */
@Repository
public interface TradeSignalRepository extends JpaRepository<TradeSignal, Long> {

    /**
     * Find all trade signals for a specific strategy run, ordered by signal date ascending.
     *
     * @param strategyRunId the strategy run ID
     * @return list of trade signals
     */
    List<TradeSignal> findByStrategyRunIdOrderBySignalDateAsc(Long strategyRunId);

    /**
     * Find all trade signals for a specific strategy run and instrument.
     *
     * @param strategyRunId the strategy run ID
     * @param instrumentId  the instrument ID
     * @return list of trade signals
     */
    List<TradeSignal> findByStrategyRunIdAndInstrumentId(Long strategyRunId, Long instrumentId);

    /**
     * Find all trade signals for a specific strategy run within a date range.
     *
     * @param strategyRunId the strategy run ID
     * @param startDate     the start date (inclusive)
     * @param endDate       the end date (inclusive)
     * @return list of trade signals
     */
    List<TradeSignal> findByStrategyRunIdAndSignalDateBetweenOrderBySignalDateAsc(
            Long strategyRunId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all trade signals for an instrument within a date range.
     *
     * @param instrumentId the instrument ID
     * @param startDate    the start date (inclusive)
     * @param endDate      the end date (inclusive)
     * @return list of trade signals
     */
    List<TradeSignal> findByInstrumentIdAndSignalDateBetweenOrderBySignalDateAsc(
            Long instrumentId, LocalDate startDate, LocalDate endDate);

    /**
     * Count the number of trade signals for a strategy run.
     *
     * @param strategyRunId the strategy run ID
     * @return count of trade signals
     */
    @Query("SELECT COUNT(ts) FROM TradeSignal ts WHERE ts.strategyRun.id = :strategyRunId")
    long countByStrategyRunId(@Param("strategyRunId") Long strategyRunId);

    /**
     * Delete all trade signals for a specific strategy run.
     *
     * @param strategyRunId the strategy run ID
     */
    void deleteByStrategyRunId(Long strategyRunId);

    /**
     * Find all trade signals for a specific strategy run and status.
     *
     * @param strategyRunId the strategy run ID
     * @param status the status
     * @return list of trade signals
     */
    List<TradeSignal> findByStrategyRunIdAndStatus(Long strategyRunId, TradeSignalStatus status);

    /**
     * Find all trade signals with a specific status.
     *
     * @param status the status
     * @return list of trade signals
     */
    List<TradeSignal> findByStatus(TradeSignalStatus status);
}
