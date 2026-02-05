package com.quantlab.backend.config;

import com.quantlab.backend.service.BhavcopyLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Comparator;
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
    public DataLoaderOnStartup(BhavcopyLoaderService bhavcopyLoaderService) {
        this.bhavcopyLoaderService = bhavcopyLoaderService;
    }

    /**
     * Triggered when the Spring Boot application is fully ready.
     * Loads all Bhavcopy CSV files from the configured directory.
     */
//    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready. Starting Bhavcopy data loading...");
        loadAllBhavcopyFiles();
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
