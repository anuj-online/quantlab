package com.quantlab.backend.repository;

import com.quantlab.backend.entity.PaperTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for PaperTrade entity.
 */
@Repository
public interface PaperTradeRepository extends JpaRepository<PaperTrade, Long> {

    /**
     * Find all paper trades for a specific strategy run, ordered by entry date ascending.
     *
     * @param strategyRunId the strategy run ID
     * @return list of paper trades
     */
    List<PaperTrade> findByStrategyRunIdOrderByEntryDateAsc(Long strategyRunId);

    /**
     * Find all paper trades for a specific strategy run and instrument.
     *
     * @param strategyRunId the strategy run ID
     * @param instrumentId  the instrument ID
     * @return list of paper trades
     */
    List<PaperTrade> findByStrategyRunIdAndInstrumentId(Long strategyRunId, Long instrumentId);

    /**
     * Find all paper trades for a specific strategy run within an exit date range.
     *
     * @param strategyRunId the strategy run ID
     * @param startDate     the start date (inclusive)
     * @param endDate       the end date (inclusive)
     * @return list of paper trades
     */
    List<PaperTrade> findByStrategyRunIdAndExitDateBetweenOrderByExitDateAsc(
            Long strategyRunId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all paper trades for an instrument within an entry date range.
     *
     * @param instrumentId the instrument ID
     * @param startDate    the start date (inclusive)
     * @param endDate      the end date (inclusive)
     * @return list of paper trades
     */
    List<PaperTrade> findByInstrumentIdAndEntryDateBetweenOrderByEntryDateAsc(
            Long instrumentId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculate the total PnL for a strategy run.
     *
     * @param strategyRunId the strategy run ID
     * @return total PnL (sum of all trades)
     */
    @Query("SELECT COALESCE(SUM(pt.pnl), 0) FROM PaperTrade pt WHERE pt.strategyRun.id = :strategyRunId")
    BigDecimal calculateTotalPnlByStrategyRun(@Param("strategyRunId") Long strategyRunId);

    /**
     * Calculate the total PnL percentage for a strategy run.
     *
     * @param strategyRunId the strategy run ID
     * @return total PnL percentage
     */
    @Query("SELECT COALESCE(SUM(pt.pnlPct), 0) FROM PaperTrade pt WHERE pt.strategyRun.id = :strategyRunId")
    BigDecimal calculateTotalPnlPctByStrategyRun(@Param("strategyRunId") Long strategyRunId);

    /**
     * Count the number of paper trades for a strategy run.
     *
     * @param strategyRunId the strategy run ID
     * @return count of paper trades
     */
    @Query("SELECT COUNT(pt) FROM PaperTrade pt WHERE pt.strategyRun.id = :strategyRunId")
    long countByStrategyRunId(@Param("strategyRunId") Long strategyRunId);

    /**
     * Count winning trades for a strategy run.
     *
     * @param strategyRunId the strategy run ID
     * @return count of winning trades
     */
    @Query("SELECT COUNT(pt) FROM PaperTrade pt WHERE pt.strategyRun.id = :strategyRunId AND pt.pnl > 0")
    long countWinningTradesByStrategyRun(@Param("strategyRunId") Long strategyRunId);

    /**
     * Count losing trades for a strategy run.
     *
     * @param strategyRunId the strategy run ID
     * @return count of losing trades
     */
    @Query("SELECT COUNT(pt) FROM PaperTrade pt WHERE pt.strategyRun.id = :strategyRunId AND pt.pnl < 0")
    long countLosingTradesByStrategyRun(@Param("strategyRunId") Long strategyRunId);

    /**
     * Delete all paper trades for a specific strategy run.
     *
     * @param strategyRunId the strategy run ID
     */
    void deleteByStrategyRunId(Long strategyRunId);
}
