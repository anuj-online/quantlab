package com.quantlab.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous task execution.
 * Enables parallel processing of Bhavcopy files and other async operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Core pool size for async task executor.
     * Default: 2 threads (conservative for database operations)
     */
    @Value("${bhavcopy.async.corePoolSize:2}")
    private int corePoolSize;

    /**
     * Maximum pool size for async task executor.
     * Default: 4 threads (prevents overwhelming the database)
     */
    @Value("${bhavcopy.async.maxPoolSize:4}")
    private int maxPoolSize;

    /**
     * Queue capacity for pending tasks.
     * Default: 10 (limits memory usage when many files are pending)
     */
    @Value("${bhavcopy.async.queueCapacity:10}")
    private int queueCapacity;

    /**
     * Thread name prefix for easier debugging.
     */
    private static final String THREAD_NAME_PREFIX = "bhavcopy-async-";

    /**
     * Core pool size for screening task executor.
     * Default: 4 threads (higher for I/O-bound screening operations)
     */
    @Value("${screening.async.corePoolSize:4}")
    private int screeningCorePoolSize;

    /**
     * Maximum pool size for screening task executor.
     * Default: 8 threads (for parallel strategy and instrument processing)
     */
    @Value("${screening.async.maxPoolSize:8}")
    private int screeningMaxPoolSize;

    /**
     * Task executor for Bhavcopy file processing.
     * Uses a bounded thread pool to prevent database connection exhaustion.
     *
     * @return configured task executor
     */
    @Bean(name = "bhavcopyTaskExecutor")
    public Executor bhavcopyTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(THREAD_NAME_PREFIX);

        // Rejection policy: caller runs to prevent task loss
        // If queue is full, the calling thread will execute the task
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Initialized BhavcopyTaskExecutor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                corePoolSize, maxPoolSize, queueCapacity);

        return executor;
    }

    /**
     * Task executor for screening operations.
     * Uses a larger thread pool for parallel strategy and instrument processing.
     * <p>
     * Screening is I/O-bound (market data fetching, database queries),
     * so a higher thread count improves performance significantly.
     * </p>
     *
     * @return configured task executor for screening
     */
    @Bean(name = "screeningTaskExecutor")
    public Executor screeningTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(screeningCorePoolSize);
        executor.setMaxPoolSize(screeningMaxPoolSize);
        executor.setQueueCapacity(100); // Larger queue for screening
        executor.setThreadNamePrefix("screening-async-");

        // Rejection policy: caller runs to prevent task loss
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Initialized ScreeningTaskExecutor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                screeningCorePoolSize, screeningMaxPoolSize, 100);

        return executor;
    }
}
