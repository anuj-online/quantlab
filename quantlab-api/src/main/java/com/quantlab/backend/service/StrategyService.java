package com.quantlab.backend.service;

import com.quantlab.backend.dto.RunStrategyRequest;
import com.quantlab.backend.dto.RunStrategyResponse;
import com.quantlab.backend.dto.StrategyResponse;
import com.quantlab.backend.entity.Strategy;
import com.quantlab.backend.mapper.StrategyMapper;
import com.quantlab.backend.repository.StrategyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for strategy-related operations.
 */
@Service
public class StrategyService {

    private final StrategyRepository strategyRepository;
    private final StrategyRunService strategyRunService;

    public StrategyService(StrategyRepository strategyRepository, StrategyRunService strategyRunService) {
        this.strategyRepository = strategyRepository;
        this.strategyRunService = strategyRunService;
    }

    /**
     * Get all active strategies.
     *
     * @return list of active strategy response DTOs
     */
    public List<StrategyResponse> getAllActiveStrategies() {
        List<Strategy> strategies = strategyRepository.findByActiveTrueOrderByCodeAsc();
        return strategies.stream()
                .map(StrategyMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all strategies (including inactive).
     *
     * @return list of all strategy response DTOs
     */
    public List<StrategyResponse> getAllStrategies() {
        List<Strategy> strategies = strategyRepository.findAllByOrderByCodeAsc();
        return strategies.stream()
                .map(StrategyMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find a strategy by its unique code.
     *
     * @param code the strategy code
     * @return the strategy if found
     * @throws jakarta.persistence.EntityNotFoundException if strategy not found
     */
    public Strategy getStrategyByCode(String code) {
        return strategyRepository.findByCode(code)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Strategy not found with code: " + code));
    }

    /**
     * Find a strategy by its unique code and return as DTO.
     *
     * @param code the strategy code
     * @return the strategy response DTO if found, null otherwise
     */
    public StrategyResponse getStrategyResponseByCode(String code) {
        Optional<Strategy> strategy = strategyRepository.findByCode(code);
        return strategy.map(StrategyMapper::toDto).orElse(null);
    }

    /**
     * Run a strategy with the given parameters.
     * Delegates to StrategyRunService for the actual execution.
     *
     * @param request the run strategy request
     * @return the run strategy response with the strategy run ID and status
     */
    @Transactional
    public RunStrategyResponse runStrategy(RunStrategyRequest request) {
        return strategyRunService.runStrategy(request);
    }
}
