package com.quantlab.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantlab.backend.dto.PaperTradeResponse;
import com.quantlab.backend.dto.RunStrategyRequest;
import com.quantlab.backend.dto.RunStrategyResponse;
import com.quantlab.backend.dto.TradeSignalResponse;
import com.quantlab.backend.entity.*;
import com.quantlab.backend.mapper.PaperTradeMapper;
import com.quantlab.backend.mapper.TradeSignalMapper;
import com.quantlab.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for running strategies and managing strategy runs.
 * Strategy runs are immutable - re-running = new strategy_run.
 */
@Service
@Transactional
public class StrategyRunService {

    private static final Logger log = LoggerFactory.getLogger(StrategyRunService.class);

    private final StrategyRunRepository strategyRunRepository;
    private final StrategyRepository strategyRepository;
    private final TradeSignalRepository tradeSignalRepository;
    private final PaperTradeRepository paperTradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;

    public StrategyRunService(
            StrategyRunRepository strategyRunRepository,
            StrategyRepository strategyRepository,
            TradeSignalRepository tradeSignalRepository,
            PaperTradeRepository paperTradeRepository,
            InstrumentRepository instrumentRepository,
            ObjectMapper objectMapper) {
        this.strategyRunRepository = strategyRunRepository;
        this.strategyRepository = strategyRepository;
        this.tradeSignalRepository = tradeSignalRepository;
        this.paperTradeRepository = paperTradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Run a strategy with the given parameters.
     * For the initial implementation, this creates a StrategyRun record.
     * The actual backtesting logic can be implemented later.
     *
     * @param request the run strategy request
     * @return the run strategy response with the strategy run ID and status
     */
    public RunStrategyResponse runStrategy(RunStrategyRequest request) {
        log.info("Running strategy: {} for market: {} from {} to {}",
                request.getStrategyCode(), request.getMarket(),
                request.getStartDate(), request.getEndDate());

        // Find the strategy by code
        Strategy strategy = strategyRepository.findByCode(request.getStrategyCode())
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

        // Convert params to JSON
        String paramsJson;
        try {
            paramsJson = objectMapper.writeValueAsString(request.getParams());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize strategy parameters", e);
        }

        // Create the strategy run
        StrategyRun strategyRun = new StrategyRun();
        strategyRun.setStrategy(strategy);
        strategyRun.setMarket(market);
        strategyRun.setParamsJson(paramsJson);
        strategyRun.setStartDate(startDate);
        strategyRun.setEndDate(endDate);
        strategyRun.setRunTimestamp(LocalDate.now());

        strategyRun = strategyRunRepository.save(strategyRun);

        log.info("Created strategy run with ID: {}", strategyRun.getId());

        // TODO: Implement actual backtesting logic
        // This would involve:
        // 1. Loading historical candle data for the market and date range
        // 2. Running the strategy algorithm on the data
        // 3. Generating trade signals
        // 4. Simulating paper trades
        // 5. Saving signals and trades to the database

        return new RunStrategyResponse(strategyRun.getId(), "COMPLETED");
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
