package com.quantlab.backend.service;

import com.quantlab.backend.domain.TradeSignal;
import com.quantlab.backend.dto.ScreeningRequest;
import com.quantlab.backend.dto.ScreeningResponse;
import com.quantlab.backend.dto.ScreeningSignal;
import com.quantlab.backend.entity.Instrument;
import com.quantlab.backend.entity.MarketType;
import com.quantlab.backend.entity.ScreeningResult;
import com.quantlab.backend.entity.Side;
import com.quantlab.backend.entity.SignalType;
import com.quantlab.backend.repository.CandleRepository;
import com.quantlab.backend.repository.InstrumentRepository;
import com.quantlab.backend.repository.ScreeningResultsRepository;
import com.quantlab.backend.repository.StrategyRepository;
import com.quantlab.backend.strategy.ExecutionMode;
import com.quantlab.backend.strategy.Strategy;
import com.quantlab.backend.strategy.StrategyContext;
import com.quantlab.backend.strategy.StrategyParams;
import com.quantlab.backend.strategy.StrategyRegistry;
import com.quantlab.backend.strategy.StrategyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for running screening operations.
 * <p>
 * Screening evaluates multiple strategies against all instruments for a specific date,
 * generating actionable signals (BUY/SELL) that traders can act upon.
 * Unlike backtesting, screening is point-in-time analysis focused on the latest signals.
 * </p>
 * <p>
 * Key differences from StrategyRunService:
 * <ul>
 *   <li>Screening evaluates the latest/most recent data point</li>
 *   <li>No paper trading or analytics generation</li>
 *   <li>Results are stored in screening_results table, not trade_signal</li>
 *   <li>Multiple strategies can be screened in a single request</li>
 *   <li>Results are grouped by strategy for easy comparison</li>
 * </ul>
 * </p>
 */
@Service
@Transactional
public class ScreeningService {

    private static final Logger log = LoggerFactory.getLogger(ScreeningService.class);

    private final ScreeningResultsRepository screeningResultsRepository;
    private final StrategyRepository strategyRepository;
    private final InstrumentRepository instrumentRepository;
    private final CandleRepository candleRepository;
    private final StrategyRegistry strategyRegistry;

    public ScreeningService(
            ScreeningResultsRepository screeningResultsRepository,
            StrategyRepository strategyRepository,
            InstrumentRepository instrumentRepository,
            CandleRepository candleRepository,
            StrategyRegistry strategyRegistry) {
        this.screeningResultsRepository = screeningResultsRepository;
        this.strategyRepository = strategyRepository;
        this.instrumentRepository = instrumentRepository;
        this.candleRepository = candleRepository;
        this.strategyRegistry = strategyRegistry;
    }

    /**
     * Run screening for the specified strategies on the given date.
     * <p>
     * This method:
     * 1. Validates the request (strategies exist, date has data)
     * 2. For each strategy, evaluates all active instruments
     * 3. Collects actionable signals (isActionable = true)
     * 4. Saves results to screening_results table
     * 5. Returns signals grouped by strategy
     * </p>
     *
     * @param request the screening request with strategy codes and date
     * @return screening response with signals grouped by strategy
     * @throws IllegalArgumentException if request is invalid
     */
    public ScreeningResponse runScreening(ScreeningRequest request) {
        log.info("Running screening for strategies: {} on date: {}",
                request.getStrategyCodes(), request.getDate());

        // Validate request
        validateScreeningRequest(request);

        // Parse market if provided
        MarketType market = parseMarket(request.getMarket());

        // Get instruments to screen
        List<Instrument> instruments = getInstrumentsForScreening(market);
        log.info("Screening {} instruments", instruments.size());

        // Map to store signals by strategy
        Map<String, List<ScreeningSignal>> signalsByStrategy = new HashMap<>();
        int totalSignals = 0;

        // Process each strategy
        for (String strategyCode : request.getStrategyCodes()) {
            log.debug("Processing strategy: {}", strategyCode);

            // Validate strategy exists
            if (!strategyRepository.existsByCode(strategyCode)) {
                log.warn("Strategy not found: {}, skipping", strategyCode);
                continue;
            }

            // Get strategy implementation
            Strategy strategyImpl = strategyRegistry.getStrategy(strategyCode.toLowerCase());

            // Screen this strategy
            List<ScreeningSignal> strategySignals = screenStrategy(
                    strategyImpl,
                    strategyCode,
                    instruments,
                    request.getDate()
            );

            if (!strategySignals.isEmpty()) {
                signalsByStrategy.put(strategyCode, strategySignals);
                totalSignals += strategySignals.size();

                // Save to database
                saveScreeningResults(strategyCode, request.getDate(), strategySignals);
            }

            log.info("Strategy {} generated {} actionable signals",
                    strategyCode, strategySignals.size());
        }

        // Build response
        ScreeningResponse response = new ScreeningResponse();
        response.setScreeningDate(request.getDate());
        response.setMarket(request.getMarket());
        response.setSignalsByStrategy(signalsByStrategy);
        response.setTotalSignals(totalSignals);
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        log.info("Screening completed. Total signals: {}", totalSignals);
        return response;
    }

    /**
     * Validate the screening request.
     */
    private void validateScreeningRequest(ScreeningRequest request) {
        if (request.getStrategyCodes() == null || request.getStrategyCodes().isEmpty()) {
            throw new IllegalArgumentException("At least one strategy code must be provided");
        }

        if (request.getDate() == null) {
            throw new IllegalArgumentException("Screening date must be provided");
        }

        // Validate date is not in the future
        if (request.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Screening date cannot be in the future");
        }

        // Validate market if provided
        if (request.getMarket() != null) {
            try {
                MarketType.valueOf(request.getMarket().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid market type: " + request.getMarket());
            }
        }
    }

    /**
     * Parse market type from string.
     */
    private MarketType parseMarket(String marketStr) {
        if (marketStr == null || marketStr.trim().isEmpty()) {
            return null; // Screen all markets
        }
        return MarketType.valueOf(marketStr.toUpperCase());
    }

    /**
     * Get instruments to screen based on market filter.
     */
    private List<Instrument> getInstrumentsForScreening(MarketType market) {
        List<Instrument> instruments;
        if (market != null) {
            instruments = instrumentRepository.findByMarketAndActiveOrderBySymbolAsc(market, true);
        } else {
            instruments = instrumentRepository.findByActiveOrderBySymbolAsc(true);
        }
        return instruments;
    }

    /**
     * Screen a single strategy across all instruments.
     * Returns actionable signals only (BUY/SELL).
     */
    private List<ScreeningSignal> screenStrategy(
            Strategy strategyImpl,
            String strategyCode,
            List<Instrument> instruments,
            LocalDate screenDate) {

        List<ScreeningSignal> actionableSignals = new ArrayList<>();

        for (Instrument instrument : instruments) {
            try {
                // Load historical candles up to the screen date
                // Need sufficient history for strategy indicators
                LocalDate startDate = calculateHistoricalStartDate(strategyImpl.getMinCandlesRequired(), screenDate);

                List<com.quantlab.backend.entity.Candle> candles = candleRepository
                        .findByInstrumentIdAndTradeDateBetweenOrderByTradeDateAsc(
                                instrument.getId(), startDate, screenDate);

                if (candles.isEmpty()) {
                    log.debug("No candles found for {} up to {}", instrument.getSymbol(), screenDate);
                    continue;
                }

                if (candles.size() < strategyImpl.getMinCandlesRequired()) {
                    log.debug("Insufficient candles for {}: {} (required: {})",
                            instrument.getSymbol(), candles.size(), strategyImpl.getMinCandlesRequired());
                    continue;
                }

                // Generate signals using the strategy
                StrategyParams params = new StrategyParams(new HashMap<>()); // Use default params
                List<TradeSignal> signals = strategyImpl.generateSignals(candles, params);

                // Filter for actionable signals on the screen date
                for (TradeSignal signal : signals) {
                    // Only include signals that match the screen date
                    if (signal.getSignalDate().equals(screenDate)) {
                        // Check if signal is actionable (BUY or SELL)
                        if (isActionableSignal(signal)) {
                            ScreeningSignal screeningSignal = convertToScreeningSignal(
                                    signal, strategyCode);
                            actionableSignals.add(screeningSignal);
                        }
                    }
                }

            } catch (Exception e) {
                log.error("Error screening instrument {} with strategy {}: {}",
                        instrument.getSymbol(), strategyCode, e.getMessage(), e);
            }
        }

        return actionableSignals;
    }

    /**
     * Calculate the start date for loading historical candles.
     * Adds buffer to ensure we have enough data for indicators.
     */
    private LocalDate calculateHistoricalStartDate(int minCandlesRequired, LocalDate endDate) {
        // Add 50% buffer for indicators that need more data
        int daysNeeded = (int) (minCandlesRequired * 1.5);
        return endDate.minusDays(daysNeeded);
    }

    /**
     * Check if a signal is actionable (BUY or SELL).
     * HOLD signals are not considered actionable for screening.
     */
    private boolean isActionableSignal(TradeSignal signal) {
        return signal.getSide() == Side.BUY || signal.getSide() == Side.SELL;
    }

    /**
     * Convert domain TradeSignal to ScreeningSignal DTO.
     */
    private ScreeningSignal convertToScreeningSignal(TradeSignal signal, String strategyCode) {
        return new ScreeningSignal(
                signal.getSymbol(),
                signal.getSignalDate(),
                signal.getSide().name(),
                signal.getEntryPrice(),
                signal.getStopLoss(),
                signal.getTargetPrice(),
                strategyCode
        );
    }

    /**
     * Save screening results to the database.
     */
    private void saveScreeningResults(String strategyCode, LocalDate runDate,
                                     List<ScreeningSignal> signals) {
        for (ScreeningSignal signal : signals) {
            ScreeningResult result = new ScreeningResult();
            result.setRunDate(runDate);
            result.setSymbol(signal.getSymbol());
            result.setStrategyCode(strategyCode);
            result.setSignalType(SignalType.valueOf(signal.getSide()));
            result.setEntry(signal.getEntryPrice());
            result.setStopLoss(signal.getStopLoss());
            result.setTarget(signal.getTarget());
            result.setCreatedAt(LocalDateTime.now());

            screeningResultsRepository.save(result);
        }
        log.debug("Saved {} screening results for strategy {}", signals.size(), strategyCode);
    }

    /**
     * Get historical screening results for a specific date.
     * Useful for viewing past screening results without re-running.
     *
     * @param date the screening date
     * @return screening response with historical signals
     */
    @Transactional(readOnly = true)
    public ScreeningResponse getHistoricalScreening(LocalDate date) {
        log.info("Fetching historical screening results for date: {}", date);

        List<ScreeningResult> results = screeningResultsRepository
                .findByRunDateOrderBySymbolAscStrategyCodeAsc(date);

        if (results.isEmpty()) {
            log.info("No screening results found for date: {}", date);
            return createEmptyResponse(date);
        }

        // Group by strategy
        Map<String, List<ScreeningSignal>> signalsByStrategy = results.stream()
                .filter(r -> r.getSignalType() == SignalType.BUY ||
                           r.getSignalType() == SignalType.SELL)
                .collect(Collectors.groupingBy(
                        ScreeningResult::getStrategyCode,
                        Collectors.mapping(this::convertEntityToDto, Collectors.toList())
                ));

        // Determine market from results
        String market = null;
        if (!results.isEmpty()) {
            Instrument firstInstrument = instrumentRepository.findBySymbol(results.get(0).getSymbol());
            if (firstInstrument != null) {
                market = firstInstrument.getMarket().name();
            }
        }

        ScreeningResponse response = new ScreeningResponse();
        response.setScreeningDate(date);
        response.setMarket(market);
        response.setSignalsByStrategy(signalsByStrategy);
        response.setTotalSignals(signalsByStrategy.values().stream().mapToInt(List::size).sum());
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        return response;
    }

    /**
     * Convert ScreeningResult entity to ScreeningSignal DTO.
     */
    private ScreeningSignal convertEntityToDto(ScreeningResult entity) {
        return new ScreeningSignal(
                entity.getSymbol(),
                entity.getRunDate(),
                entity.getSignalType().name(),
                entity.getEntry(),
                entity.getStopLoss(),
                entity.getTarget(),
                entity.getStrategyCode()
        );
    }

    /**
     * Create an empty screening response when no results are found.
     */
    private ScreeningResponse createEmptyResponse(LocalDate date) {
        ScreeningResponse response = new ScreeningResponse();
        response.setScreeningDate(date);
        response.setMarket(null);
        response.setSignalsByStrategy(new HashMap<>());
        response.setTotalSignals(0);
        response.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return response;
    }

    /**
     * Get available screening dates.
     * Useful for populating date filters in the UI.
     *
     * @return list of dates with screening results, in descending order
     */
    @Transactional(readOnly = true)
    public List<LocalDate> getAvailableScreeningDates() {
        return screeningResultsRepository.findDistinctRunDatesOrderByRunDateDesc();
    }

    /**
     * Get the most recent screening date.
     *
     * @return the most recent screening date, or null if no screenings exist
     */
    @Transactional(readOnly = true)
    public LocalDate getMostRecentScreeningDate() {
        return screeningResultsRepository.findMaxRunDate();
    }
}
