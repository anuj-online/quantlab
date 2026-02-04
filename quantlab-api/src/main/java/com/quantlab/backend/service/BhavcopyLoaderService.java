package com.quantlab.backend.service;

import com.quantlab.backend.dto.BhavcopyRow;
import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Instrument;
import com.quantlab.backend.entity.MarketType;
import com.quantlab.backend.repository.CandleRepository;
import com.quantlab.backend.repository.InstrumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Service for loading Bhavcopy CSV files into the database.
 * Implements idempotent loading with comprehensive logging.
 */
@Service
public class BhavcopyLoaderService {

    private static final Logger log = LoggerFactory.getLogger(BhavcopyLoaderService.class);

    /**
     * Date format for CSV DATE1 column: dd-MMM-yyyy (e.g., "01-Feb-2026")
     */
    private static final DateTimeFormatter CSV_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private final InstrumentRepository instrumentRepository;
    private final CandleRepository candleRepository;

    @Autowired
    public BhavcopyLoaderService(InstrumentRepository instrumentRepository,
                                 CandleRepository candleRepository) {
        this.instrumentRepository = instrumentRepository;
        this.candleRepository = candleRepository;
    }

    /**
     * Load a single Bhavcopy CSV file.
     * This method is idempotent - safe to call multiple times with the same file.
     * Can be called from both ApplicationReadyEvent and @Scheduled tasks.
     *
     * @param filePath Path to the Bhavcopy CSV file
     * @throws IOException if file reading fails
     */
    @Transactional
    public void loadBhavcopyFile(Path filePath) throws IOException {
        log.info("Starting Bhavcopy file load: {}", filePath);

        BhavcopyLoadMetrics metrics = new BhavcopyLoadMetrics();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            // Skip header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.warn("Empty file: {}", filePath);
                return;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                metrics.totalRowsRead++;

                try {
                    BhavcopyRow row = parseCsvLine(line);
                    if (row == null) {
                        continue;
                    }

                    // Filter: Only process EQ series
                    if (!"EQ".equals(row.getSeries())) {
                        continue;
                    }

                    metrics.equityRowsProcessed++;

                    // Find or create instrument
                    InstrumentWithCreationFlag wrapper = findOrCreateInstrument(row.getSymbol());
                    if (wrapper.isNewlyCreated()) {
                        metrics.instrumentsCreated++;
                    }

                    // Check if candle already exists (idempotent check)
                    if (candleRepository.existsByInstrumentIdAndTradeDate(
                            wrapper.getId(), row.getDate())) {
                        metrics.candlesSkipped++;
                        continue;
                    }

                    // Insert new candle
                    Candle candle = createCandle(wrapper.getInstrument(), row);
                    candleRepository.save(candle);
                    metrics.candlesInserted++;

                } catch (Exception e) {
                    log.error("Error processing line {}: {}", metrics.totalRowsRead, e.getMessage());
                    metrics.errors++;
                }
            }
        }

        logLoadSummary(filePath, metrics);
    }

    /**
     * Parse a single CSV line into a BhavcopyRow object.
     * Handles the comma-separated format with proper type conversions.
     *
     * @param line CSV line
     * @return BhavcopyRow or null if parsing fails
     */
    private BhavcopyRow parseCsvLine(String line) {
        String[] fields = line.split(",");
        if (fields.length < 15) {
            log.warn("Skipping malformed line with {} fields", fields.length);
            return null;
        }

        try {
            BhavcopyRow row = new BhavcopyRow();

            // SYMBOL (field 0)
            row.setSymbol(fields[0].trim());

            // SERIES (field 1)
            row.setSeries(fields[1].trim());

            // DATE1 (field 2) - Parse "dd-MMM-yyyy" format
            row.setDate(parseDate(fields[2].trim()));

            // PREV_CLOSE (field 3)
            row.setPrevClose(parseDecimal(fields[3].trim()));

            // OPEN_PRICE (field 4)
            row.setOpenPrice(parseDecimal(fields[4].trim()));

            // HIGH_PRICE (field 5)
            row.setHighPrice(parseDecimal(fields[5].trim()));

            // LOW_PRICE (field 6)
            row.setLowPrice(parseDecimal(fields[6].trim()));

            // LAST_PRICE (field 7) - Ignored per spec
            row.setLastPrice(parseDecimal(fields[7].trim()));

            // CLOSE_PRICE (field 8)
            row.setClosePrice(parseDecimal(fields[8].trim()));

            // AVG_PRICE (field 9) - Ignored per spec
            row.setAvgPrice(parseDecimal(fields[9].trim()));

            // TTL_TRD_QNTY (field 10) - Volume
            row.setTotalTradedQuantity(parseLong(fields[10].trim()));

            // TURNOVER_LACS (field 11) - Ignored per spec
            row.setTurnoverLacs(parseDecimal(fields[11].trim()));

            // NO_OF_TRADES (field 12) - Ignored per spec
            row.setNumberOfTrades(parseLong(fields[12].trim()));

            // DELIV_QTY (field 13) - Ignored per spec
            row.setDeliverableQuantity(parseLong(fields[13].trim()));

            // DELIV_PER (field 14) - Ignored per spec
            row.setDeliverablePercentage(parseDecimal(fields[14].trim()));

            // Validate essential fields required for candle creation
            // These fields MUST NOT be null as they are used for candle insertion
            if (row.getOpenPrice() == null) {
                log.error("Missing essential field OPEN_PRICE for symbol: {}", row.getSymbol());
                return null;
            }
            if (row.getHighPrice() == null) {
                log.error("Missing essential field HIGH_PRICE for symbol: {}", row.getSymbol());
                return null;
            }
            if (row.getLowPrice() == null) {
                log.error("Missing essential field LOW_PRICE for symbol: {}", row.getSymbol());
                return null;
            }
            if (row.getClosePrice() == null) {
                log.error("Missing essential field CLOSE_PRICE for symbol: {}", row.getSymbol());
                return null;
            }
            if (row.getTotalTradedQuantity() == null) {
                log.error("Missing essential field TTL_TRD_QNTY for symbol: {}", row.getSymbol());
                return null;
            }

            return row;

        } catch (DateTimeParseException e) {
            log.error("Date parsing error for line: {}", line);
            return null;
        } catch (NumberFormatException e) {
            log.error("Number parsing error for line: {}", line);
            return null;
        }
    }

    /**
     * Parse date string in "dd-MMM-yyyy" format (e.g., "01-Feb-2026").
     *
     * @param dateStr date string
     * @return LocalDate
     * @throws DateTimeParseException if parsing fails
     */
    private LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, CSV_DATE_FORMATTER);
    }

    /**
     * Parse decimal string to BigDecimal.
     * Handles empty strings and "-" as null (used in Bhavcopy for missing values).
     *
     * @param value decimal string
     * @return BigDecimal or null
     */
    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isEmpty() || "-".equals(value)) {
            return null;
        }
        return new BigDecimal(value);
    }

    /**
     * Parse long integer string.
     * Handles empty strings and "-" as null (used in Bhavcopy for missing values).
     *
     * @param value long string
     * @return Long or null
     */
    private Long parseLong(String value) {
        if (value == null || value.isEmpty() || "-".equals(value)) {
            return null;
        }
        return Long.parseLong(value);
    }

    /**
     * Find or create an instrument for the given symbol.
     * Instruments are always created with market=INDIA and active=true.
     *
     * @param symbol trading symbol
     * @return Instrument wrapper with a flag indicating if it was newly created
     */
    private InstrumentWithCreationFlag findOrCreateInstrument(String symbol) {
        Instrument existing = instrumentRepository.findBySymbolAndMarket(symbol, MarketType.INDIA);

        if (existing != null) {
            return new InstrumentWithCreationFlag(existing, false);
        }

        // Create new instrument
        Instrument newInstrument = new Instrument();
        newInstrument.setSymbol(symbol);
        newInstrument.setName(symbol); // Name defaults to symbol initially
        newInstrument.setMarket(MarketType.INDIA);
        newInstrument.setActive(true);

        Instrument saved = instrumentRepository.save(newInstrument);
        return new InstrumentWithCreationFlag(saved, true);
    }

    /**
     * Create a Candle entity from BhavcopyRow.
     *
     * @param instrument the instrument
     * @param row the Bhavcopy row data
     * @return Candle entity
     */
    private Candle createCandle(Instrument instrument, BhavcopyRow row) {
        Candle candle = new Candle();
        candle.setInstrument(instrument);
        candle.setTradeDate(row.getDate());
        candle.setOpen(row.getOpenPrice());
        candle.setHigh(row.getHighPrice());
        candle.setLow(row.getLowPrice());
        candle.setClose(row.getClosePrice());
        candle.setVolume(row.getTotalTradedQuantity());
        return candle;
    }

    /**
     * Log a comprehensive summary of the load operation.
     *
     * @param filePath the file that was loaded
     * @param metrics load metrics
     */
    private void logLoadSummary(Path filePath, BhavcopyLoadMetrics metrics) {
        log.info("========================================");
        log.info("Bhavcopy Load Summary");
        log.info("========================================");
        log.info("File: {}", filePath.getFileName());
        log.info("Trading Date: {}", metrics.tradingDate);
        log.info("Total Rows Read: {}", metrics.totalRowsRead);
        log.info("EQ Rows Processed: {}", metrics.equityRowsProcessed);
        log.info("Instruments Created: {}", metrics.instrumentsCreated);
        log.info("Candles Inserted: {}", metrics.candlesInserted);
        log.info("Candles Skipped (already exist): {}", metrics.candlesSkipped);
        if (metrics.errors > 0) {
            log.warn("Errors: {}", metrics.errors);
        }
        log.info("========================================");
    }

    /**
     * Wrapper class to track whether an instrument was newly created.
     */
    private static class InstrumentWithCreationFlag {
        private final Instrument instrument;
        private final boolean newlyCreated;

        InstrumentWithCreationFlag(Instrument instrument, boolean newlyCreated) {
            this.instrument = instrument;
            this.newlyCreated = newlyCreated;
        }

        public Instrument getInstrument() {
            return instrument;
        }

        public boolean isNewlyCreated() {
            return newlyCreated;
        }

        public Long getId() {
            return instrument.getId();
        }
    }

    /**
     * Metrics class to track load statistics.
     */
    private static class BhavcopyLoadMetrics {
        int totalRowsRead = 0;
        int equityRowsProcessed = 0;
        int instrumentsCreated = 0;
        int candlesInserted = 0;
        int candlesSkipped = 0;
        int errors = 0;
        LocalDate tradingDate;

        BhavcopyLoadMetrics() {
        }
    }
}
