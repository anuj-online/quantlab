package com.quantlab.backend.controller;

import com.quantlab.backend.dto.*;
import com.quantlab.backend.entity.CapitalAllocationSnapshot;
import com.quantlab.backend.service.CapitalAllocationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for capital allocation operations.
 * <p>
 * Capital allocation simulation helps traders understand how to deploy
 * capital across multiple ranked trading signals using risk-based position sizing.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Risk-based position sizing</li>
 *   <li>Maximum open trades constraint</li>
 *   <li>Expected R-multiple calculation</li>
 *   <li>Historical allocation snapshots</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/capital-allocation")
public class CapitalAllocationController {

    private static final Logger log = LoggerFactory.getLogger(CapitalAllocationController.class);

    private final CapitalAllocationService allocationService;

    /**
     * Constructor-based dependency injection.
     *
     * @param allocationService the capital allocation service
     */
    public CapitalAllocationController(CapitalAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    /**
     * Simulate capital allocation for the top N ranked signals.
     * <p>
     * POST /api/v1/capital-allocation/simulate
     * </p>
     * <p>
     * Request body:
     * <pre>
     * {
     *   "date": "2024-01-15",
     *   "totalCapital": 1000000,
     *   "riskPerTradePct": 1.0,
     *   "maxOpenTrades": 5
     * }
     * </pre>
     * </p>
     * <p>
     * Response:
     * <pre>
     * {
     *   "id": 1,
     *   "runDate": "2024-01-15",
     *   "totalCapital": 1000000,
     *   "deployedCapital": 750000,
     *   "freeCash": 250000,
     *   "expectedRMultiple": 3.5,
     *   "positions": [
     *     {
     *       "symbol": "INFY",
     *       "quantity": 100,
     *       "capitalUsed": 150000,
     *       "riskAmount": 5000,
     *       "expectedR": 2.5,
     *       "allocationPct": 15.0
     *     }
     *   ]
     * }
     * </pre>
     * </p>
     *
     * @param request the allocation request
     * @return allocation snapshot with position details
     */
    @PostMapping("/simulate")
    public ResponseEntity<com.quantlab.backend.dto.CapitalAllocationSnapshot> simulateAllocation(@Valid @RequestBody AllocationRequest request) {
        log.info("Received capital allocation request: {}", request);

        try {
            com.quantlab.backend.dto.CapitalAllocationSnapshot snapshot = allocationService.simulateAllocation(request);
            return ResponseEntity.ok(snapshot);
        } catch (IllegalArgumentException e) {
            log.error("Invalid allocation request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error simulating allocation: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Get capital allocation history for a date range.
     * <p>
     * GET /api/v1/capital-allocation/history?startDate=2024-01-01&endDate=2024-01-31
     * </p>
     *
     * @param startDate the start date (format: yyyy-MM-dd)
     * @param endDate the end date (format: yyyy-MM-dd)
     * @return list of allocation snapshots ordered by date descending
     */
    @GetMapping("/history")
    public ResponseEntity<List<com.quantlab.backend.dto.CapitalAllocationSnapshot>> getHistory(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("Fetching capital allocation history from {} to {}", startDate, endDate);

        try {
            List<com.quantlab.backend.dto.CapitalAllocationSnapshot> history = allocationService.getHistory(startDate, endDate);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching allocation history: {}", e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}