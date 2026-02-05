package com.quantlab.backend.repository;

import com.quantlab.backend.entity.MarketType;
import com.quantlab.backend.entity.StrategyRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for StrategyRun entity.
 */
@Repository
public interface StrategyRunRepository extends JpaRepository<StrategyRun, Long> {

    /**
     * Find all strategy runs for a specific strategy and market, ordered by run timestamp descending.
     *
     * @param strategyId the strategy ID
     * @param market     the market type
     * @return list of strategy runs
     */
    List<StrategyRun> findByStrategyIdAndMarketOrderByRunTimestampDesc(Long strategyId, MarketType market);

    /**
     * Find all strategy runs for a specific strategy ordered by run timestamp descending.
     *
     * @param strategyId the strategy ID
     * @return list of strategy runs
     */
    List<StrategyRun> findByStrategyIdOrderByRunTimestampDesc(Long strategyId);

    /**
     * Find all strategy runs ordered by run timestamp descending.
     *
     * @return list of all strategy runs
     */
    List<StrategyRun> findAllByOrderByRunTimestampDesc();

    /**
     * Find strategy runs by strategy ID within a date range.
     *
     * @param strategyId the strategy ID
     * @param startDate  the start date
     * @param endDate    the end date
     * @return list of strategy runs
     */
    List<StrategyRun> findByStrategyIdAndStartDateBetweenOrderByRunTimestampDesc(
            Long strategyId, LocalDate startDate, LocalDate endDate);

    /**
     * Find the most recent strategy run for a strategy and market.
     *
     * @param strategyId the strategy ID
     * @param market     the market type
     * @return the most recent strategy run or null
     */
    @Query("SELECT sr FROM StrategyRun sr WHERE sr.strategy.id = :strategyId AND sr.market = :market ORDER BY sr.runTimestamp DESC LIMIT 1")
    StrategyRun findLatestRun(@Param("strategyId") Long strategyId, @Param("market") MarketType market);

    /**
     * Find strategy runs by strategy code, market, and date range.
     * Used for finding existing runs for strategy comparison.
     *
     * @param strategyCode the strategy code
     * @param market the market type
     * @param startDate the start date
     * @param endDate the end date
     * @return the strategy run if found
     */
    @Query("SELECT sr FROM StrategyRun sr " +
           "WHERE sr.strategy.code = :strategyCode " +
           "AND sr.market = :market " +
           "AND sr.startDate = :startDate " +
           "AND sr.endDate = :endDate")
    Optional<StrategyRun> findByStrategy_CodeAndMarketAndStartDateAndEndDate(
            @Param("strategyCode") String strategyCode,
            @Param("market") MarketType market,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Delete all strategy runs for a specific strategy.
     *
     * @param strategyId the strategy ID
     */
    void deleteByStrategyId(Long strategyId);
}
