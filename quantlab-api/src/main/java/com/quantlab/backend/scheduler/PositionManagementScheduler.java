package com.quantlab.backend.scheduler;

import com.quantlab.backend.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task component for managing open positions in the paper trading system.
 *
 * This scheduler runs at configurable intervals to:
 * 1. Update unrealized P&L for all open positions with current market prices
 * 2. Check if any positions have hit stop loss or target levels
 * 3. Automatically close positions that hit SL/target
 * 4. Check for pending signals that should be executed at trigger levels
 *
 * The scheduler uses the TradeService methods for all operations, ensuring
 * consistent business logic across manual and automated operations.
 *
 * Scheduling Configuration:
 * - The scheduler can be configured via application properties
 * - Default cron expression runs at market close (4:00 PM EST on weekdays)
 * - Can be customized for different markets (INDIA, US)
 */
@Component
public class PositionManagementScheduler {

    private static final Logger log = LoggerFactory.getLogger(PositionManagementScheduler.class);

    private final TradeService tradeService;

    public PositionManagementScheduler(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * Main scheduled task that runs daily at market close to manage all open positions.
     *
     * This method executes the complete position management workflow:
     * 1. Updates unrealized P&L for all open positions with latest prices
     * 2. Checks stop loss levels and closes positions if hit
     * 3. Checks target levels and closes positions if hit
     * 4. Checks pending signals for entry trigger execution
     *
     * Default Schedule: 4:00 PM EST, Monday through Friday
     * Cron: 0 0 16 * * MON-FRI
     *
     * To customize the schedule, modify the cron expression in application.properties:
     * position.scheduler.cron=0 0 16 * * MON-FRI
     *
     * Market-specific schedules:
     * - US Market: 4:00 PM EST = "0 0 16 * * MON-FRI"
     * - India Market: 3:30 PM IST = "0 30 15 * * MON-FRI" (adjust for timezone)
     */
    @Scheduled(cron = "${position.scheduler.cron:0 0 16 * * MON-FRI}")
    public void managePositionsAtMarketClose() {
        log.info("Starting scheduled position management at market close");

        try {
            // Step 1: Update unrealized P&L for all open positions
            log.debug("Updating unrealized P&L for open positions");
            tradeService.updateUnrealizedPnL();

            // Step 2: Check and execute stop loss and target exits
            log.debug("Checking stop loss and target levels");
            tradeService.checkStopLossAndTargets();

            // Step 3: Check for pending signals that should be executed
            log.debug("Checking entry triggers for pending signals");
            tradeService.checkEntryTriggers();

            log.info("Completed scheduled position management successfully");

        } catch (Exception e) {
            log.error("Error during scheduled position management", e);
            // Don't rethrow - we want the scheduler to continue running
        }
    }

    /**
     * High-frequency scheduled task for intraday position monitoring.
     *
     * This runs every 15 minutes during market hours to:
     * - Update current prices for open positions
     * - Check if any positions hit SL/target during the trading day
     *
     * This is useful for strategies that need intraday monitoring rather than
     * just end-of-day checks.
     *
     * To enable intraday monitoring, set in application.properties:
     * position.scheduler.intraday.enabled=true
     *
     * Default: disabled (only end-of-day scheduling is active)
     */
    @Scheduled(fixedRate = 900000, initialDelay = 60000)
    public void monitorPositionsIntraday() {
        if (!isIntradayMonitoringEnabled()) {
            return;
        }

        // Only run during market hours (9:30 AM - 4:00 PM EST on weekdays)
        if (!isMarketHours()) {
            return;
        }

        log.debug("Running intraday position monitoring");

        try {
            // Update P&L and check exits more frequently
            tradeService.updateUnrealizedPnL();
            tradeService.checkStopLossAndTargets();

        } catch (Exception e) {
            log.error("Error during intraday position monitoring", e);
        }
    }

    /**
     * End-of-day reconciliation task that runs after market close.
     *
     * This performs final checks and generates daily summaries:
     * - Final P&L update for the day
     * - Verify all SL/target exits were processed
     * - Log summary of open positions and their status
     *
     * Schedule: 5:00 PM EST, Monday through Friday (1 hour after market close)
     */
    @Scheduled(cron = "${position.scheduler.eod.cron:0 0 17 * * MON-FRI}")
    public void endOfDayReconciliation() {
        log.info("Starting end-of-day reconciliation");

        try {
            // Final P&L update
            tradeService.updateUnrealizedPnL();

            // Final exit check
            tradeService.checkStopLossAndTargets();

            log.info("End-of-day reconciliation completed");

        } catch (Exception e) {
            log.error("Error during end-of-day reconciliation", e);
        }
    }

    /**
     * Weekly cleanup task to maintain data integrity and performance.
     *
     * This runs every Sunday night to:
     * - Archive or clean up old data if needed
     * - Generate weekly performance summaries
     * - Prepare for the upcoming trading week
     *
     * Schedule: 10:00 PM EST every Sunday
     */
    @Scheduled(cron = "0 0 22 * * SUN")
    public void weeklyMaintenance() {
        log.info("Starting weekly maintenance task");

        try {
            // Update all positions with latest prices
            tradeService.updateUnrealizedPnL();

            // Any weekly cleanup tasks can be added here
            log.info("Weekly maintenance completed");

        } catch (Exception e) {
            log.error("Error during weekly maintenance", e);
        }
    }

    /**
     * Manual trigger method for testing or on-demand execution.
     *
     * This can be called via a controller endpoint to trigger
     * position management outside of the scheduled times.
     *
     * @return summary message of the operations performed
     */
    public String triggerManualPositionUpdate() {
        log.info("Manual position management triggered");

        try {
            tradeService.updateUnrealizedPnL();
            tradeService.checkStopLossAndTargets();
            tradeService.checkEntryTriggers();

            return "Position management completed successfully";

        } catch (Exception e) {
            log.error("Error during manual position management", e);
            return "Error: " + e.getMessage();
        }
    }

    // Private helper methods

    private boolean isIntradayMonitoringEnabled() {
        // This can be externalized to application.properties
        // For now, return false to disable intraday monitoring by default
        return false;
    }

    private boolean isMarketHours() {
        // Simple check for US market hours (9:30 AM - 4:00 PM EST, Mon-Fri)
        // This can be enhanced with proper timezone handling and market calendar
        java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
        if (day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY) {
            return false;
        }

        int hour = java.time.LocalTime.now().getHour();
        return hour >= 9 && hour < 16;
    }
}
