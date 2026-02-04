package com.quantlab.backend.controller;

import com.quantlab.backend.dto.RunStrategyRequest;
import com.quantlab.backend.dto.RunStrategyResponse;
import com.quantlab.backend.dto.StrategyResponse;
import com.quantlab.backend.service.StrategyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Strategy management.
 * Handles API endpoints for listing strategies and running strategy backtests.
 */
@RestController
@RequestMapping("/api/v1")
public class StrategyController {

    private final StrategyService strategyService;

    /**
     * Constructor-based dependency injection.
     *
     * @param strategyService the strategy service
     */
    public StrategyController(StrategyService strategyService) {
        this.strategyService = strategyService;
    }

    /**
     * Get list of all available strategies.
     *
     * @return list of strategies
     */
    @GetMapping("/strategies")
    public ResponseEntity<List<StrategyResponse>> getStrategies() {
        List<StrategyResponse> strategies = strategyService.getAllStrategies();
        return ResponseEntity.ok(strategies);
    }

    /**
     * Run a strategy backtest with the specified parameters.
     *
     * @param request the strategy run request containing strategy code, market, dates, and parameters
     * @return the strategy run response with run ID and status
     */
    @PostMapping("/strategies/run")
    public ResponseEntity<RunStrategyResponse> runStrategy(@Valid @RequestBody RunStrategyRequest request) {
        RunStrategyResponse response = strategyService.runStrategy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
