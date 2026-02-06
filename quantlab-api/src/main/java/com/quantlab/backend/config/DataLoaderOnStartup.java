package com.quantlab.backend.config;

import com.quantlab.backend.service.BhavcopyLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

/**
 * Component that triggers data loading when the application is ready.
 * Scans the configured directory for Bhavcopy CSV files and loads them.
 *
 * Files are processed in chronological order (by filename date).
 * This ensures data is loaded in the correct sequence.
 *
 * File format: YYYYMMDD_NSE.csv (e.g., 20250701_NSE.csv)
 *
 * Designed to support both:
 * - Application startup (ApplicationReadyEvent)
 * - Future @Scheduled cron tasks
 */
@Component
public class DataLoaderOnStartup {

    private static final Logger log = LoggerFactory.getLogger(DataLoaderOnStartup.class);

    /**
     * Pattern for Bhavcopy files: *_NSE.csv
     * Example: 20250701_NSE.csv (format: YYYYMMDD_NSE.csv)
     */
    private static final String BHAVCOPY_FILE_PATTERN = "*_NSE.csv";

    private final BhavcopyLoaderService bhavcopyLoaderService;

    @Value("${bhavcopy.file.dir}")
    private String bhavcopyFileDir;

    @Autowired
    public DataLoaderOnStartup(BhavcopyLoaderService bhavcopyLoaderService,
                               Executor bhavcopyTaskExecutor) {
        this.bhavcopyLoaderService = bhavcopyLoaderService;
        this.bhavcopyTaskExecutor = (ThreadPoolTaskExecutor) bhavcopyTaskExecutor;
    }

    /**
     * Task executor for parallel file processing.
     * Injected from AsyncConfig.bhavcopyTaskExecutor bean.
     */
    private final ThreadPoolTaskExecutor bhavcopyTaskExecutor;

    /**
     * Triggered when the Spring Boot application is fully ready.
     * Loads all Bhavcopy CSV files from the configured directory in parallel.
     */
//    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready. Starting Bhavcopy data loading...");
        loadAllBhavcopyFilesParallel();
    }

    /**
     * Load all Bhavcopy CSV files from the configured directory.
     * Files are processed in chronological order based on the date in the filename.
     *
     * This method is public so it can be called from scheduled tasks in the future.
     */
    public void loadAllBhavcopyFiles() {
        Path dirPath = Paths.get(bhavcopyFileDir);

        if (!Files.exists(dirPath)) {
            log.error("Bhavcopy directory does not exist: {}", bhavcopyFileDir);
            return;
        }

        if (!Files.isDirectory(dirPath)) {
            log.error("Bhavcopy path is not a directory: {}", bhavcopyFileDir);
            return;
        }

        log.info("Scanning directory for Bhavcopy files: {}", bhavcopyFileDir);

        try (Stream<Path> files = Files.list(dirPath)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isBhavcopyFile)
                    .sorted(this::compareByFileDate)
                    .forEach(this::loadFileSafely);

        } catch (IOException e) {
            log.error("Error listing Bhavcopy files", e);
        }
    }

    /**
     * Load all Bhavcopy CSV files in parallel using the configured thread pool.
     * Files are processed in chronological order based on the date in the filename.
     * Multiple files are processed concurrently while respecting database connection limits.
     *
     * Uses batch insert operations for improved performance.
     *
     * This method is public so it can be called from scheduled tasks in the future.
     */
    public void loadAllBhavcopyFilesParallel() {
        Path dirPath = Paths.get(bhavcopyFileDir);

        if (!Files.exists(dirPath)) {
            log.error("Bhavcopy directory does not exist: {}", bhavcopyFileDir);
            return;
        }

        if (!Files.isDirectory(dirPath)) {
            log.error("Bhavcopy path is not a directory: {}", bhavcopyFileDir);
            return;
        }

        log.info("Scanning directory for Bhavcopy files (parallel mode): {}", bhavcopyFileDir);

        List<Path> bhavcopyFiles;
        try (Stream<Path> files = Files.list(dirPath)) {
            bhavcopyFiles = files.filter(Files::isRegularFile)
                    .filter(this::isBhavcopyFile)
                    .sorted(this::compareByFileDate)
                    .toList();

        } catch (IOException e) {
            log.error("Error listing Bhavcopy files", e);
            return;
        }

        if (bhavcopyFiles.isEmpty()) {
            log.info("No Bhavcopy files found");
            return;
        }

        log.info("Found {} Bhavcopy files. Processing in parallel...", bhavcopyFiles.size());
        long startTime = System.currentTimeMillis();

        // Submit all files for parallel processing
        List<CompletableFuture<BhavcopyLoaderService.BhavcopyLoadMetrics>> futures = new ArrayList<>();
        for (Path file : bhavcopyFiles) {
            CompletableFuture<BhavcopyLoaderService.BhavcopyLoadMetrics> future = CompletableFuture.supplyAsync(
                    () -> loadFileSafelyWithMetrics(file),
                    bhavcopyTaskExecutor
            );
            futures.add(future);
        }

        // Wait for all tasks to complete and aggregate results
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allOf.join(); // Wait for completion

            List<BhavcopyLoaderService.BhavcopyLoadMetrics> metricsList = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            BhavcopyLoaderService.BhavcopyLoadMetrics aggregated = BhavcopyLoaderService.aggregateMetrics(metricsList);

            long duration = System.currentTimeMillis() - startTime;
            log.info("========================================");
            log.info("Parallel Bhavcopy Load Complete");
            log.info("========================================");
            log.info("Total Files Processed: {}", bhavcopyFiles.size());
            log.info("Total Rows Read: {}", aggregated.totalRowsRead);
            log.info("Total EQ Rows Processed: {}", aggregated.equityRowsProcessed);
            log.info("Total Instruments Created: {}", aggregated.instrumentsCreated);
            log.info("Total Candles Inserted: {}", aggregated.candlesInserted);
            log.info("Total Candles Skipped: {}", aggregated.candlesSkipped);
            if (aggregated.errors > 0) {
                log.warn("Total Errors: {}", aggregated.errors);
            }
            log.info("Total Duration: {} ms", duration);
            log.info("========================================");

        } catch (Exception e) {
            log.error("Error during parallel Bhavcopy loading", e);
        }
    }

    /**
     * Load a single Bhavcopy file safely with error handling.
     * Can be called directly to load a specific file.
     *
     * @param filePath Path to the Bhavcopy CSV file
     */
    public void loadFileSafely(Path filePath) {
        try {
            log.info("Loading file: {}", filePath.getFileName());
            bhavcopyLoaderService.loadBhavcopyFile(filePath);
            log.info("Successfully loaded: {}", filePath.getFileName());

        } catch (IOException e) {
            log.error("Failed to load file: {}", filePath.getFileName(), e);
        }
    }

    /**
     * Load a single Bhavcopy file safely with error handling and return metrics.
     * Uses batch insert operations for improved performance.
     * Used by parallel file processing.
     *
     * @param filePath Path to the Bhavcopy CSV file
     * @return metrics summarizing the load operation (empty metrics on error)
     */
    private BhavcopyLoaderService.BhavcopyLoadMetrics loadFileSafelyWithMetrics(Path filePath) {
        try {
            log.info("[Thread: {}] Loading file: {}", Thread.currentThread().getName(), filePath.getFileName());
            BhavcopyLoaderService.BhavcopyLoadMetrics metrics = bhavcopyLoaderService.loadBhavcopyFileBatch(filePath);
            log.info("[Thread: {}] Successfully loaded: {} (inserted: {}, skipped: {})",
                    Thread.currentThread().getName(), filePath.getFileName(),
                    metrics.candlesInserted, metrics.candlesSkipped);
            return metrics;

        } catch (IOException e) {
            log.error("[Thread: {}] Failed to load file: {}",
                    Thread.currentThread().getName(), filePath.getFileName(), e);
            BhavcopyLoaderService.BhavcopyLoadMetrics errorMetrics = new BhavcopyLoaderService.BhavcopyLoadMetrics();
            errorMetrics.errors = 1;
            return errorMetrics;
        }
    }

    /**
     * Check if a file matches the Bhavcopy naming pattern.
     * New format: YYYYMMDD_NSE.csv
     * Example: 20250701_NSE.csv
     *
     * @param path file path
     * @return true if file matches pattern
     */
    private boolean isBhavcopyFile(Path path) {
        String fileName = path.getFileName().toString();
        return fileName.endsWith("_NSE.csv") &&
                fileName.length() == "_NSE.csv".length() + 8; // 8 digits for YYYYMMDD
    }

    /**
     * Compare two Bhavcopy files by the date embedded in their filenames.
     * Format: YYYYMMDD_NSE.csv
     *
     * @param p1 first file path
     * @param p2 second file path
     * @return comparator result
     */
    private int compareByFileDate(Path p1, Path p2) {
        LocalDate date1 = extractDateFromFilename(p1.getFileName().toString());
        LocalDate date2 = extractDateFromFilename(p2.getFileName().toString());

        if (date1 == null && date2 == null) {
            return 0;
        }
        if (date1 == null) {
            return 1;
        }
        if (date2 == null) {
            return -1;
        }

        return date1.compareTo(date2);
    }

    /**
     * Extract date from Bhavcopy filename.
     * New format: YYYYMMDD_NSE.csv -> LocalDate
     *
     * Example: 20250701_NSE.csv -> 2025-07-01
     *
     * @param filename the filename
     * @return LocalDate or null if parsing fails
     */
    private LocalDate extractDateFromFilename(String filename) {
        try {
            // Extract YYYYMMDD part: 20250701_NSE.csv
            String baseName = filename.replace("_NSE.csv", "");
            String dateStr = baseName;

            if (dateStr.length() != 8) {
                log.warn("Unexpected filename format: {}", filename);
                return null;
            }

            int year = Integer.parseInt(dateStr.substring(0, 4));
            int month = Integer.parseInt(dateStr.substring(4, 6));
            int day = Integer.parseInt(dateStr.substring(6, 8));

            return LocalDate.of(year, month, day);

        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            log.warn("Failed to parse date from filename: {}", filename);
            return null;
        }
    }
}
