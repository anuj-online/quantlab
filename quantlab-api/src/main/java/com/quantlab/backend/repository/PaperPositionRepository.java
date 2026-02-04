package com.quantlab.backend.repository;

import com.quantlab.backend.entity.PaperPosition;
import com.quantlab.backend.entity.PositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for PaperPosition entity.
 */
@Repository
public interface PaperPositionRepository extends JpaRepository<PaperPosition, Long> {

    /**
     * Find all paper positions for a specific strategy run.
     *
     * @param strategyRunId the strategy run ID
     * @return list of paper positions
     */
    List<PaperPosition> findByStrategyRunId(Long strategyRunId);

    /**
     * Find all paper positions for a specific strategy run and instrument.
     *
     * @param strategyRunId the strategy run ID
     * @param instrumentId  the instrument ID
     * @return list of paper positions
     */
    List<PaperPosition> findByStrategyRunIdAndInstrumentId(Long strategyRunId, Long instrumentId);

    /**
     * Find all open paper positions for a specific strategy run.
     *
     * @param strategyRunId the strategy run ID
     * @param status        the position status (typically OPEN)
     * @return list of open paper positions
     */
    List<PaperPosition> findByStrategyRunIdAndStatus(Long strategyRunId, PositionStatus status);

    /**
     * Find all open paper positions for a specific strategy run and instrument.
     *
     * @param strategyRunId the strategy run ID
     * @param instrumentId  the instrument ID
     * @param status        the position status (typically OPEN)
     * @return list of open paper positions
     */
    List<PaperPosition> findByStrategyRunIdAndInstrumentIdAndStatus(
            Long strategyRunId, Long instrumentId, PositionStatus status);

    /**
     * Find all paper positions with a specific status.
     *
     * @param status the position status
     * @return list of paper positions
     */
    List<PaperPosition> findByStatus(PositionStatus status);

    /**
     * Find all open positions across all strategy runs.
     *
     * @return list of all open paper positions
     */
    List<PaperPosition> findByStatusOrderByEntryDateAsc(PositionStatus status);

    /**
     * Count open paper positions for a strategy run.
     *
     * @param strategyRunId the strategy run ID
     * @return count of open positions
     */
    @Query("SELECT COUNT(pp) FROM PaperPosition pp WHERE pp.strategyRun.id = :strategyRunId AND pp.status = 'OPEN'")
    long countOpenPositionsByStrategyRun(@Param("strategyRunId") Long strategyRunId);

    /**
     * Check if an open position exists for a strategy run and instrument.
     *
     * @param strategyRunId the strategy run ID
     * @param instrumentId  the instrument ID
     * @return true if an open position exists
     */
    @Query("SELECT CASE WHEN COUNT(pp) > 0 THEN true ELSE false END FROM PaperPosition pp " +
           "WHERE pp.strategyRun.id = :strategyRunId AND pp.instrument.id = :instrumentId AND pp.status = 'OPEN'")
    boolean existsOpenPosition(@Param("strategyRunId") Long strategyRunId, @Param("instrumentId") Long instrumentId);

    /**
     * Delete all paper positions for a specific strategy run.
     *
     * @param strategyRunId the strategy run ID
     */
    void deleteByStrategyRunId(Long strategyRunId);
}
