package com.quantlab.backend.repository;

import com.quantlab.backend.entity.Strategy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Strategy entity.
 */
@Repository
public interface StrategyRepository extends JpaRepository<Strategy, Long> {

    /**
     * Find all active strategies ordered by code ascending.
     *
     * @return list of active strategies
     */
    List<Strategy> findByActiveTrueOrderByCodeAsc();

    /**
     * Find all strategies ordered by code ascending.
     *
     * @return list of all strategies
     */
    List<Strategy> findAllByOrderByCodeAsc();

    /**
     * Find a strategy by its unique code.
     *
     * @param code the strategy code
     * @return the strategy if found
     */
    Optional<Strategy> findByCode(String code);

    /**
     * Check if a strategy exists by its code.
     *
     * @param code the strategy code
     * @return true if strategy exists
     */
    boolean existsByCode(String code);

    /**
     * Find all active strategies.
     *
     * @return list of active strategies
     */
    List<Strategy> findByActiveTrue();
}
