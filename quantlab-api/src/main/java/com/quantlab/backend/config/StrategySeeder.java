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

        // Seed EOD_BREAKOUT strategy
        if (seedStrategyIfNotExists(
                "EOD_BREAKOUT",
                "EOD Breakout with Volume",
                "Buy on close > 20-day high with volume confirmation",
                true,
                20
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed SMA_CROSSOVER strategy
        if (seedStrategyIfNotExists(
                "SMA_CROSSOVER",
                "SMA 20/50 Crossover",
                "Buy when SMA 20 crosses above SMA 50",
                true,
                50
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed GAP_UP_MOMENTUM strategy
        if (seedStrategyIfNotExists(
                "GAP_UP_MOMENTUM",
                "Gap-Up Momentum",
                "Buy on gap-up opening with 1% stop loss",
                true,
                5
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed NR4_INSIDE_BAR strategy
        if (seedStrategyIfNotExists(
                "NR4_INSIDE_BAR",
                "NR4 + Inside Bar Volatility Squeeze",
                "Volatility contraction pattern: NR4 + Inside Bar for breakout screening",
                true,
                4
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Priority 1: Price Action (Single Candle Patterns)

        // Seed MOMENTUM_3D strategy
        if (seedStrategyIfNotExists(
                "MOMENTUM_3D",
                "3-Day Momentum Burst",
                "3 consecutive bullish candles + increasing volume + close > 10-day high",
                true,
                10
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed GAP_HOLD strategy
        if (seedStrategyIfNotExists(
                "GAP_HOLD",
                "Gap-Up Hold Continuation",
                "Gap up > 1.5% + range holds + close near high",
                true,
                5
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed EMA20_PULLBACK strategy
        if (seedStrategyIfNotExists(
                "EMA20_PULLBACK",
                "Trend Pullback to EMA20",
                "Price above EMA50 + pullback to EMA20 + bullish close",
                true,
                50
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Priority 2: Volatility Patterns

        // Seed BB_SQUEEZE strategy
        if (seedStrategyIfNotExists(
                "BB_SQUEEZE",
                "Bollinger Band Squeeze",
                "BB width lowest in 20 days + breakout outside band",
                true,
                20
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed RANGE_BREAK_VOL strategy
        if (seedStrategyIfNotExists(
                "RANGE_BREAK_VOL",
                "Volume Expansion Breakout",
                "Tight range 10 days + breakout with volume > 2x",
                true,
                10
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Priority 3: Reversal Patterns

        // Seed HV_REVERSAL strategy
        if (seedStrategyIfNotExists(
                "HV_REVERSAL",
                "High Volume Reversal",
                "Volume >= 2x + close near low + bullish next day (2-candle pattern)",
                true,
                20
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed FAILED_BREAKDOWN strategy
        if (seedStrategyIfNotExists(
                "FAILED_BREAKDOWN",
                "Failed Breakdown (Bear Trap)",
                "Break support + close above next day + high volume (2-candle pattern)",
                true,
                20
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Priority 4: Structure & Trend

        // Seed HH_HL_STRUCTURE strategy
        if (seedStrategyIfNotExists(
                "HH_HL_STRUCTURE",
                "Higher High Higher Low Structure",
                "3 swing highs increasing + 3 swing lows increasing (swing detection)",
                true,
                30
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed REL_STRENGTH_30D strategy
        if (seedStrategyIfNotExists(
                "REL_STRENGTH_30D",
                "Relative Strength Momentum (30-Day)",
                "Stock return > Index return + breaks high (requires index data)",
                true,
                30
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed RSI_MEAN_REVERSION strategy
        if (seedStrategyIfNotExists(
                "RSI_MEAN_REVERSION",
                "RSI Mean Reversion",
                "Buy on RSI crossing below oversold threshold (30) with bullish candle and volume confirmation",
                true,
                35
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed MORNING_STAR strategy
        if (seedStrategyIfNotExists(
                "MORNING_STAR",
                "Morning Star Reversal",
                "Three-candle bullish reversal: large bearish candle + small doji + strong bullish candle",
                true,
                3
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed ATR_BREAKOUT strategy
        if (seedStrategyIfNotExists(
                "ATR_BREAKOUT",
                "ATR Volatility Breakout",
                "Buy when today's range exceeds 1.5x ATR with volume confirmation",
                true,
                35
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed RSI_DIVERGENCE strategy
        if (seedStrategyIfNotExists(
                "RSI_DIVERGENCE",
                "RSI Divergence Detector",
                "Identifies hidden and regular divergences between price and RSI for leading reversal signals",
                true,
                30
        )) {
            createdCount++;
        } else {
            skippedCount++;
        }

        // Seed DOJI_REVERSAL strategy
        if (seedStrategyIfNotExists(
                "DOJI_REVERSAL",
                "Doji Candlestick Reversal",
                "Identifies doji patterns (Gravestone, Dragonfly, Standard) with confirmation candles",
                true,
                2
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
     * @param supportsScreening whether the strategy supports screening mode
     * @param minLookbackDays minimum days of historical data required
     * @return true if strategy was created, false if it already existed
     */
    private boolean seedStrategyIfNotExists(String code, String name, String description,
                                            boolean supportsScreening, int minLookbackDays) {
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
                    strategy.setSupportsScreening(supportsScreening);
                    strategy.setMinLookbackDays(minLookbackDays);

                    Strategy saved = strategyRepository.save(strategy);
                    log.info("Seeded strategy: {} - {}", saved.getCode(), saved.getName());
                    return true;
                });
    }
}
