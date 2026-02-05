package com.quantlab.backend.config;

import com.quantlab.backend.entity.Strategy;
import com.quantlab.backend.repository.StrategyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Component that seeds the database with initial trading strategies on application startup.
 * Checks for existence by strategy code before inserting to avoid duplicates.
 *
 * This seeder ensures the database is populated with core strategy definitions
 * required for the quantitative trading system.
 *
 * Designed to coexist with other startup components like DataLoaderOnStartup.
 */
@Component
public class StrategySeeder {

    private static final Logger log = LoggerFactory.getLogger(StrategySeeder.class);

    private final StrategyRepository strategyRepository;

    @Autowired
    public StrategySeeder(StrategyRepository strategyRepository) {
        this.strategyRepository = strategyRepository;
    }

    /**
     * Triggered when the Spring Boot application is fully ready.
     * Seeds the database with initial strategy definitions if they don't exist.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        log.info("Application ready. Checking strategy seed data...");

        int createdCount = 0;
        int skippedCount = 0;

        // Seed EMA_BREAKOUT strategy
        if (seedStrategyIfNotExists(
                "EMA_BREAKOUT",
                "EOD Breakout with Volume",
                "Buy on close > 20-day high with volume confirmation"
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed SMA_CROSSOVER strategy
        if (seedStrategyIfNotExists(
                "SMA_CROSSOVER",
                "SMA Crossover",
                "Buy when fast SMA crosses above slow SMA"
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed GAP_UP_MOMENTUM strategy
        if (seedStrategyIfNotExists(
                "GAP_UP_MOMENTUM",
                "Gap-Up Momentum",
                "Buy on gap-up opening with 1% stop loss"
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        log.info("Strategy seeding completed. Created: {}, Skipped: {}", createdCount, skippedCount);
    }

    /**
     * Seeds a single strategy if it doesn't already exist in the database.
     * Uses the strategy code for uniqueness check.
     *
     * @param code the unique strategy code
     * @param name the strategy display name
     * @param description the strategy description
     * @return true if strategy was created, false if it already existed
     */
    private boolean seedStrategyIfNotExists(String code, String name, String description) {
        return strategyRepository.findByCode(code)
                .map(existing -> {
                    log.debug("Strategy already exists: {} - {}", code, name);
                    return false;
                })
                .orElseGet(() -> {
                    Strategy strategy = new Strategy();
                    strategy.setCode(code);
                    strategy.setName(name);
                    strategy.setDescription(description);
                    strategy.setActive(true);

                    Strategy saved = strategyRepository.save(strategy);
                    log.info("Seeded strategy: {} - {}", saved.getCode(), saved.getName());
                    return true;
                });
    }
}
