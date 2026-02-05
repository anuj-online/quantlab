package com.quantlab.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantlab.backend.dto.AnalyticsResponse;
import com.quantlab.backend.dto.EquityCurvePoint;
import com.quantlab.backend.dto.PaperTradeResponse;
import com.quantlab.backend.dto.RunStrategyRequest;
import com.quantlab.backend.dto.RunStrategyResponse;
import com.quantlab.backend.dto.TradeSignalResponse;
import com.quantlab.backend.entity.*;
import com.quantlab.backend.mapper.PaperTradeMapper;
import com.quantlab.backend.mapper.TradeSignalMapper;
import com.quantlab.backend.repository.*;
import com.quantlab.backend.strategy.AnalyticsEngine;
import com.quantlab.backend.strategy.PaperTradingEngine;
import com.quantlab.backend.strategy.Strategy;
import com.quantlab.backend.strategy.StrategyParams;
import com.quantlab.backend.strategy.StrategyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for running strategies and managing strategy runs.
 * Strategy runs are immutable - re-running = new strategy_run.
 */
@Service
@Transactional
public class StrategyRunService {

    private static final Logger log = LoggerFactory.getLogger(StrategyRunService.class);
    private static final BigDecimal DEFAULT_INITIAL_CAPITAL = new BigDecimal("100000");
    private static final double DEFAULT_RISK_PER_TRADE = 0.01; // 1%

    private final StrategyRunRepository strategyRunRepository;
    private final StrategyRepository strategyRepository;
    private final TradeSignalRepository tradeSignalRepository;
    private final PaperTradeRepository paperTradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CandleRepository candleRepository;
    private final ObjectMapper objectMapper;
    private final StrategyRegistry strategyRegistry;
    private final PaperTradingEngine paperTradingEngine;
    private final AnalyticsEngine analyticsEngine;

    public StrategyRunService(
            StrategyRunRepository strategyRunRepository,
            StrategyRepository strategyRepository,
            TradeSignalRepository tradeSignalRepository,
            PaperTradeRepository paperTradeRepository,
            InstrumentRepository instrumentRepository,
            CandleRepository candleRepository,
            ObjectMapper objectMapper,
            StrategyRegistry strategyRegistry,
            PaperTradingEngine paperTradingEngine,
            AnalyticsEngine analyticsEngine) {
        this.strategyRunRepository = strategyRunRepository;
        this.strategyRepository = strategyRepository;
        this.tradeSignalRepository = tradeSignalRepository;
        this.paperTradeRepository = paperTradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.candleRepository = candleRepository;
        this.objectMapper = objectMapper;
        this.strategyRegistry = strategyRegistry;
        this.paperTradingEngine = paperTradingEngine;
        this.analyticsEngine = analyticsEngine;
    }

    /**
     * Run a strategy with the given parameters.
     * This orchestrates the full backtesting pipeline:
     * 1. Load candles for the market and date range
     * 2. Generate signals using the strategy implementation
     * 3. Execute paper trades
     * 4. Calculate analytics
     *
     * @param request the run strategy request
     * @return the run strategy response with the strategy run ID and status
     */
    public RunStrategyResponse runStrategy(RunStrategyRequest request) {
        log.info("Running strategy: {} for market: {} from {} to {}",
                request.getStrategyCode(), request.getMarket(),
                request.getStartDate(), request.getEndDate());

        // Find the strategy entity by code
        com.quantlab.backend.entity.Strategy strategyEntity = strategyRepository.findByCode(request.getStrategyCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Strategy not found with code: " + request.getStrategyCode()));

        // Parse the market
        MarketType market;
        try {
            market = MarketType.valueOf(request.getMarket().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid market type: " + request.getMarket());
        }

        // Parse dates
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        // Convert params to JSON for storage
        String paramsJson;
        try {
            paramsJson = objectMapper.writeValueAsString(request.getParams());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize strategy parameters", e);
        }

        // Create the strategy run record
        StrategyRun strategyRun = new StrategyRun();
        strategyRun.setStrategy(strategyEntity);
        strategyRun.setMarket(market);
        strategyRun.setParamsJson(paramsJson);
        strategyRun.setStartDate(startDate);
        strategyRun.setEndDate(endDate);
        strategyRun.setRunTimestamp(LocalDate.now());

        strategyRun = strategyRunRepository.save(strategyRun);
        Long runId = strategyRun.getId();
        log.info("Created strategy run with ID: {}", runId);

        // Get all active instruments for the market
        List<Instrument> instruments = instrumentRepository.findByMarketAndActiveOrderBySymbolAsc(market, true);
        log.info("Found {} active instruments for market: {}", instruments.size(), market);

        // Get strategy implementation from registry (registry lowercases codes)
        Strategy strategyImpl = strategyRegistry.getStrategy(request.getStrategyCode().toLowerCase());

        // Get risk per trade parameter
        Map<String, Object> params = request.getParams() != null ? request.getParams() : new HashMap<>();
        double riskPerTrade = extractDoubleParam(params, "riskPerTrade", DEFAULT_RISK_PER_TRADE);

        // Generate signals for each instrument
        List<com.quantlab.backend.domain.TradeSignal> allDomainSignals = new java.util.ArrayList<>();
        for (Instrument instrument : instruments) {
            // Load candles for this instrument in date range
            List<Candle> candles = candleRepository.findByInstrumentIdAndTradeDateBetweenOrderByTradeDateAsc(
                    instrument.getId(), startDate, endDate);

            if (candles.isEmpty()) {
                log.debug("No candles found for instrument: {} in date range", instrument.getSymbol());
                continue;
            }

            log.debug("Loaded {} candles for instrument: {}", candles.size(), instrument.getSymbol());

            // Generate signals using the strategy
            StrategyParams strategyParams = new StrategyParams(params);
            List<com.quantlab.backend.domain.TradeSignal> signals = strategyImpl.generateSignals(candles, strategyParams);

            log.debug("Generated {} signals for instrument: {}", signals.size(), instrument.getSymbol());
            allDomainSignals.addAll(signals);
        }

        log.info("Total signals generated: {}", allDomainSignals.size());

        // Convert domain signals to entity signals and persist
        List<TradeSignal> entitySignals = new java.util.ArrayList<>();
        for (com.quantlab.backend.domain.TradeSignal domainSignal : allDomainSignals) {
            TradeSignal entitySignal = convertToEntitySignal(domainSignal, strategyRun);

            // Calculate position size based on risk per trade
            int quantity = calculatePositionSize(
                    domainSignal.getEntryPrice(),
                    domainSignal.getStopLoss(),
                    DEFAULT_INITIAL_CAPITAL,
                    riskPerTrade
            );
            entitySignal.setQuantity(quantity);

            entitySignal = tradeSignalRepository.save(entitySignal);
            entitySignals.add(entitySignal);
        }

        log.info("Persisted {} trade signals", entitySignals.size());

        // Load all candles for paper trading (need full history for all instruments)
        List<Candle> allCandles = new java.util.ArrayList<>();
        for (Instrument instrument : instruments) {
            List<Candle> instrumentCandles = candleRepository.findByInstrumentIdAndTradeDateBetweenOrderByTradeDateAsc(
                    instrument.getId(), startDate, endDate);
            allCandles.addAll(instrumentCandles);
        }

        // Execute paper trades
        List<PaperTrade> paperTrades = paperTradingEngine.executePaperTrades(entitySignals, allCandles, params);

        log.info("Executed {} paper trades", paperTrades.size());

        // Calculate analytics
        AnalyticsResponse analytics = analyticsEngine.calculate(paperTrades, DEFAULT_INITIAL_CAPITAL);

        log.info("Strategy run completed. Total trades: {}, Win rate: {}, Total P&L: {}",
                analytics.getTotalTrades(), analytics.getWinRate(), analytics.getTotalPnl());

        return new RunStrategyResponse(runId, "COMPLETED");
    }

    /**
     * Convert domain TradeSignal to entity TradeSignal for persistence.
     */
    private TradeSignal convertToEntitySignal(com.quantlab.backend.domain.TradeSignal domainSignal, StrategyRun strategyRun) {
        TradeSignal entity = new TradeSignal();
        entity.setStrategyRun(strategyRun);
        entity.setInstrument(domainSignal.getInstrument());
        entity.setSignalDate(domainSignal.getSignalDate());
        entity.setSide(domainSignal.getSide());
        entity.setEntryPrice(domainSignal.getEntryPrice());
        entity.setStopLoss(domainSignal.getStopLoss());
        entity.setTargetPrice(domainSignal.getTargetPrice());
        entity.setQuantity(domainSignal.getQuantity());
        return entity;
    }

    /**
     * Calculate position size based on risk per trade.
     * quantity = (capital * riskPerTrade) / (entryPrice - stopLoss)
     * If stopLoss is null, defaults to quantity = 1
     */
    private int calculatePositionSize(BigDecimal entryPrice, BigDecimal stopLoss,
                                       BigDecimal capital, double riskPerTrade) {
        // If no stop loss is set, use default quantity
        if (stopLoss == null) {
            return 1;
        }

        BigDecimal riskPerUnit = entryPrice.subtract(stopLoss);
        if (riskPerUnit.compareTo(BigDecimal.ZERO) <= 0) {
            return 1; // Default to 1 if risk is zero or negative
        }

        BigDecimal riskAmount = capital.multiply(BigDecimal.valueOf(riskPerTrade));
        BigDecimal quantity = riskAmount.divide(riskPerUnit, 0, BigDecimal.ROUND_DOWN);

        // Ensure minimum quantity of 1
        return Math.max(1, quantity.intValue());
    }

    /**
     * Extract a double parameter from the params map.
     */
    private double extractDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get all trade signals for a specific strategy run.
     *
     * @param runId the strategy run ID
     * @return list of trade signal response DTOs
     */
    @Transactional(readOnly = true)
    public List<TradeSignalResponse> getSignals(Long runId) {
        List<TradeSignal> signals = tradeSignalRepository.findByStrategyRunIdOrderBySignalDateAsc(runId);
        return signals.stream()
                .map(TradeSignalMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all trade signals for a specific strategy run.
     * Alias for getSignals() to match controller naming.
     *
     * @param runId the strategy run ID
     * @return list of trade signal response DTOs
     */
    @Transactional(readOnly = true)
    public List<TradeSignalResponse> getTradeSignals(Long runId) {
        return getSignals(runId);
    }

    /**
     * Get all paper trades for a specific strategy run.
     *
     * @param runId the strategy run ID
     * @return list of paper trade response DTOs
     */
    @Transactional(readOnly = true)
    public List<PaperTradeResponse> getPaperTrades(Long runId) {
        List<PaperTrade> paperTrades = paperTradeRepository.findByStrategyRunIdOrderByEntryDateAsc(runId);
        return paperTrades.stream()
                .map(PaperTradeMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find a strategy run by ID.
     *
     * @param runId the strategy run ID
     * @return the strategy run if found
     * @throws jakarta.persistence.EntityNotFoundException if strategy run not found
     */
    @Transactional(readOnly = true)
    public StrategyRun getStrategyRunById(Long runId) {
        return strategyRunRepository.findById(runId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Strategy run not found with ID: " + runId));
    }
}
