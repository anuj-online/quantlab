package com.quantlab.backend.controller;

import com.quantlab.backend.dto.*;
import com.quantlab.backend.service.StrategyComparisonService;
import com.quantlab.backend.service.StrategyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Strategy management.
 * Handles API endpoints for listing strategies, running strategy backtests,
 * comparing strategies, and retrieving strategy metrics.
 */
@RestController
@RequestMapping("/api/v1")
public class StrategyController {

    private final StrategyService strategyService;
    private final StrategyComparisonService strategyComparisonService;

    /**
     * Constructor-based dependency injection.
     *
     * @param strategyService the strategy service
     * @param strategyComparisonService the strategy comparison service
     */
    public StrategyController(StrategyService strategyService,
                             StrategyComparisonService strategyComparisonService) {
        this.strategyService = strategyService;
        this.strategyComparisonService = strategyComparisonService;
    }

    /**
     * Get list of all available strategies with metadata.
     * <p>
     * Returns strategies including:
     * - code, name, description
     * - supportsScreening (boolean)
     * - minLookbackDays (int)
     *
     * @return list of strategies with full metadata
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

    /**
     * Compare multiple strategies side-by-side.
     * <p>
     * This endpoint accepts a list of strategy codes and returns performance metrics
     * for each strategy, along with identification of the best performing strategy
     * for each metric (win rate, total PnL, Sharpe ratio, etc.).
     * <p>
     * If no strategy run exists for the given parameters, returns empty metrics.
     * Strategies should be run first via POST /api/v1/strategies/run.
     *
     * @param request the comparison request containing strategy codes, market, and date range
     * @return comparison response with metrics for each strategy
     */
    @PostMapping("/strategies/compare")
    public ResponseEntity<StrategyComparisonResponse> compareStrategies(
            @Valid @RequestBody StrategyComparisonRequest request) {
        StrategyComparisonResponse response = strategyComparisonService.compareStrategies(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get performance metrics for a single strategy.
     * <p>
     * Returns comprehensive metrics including:
     * - totalTrades, winRate, avgReturn
     * - totalPnl, maxDrawdown
     * - sharpeRatio, sortinoRatio
     * - avgWin, avgLoss, profitFactor
     * <p>
     * If no strategy run exists for the given parameters, returns empty metrics.
     * Strategy should be run first via POST /api/v1/strategies/run.
     *
     * @param code the strategy code
     * @param market the market type (INDIA or US)
     * @param startDate the start date (yyyy-MM-dd format)
     * @param endDate the end date (yyyy-MM-dd format)
     * @return strategy metrics
     */
    @GetMapping("/strategies/{code}/metrics")
    public ResponseEntity<StrategyMetrics> getStrategyMetrics(
            @PathVariable String code,
            @RequestParam String market,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        StrategyMetrics metrics = strategyComparisonService.getStrategyMetrics(
                code, market, startDate, endDate);
        return ResponseEntity.ok(metrics);
    }
}
