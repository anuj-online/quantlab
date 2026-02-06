package com.quantlab.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class to enable Spring's scheduled task execution capability.
 *
 * This allows the use of @Scheduled annotations on methods for:
 * - Cron-based scheduling (e.g., market close times)
 * - Fixed-rate or fixed-delay scheduling
 * - Initial delay scheduling
 *
 * Scheduled tasks are used for:
 * - Updating unrealized P&L for open positions
 * - Checking stop loss and target levels
 * - Auto-executing pending signals at trigger levels
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
    // No additional configuration needed
    // The @EnableScheduling annotation enables detection of @Scheduled annotations
}
