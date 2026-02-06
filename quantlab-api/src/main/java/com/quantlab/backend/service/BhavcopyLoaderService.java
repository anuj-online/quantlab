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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
     * New CSV Format (13 columns):
     * SYMBOL,SERIES,OPEN,HIGH,LOW,CLOSE,LAST,PREVCLOSE,TOTTRDQTY,TOTTRDVAL,TIMESTAMP,TOTALTRADES,ISIN
     *
     * @param line CSV line
     * @return BhavcopyRow or null if parsing fails
     */
    private BhavcopyRow parseCsvLine(String line) {
        String[] fields = line.split(",");
        if (fields.length < 13) {
            log.warn("Skipping malformed line with {} fields", fields.length);
            return null;
        }

        try {
            BhavcopyRow row = new BhavcopyRow();

            // SYMBOL (field 0)
            row.setSymbol(fields[0].trim());

            // SERIES (field 1)
            row.setSeries(fields[1].trim());

            // OPEN (field 2) - formerly OPEN_PRICE
            row.setOpenPrice(parseDecimal(fields[2].trim()));

            // HIGH (field 3) - formerly HIGH_PRICE
            row.setHighPrice(parseDecimal(fields[3].trim()));

            // LOW (field 4) - formerly LOW_PRICE
            row.setLowPrice(parseDecimal(fields[4].trim()));

            // CLOSE (field 5) - formerly CLOSE_PRICE
            row.setClosePrice(parseDecimal(fields[5].trim()));

            // LAST (field 6) - formerly LAST_PRICE
            row.setLastPrice(parseDecimal(fields[6].trim()));

            // PREVCLOSE (field 7) - formerly PREV_CLOSE
            row.setPrevClose(parseDecimal(fields[7].trim()));

            // TOTTRDQTY (field 8) - formerly TTL_TRD_QNTY
            row.setTotalTradedQuantity(parseLong(fields[8].trim()));

            // TOTTRDVAL (field 9) - formerly TURNOVER_LACS
            row.setTurnoverLacs(parseDecimal(fields[9].trim()));

            // TIMESTAMP (field 10) - formerly DATE1
            row.setDate(parseDate(fields[10].trim()));

            // TOTALTRADES (field 11) - formerly NO_OF_TRADES
            row.setNumberOfTrades(parseLong(fields[11].trim()));

            // ISIN (field 12) - Not stored in BhavcopyRow (ignored)

            // Fields removed in new format (not parsed):
            // - AVG_PRICE (field 9 in old format)
            // - DELIV_QTY (field 13 in old format)
            // - DELIV_PER (field 14 in old format)

            // Validate essential fields required for candle creation
            // These fields MUST NOT be null as they are used for candle insertion
            if (row.getOpenPrice() == null) {
                log.error("Missing essential field OPEN for symbol: {}", row.getSymbol());
                return null;
            }
            if (row.getHighPrice() == null) {
                log.error("Missing essential field HIGH for symbol: {}", row.getSymbol());
                return null;
            }
            if (row.getLowPrice() == null) {
                log.error("Missing essential field LOW for symbol: {}", row.getSymbol());
                return null;
            }
            if (row.getClosePrice() == null) {
                log.error("Missing essential field CLOSE for symbol: {}", row.getSymbol());
                return null;
            }
            if (row.getTotalTradedQuantity() == null) {
                log.error("Missing essential field TOTTRDQTY for symbol: {}", row.getSymbol());
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
     * Made public for access by parallel file processing components.
     */
    public static class BhavcopyLoadMetrics {
        public int totalRowsRead = 0;
        public int equityRowsProcessed = 0;
        public int instrumentsCreated = 0;
        public int candlesInserted = 0;
        public int candlesSkipped = 0;
        public int errors = 0;
        public LocalDate tradingDate;

        public BhavcopyLoadMetrics() {
        }
    }

    // ==================== BATCH PROCESSING METHODS ====================
    // These methods optimize performance for parallel file processing

    /**
     * Batch load Bhavcopy file using bulk operations.
     * More efficient than loadBhavcopyFile for parallel processing.
     *
     * @param filePath Path to the Bhavcopy CSV file
     * @return metrics summarizing the load operation
     * @throws IOException if file reading fails
     */
    @Transactional
    public BhavcopyLoadMetrics loadBhavcopyFileBatch(Path filePath) throws IOException {
        log.info("Starting batch Bhavcopy file load: {}", filePath);

        BhavcopyLoadMetrics metrics = new BhavcopyLoadMetrics();
        List<BhavcopyRow> allRows = new ArrayList<>();
        Set<String> allSymbols = new java.util.HashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            // Skip header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.warn("Empty file: {}", filePath);
                return metrics;
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
                    allRows.add(row);
                    allSymbols.add(row.getSymbol());

                    if (metrics.tradingDate == null) {
                        metrics.tradingDate = row.getDate();
                    }

                } catch (Exception e) {
                    log.error("Error parsing line {}: {}", metrics.totalRowsRead, e.getMessage());
                    metrics.errors++;
                }
            }
        }

        // Batch process: find/create instruments
        Map<String, Instrument> instrumentMap = batchFindOrCreateInstruments(allSymbols, metrics);

        // Batch check existing candles
        List<Long> instrumentIds = allRows.stream()
                .map(r -> instrumentMap.get(r.getSymbol()).getId())
                .distinct()
                .toList();
        List<LocalDate> tradeDates = allRows.stream()
                .map(BhavcopyRow::getDate)
                .distinct()
                .toList();

        Set<Long> existingInstrumentIds = Set.copyOf(
                candleRepository.findExistingInstrumentIds(instrumentIds, tradeDates)
        );

        // Batch insert new candles
        List<Candle> candlesToInsert = new ArrayList<>();
        for (BhavcopyRow row : allRows) {
            Instrument instrument = instrumentMap.get(row.getSymbol());
            Long instrumentId = instrument.getId();
            LocalDate tradeDate = row.getDate();

            // Simple existence check (could be improved with composite key check)
            boolean candleExists = existingInstrumentIds.contains(instrumentId);

            if (!candleExists) {
                Candle candle = createCandle(instrument, row);
                candlesToInsert.add(candle);
            } else {
                metrics.candlesSkipped++;
            }
        }

        if (!candlesToInsert.isEmpty()) {
            List<Candle> saved = candleRepository.saveAll(candlesToInsert);
            metrics.candlesInserted = saved.size();
        }

        logLoadSummary(filePath, metrics);
        return metrics;
    }

    /**
     * Batch find or create instruments for the given symbols.
     * Uses a single bulk query to find existing instruments,
     * then creates any missing ones using idempotent inserts to handle
     * race conditions in parallel bhavcopy loading.
     *
     * @param symbols set of trading symbols
     * @param metrics metrics to update with creation count
     * @return map of symbol to Instrument
     */
    private Map<String, Instrument> batchFindOrCreateInstruments(Set<String> symbols, BhavcopyLoadMetrics metrics) {
        // Bulk query for existing instruments
        List<String> symbolList = new ArrayList<>(symbols);
        List<Instrument> existingInstruments = instrumentRepository
                .findBySymbolInAndMarket(symbolList, MarketType.INDIA);

        Map<String, Instrument> instrumentMap = existingInstruments.stream()
                .collect(Collectors.toMap(Instrument::getSymbol, i -> i));

        // Find missing symbols and insert them idempotently
        int createdCount = 0;
        for (String symbol : symbols) {
            if (!instrumentMap.containsKey(symbol)) {
                // Use idempotent insert - handles race condition from parallel threads
                Optional<Instrument> inserted = instrumentRepository.insertIfNotExists(
                        symbol,
                        symbol,
                        MarketType.INDIA.name()
                );

                if (inserted.isPresent()) {
                    instrumentMap.put(symbol, inserted.get());
                    createdCount++;
                } else {
                    // Insert failed due to conflict (another thread inserted it)
                    // Re-query to get the existing instrument
                    Instrument existing = instrumentRepository.findBySymbolAndMarket(symbol, MarketType.INDIA);
                    if (existing != null) {
                        instrumentMap.put(symbol, existing);
                    }
                }
            }
        }

        metrics.instrumentsCreated = createdCount;
        return instrumentMap;
    }

    /**
     * Aggregate metrics from multiple parallel file loads.
     *
     * @param metricsList list of metrics from parallel loads
     * @return aggregated metrics
     */
    public static BhavcopyLoadMetrics aggregateMetrics(List<BhavcopyLoadMetrics> metricsList) {
        BhavcopyLoadMetrics aggregated = new BhavcopyLoadMetrics();

        for (BhavcopyLoadMetrics m : metricsList) {
            aggregated.totalRowsRead += m.totalRowsRead;
            aggregated.equityRowsProcessed += m.equityRowsProcessed;
            aggregated.instrumentsCreated += m.instrumentsCreated;
            aggregated.candlesInserted += m.candlesInserted;
            aggregated.candlesSkipped += m.candlesSkipped;
            aggregated.errors += m.errors;
            if (m.tradingDate != null && (aggregated.tradingDate == null || m.tradingDate.isAfter(aggregated.tradingDate))) {
                aggregated.tradingDate = m.tradingDate;
            }
        }

        return aggregated;
    }
}
