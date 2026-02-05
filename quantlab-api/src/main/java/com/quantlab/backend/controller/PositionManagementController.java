package com.quantlab.backend.controller;

import com.quantlab.backend.scheduler.PositionManagementScheduler;
import com.quantlab.backend.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller for manual position management operations.
 *
 * This controller provides endpoints to manually trigger position management
 * tasks that would otherwise run on a schedule. Useful for:
 * - Testing position management logic
 * - On-demand updates outside scheduled times
 * - Admin operations during trading hours
 * - Debugging and troubleshooting
 */
@RestController
@RequestMapping("/api/v1/positions")
public class PositionManagementController {

    private static final Logger log = LoggerFactory.getLogger(PositionManagementController.class);

    private final PositionManagementScheduler scheduler;
    private final TradeService tradeService;

    public PositionManagementController(PositionManagementScheduler scheduler, TradeService tradeService) {
        this.scheduler = scheduler;
        this.tradeService = tradeService;
    }

    /**
     * Manually trigger a complete position management update.
     *
     * This endpoint performs the same operations as the scheduled market close task:
     * - Updates unrealized P&L for all open positions
     * - Checks stop loss and target levels
     * - Closes positions that hit SL or target
     * - Checks pending signals for entry triggers
     *
     * @return summary of the operations performed
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, String>> updatePositions() {
        log.info("Manual position update requested via API");

        try {
            String result = scheduler.triggerManualPositionUpdate();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", result,
                    "timestamp", java.time.Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("Error during manual position update", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", java.time.Instant.now().toString()
            ));
        }
    }

    /**
     * Update unrealized P&L for all open positions only.
     *
     * Use this endpoint when you want to refresh current prices without
     * triggering exit checks or signal execution.
     *
     * @return summary of P&L update operation
     */
    @PostMapping("/update-pnl")
    public ResponseEntity<Map<String, String>> updateUnrealizedPnL() {
        log.info("Manual P&L update requested via API");

        try {
            tradeService.updateUnrealizedPnL();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Unrealized P&L updated for all open positions",
                    "timestamp", java.time.Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("Error during P&L update", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", java.time.Instant.now().toString()
            ));
        }
    }

    /**
     * Check stop loss and target levels for all open positions.
     *
     * This endpoint will close any positions that have hit their stop loss
     * or target price levels based on current market data.
     *
     * @return summary of exit checks performed
     */
    @PostMapping("/check-exits")
    public ResponseEntity<Map<String, String>> checkStopLossAndTargets() {
        log.info("Manual exit check requested via API");

        try {
            tradeService.checkStopLossAndTargets();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Stop loss and target levels checked for all open positions",
                    "timestamp", java.time.Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("Error during exit check", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", java.time.Instant.now().toString()
            ));
        }
    }

    /**
     * Check pending signals for entry trigger execution.
     *
     * This endpoint will execute any pending signals where the current price
     * has reached the entry trigger level.
     *
     * @return summary of entry trigger checks performed
     */
    @PostMapping("/check-entries")
    public ResponseEntity<Map<String, String>> checkEntryTriggers() {
        log.info("Manual entry trigger check requested via API");

        try {
            tradeService.checkEntryTriggers();

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Entry triggers checked for all pending signals",
                    "timestamp", java.time.Instant.now().toString()
            ));

        } catch (Exception e) {
            log.error("Error during entry trigger check", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", e.getMessage(),
                    "timestamp", java.time.Instant.now().toString()
            ));
        }
    }

    /**
     * Health check endpoint for the position management system.
     *
     * @return status of the position management scheduler
     */
    @PostMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "scheduler", "active",
                "timestamp", java.time.Instant.now().toString()
        ));
    }
}
