package com.quantlab.backend.config;

import com.quantlab.backend.strategy.Strategy;
import com.quantlab.backend.strategy.StrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Auto-registers all Strategy implementations with the StrategyRegistry on application startup.
 *
 * This component scans the Spring application context for all beans that implement
 * the Strategy interface and registers them with the StrategyRegistry using their getCode()
 * as the registration key.
 */
@Component
public class StrategyAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(StrategyAutoConfiguration.class);

    private final ApplicationContext applicationContext;
    private final StrategyRegistry strategyRegistry;

    public StrategyAutoConfiguration(ApplicationContext applicationContext, StrategyRegistry strategyRegistry) {
        this.applicationContext = applicationContext;
        this.strategyRegistry = strategyRegistry;
    }

    /**
     * Registers all Strategy beans with the StrategyRegistry when the application is ready.
     * This runs after all beans have been created and initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void registerStrategies() {
        logger.info("Auto-registering Strategy implementations with StrategyRegistry");

        // Get all beans that implement Strategy interface
        Map<String, Strategy> strategies = applicationContext.getBeansOfType(Strategy.class);

        logger.info("Found {} Strategy beans to register", strategies.size());

        int registeredCount = 0;
        for (Map.Entry<String, Strategy> entry : strategies.entrySet()) {
            String beanName = entry.getKey();
            Strategy strategy = entry.getValue();

            try {
                String code = strategy.getCode();
                strategyRegistry.register(code, strategy);
                registeredCount++;
                logger.debug("Registered strategy: {} (bean: {}) with code: {}",
                        strategy.getClass().getSimpleName(), beanName, code);
            } catch (Exception e) {
                logger.error("Failed to register strategy bean: {} - {}",
                        beanName, e.getMessage(), e);
            }
        }

        logger.info("Strategy auto-registration completed. Registered: {} strategies", registeredCount);
        logger.info("Available strategy codes: {}", strategyRegistry.getRegisteredCodes());
    }
}
