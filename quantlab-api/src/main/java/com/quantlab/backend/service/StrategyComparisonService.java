package com.quantlab.backend.service;

import com.quantlab.backend.dto.*;
import com.quantlab.backend.entity.MarketType;
import com.quantlab.backend.entity.Strategy;
import com.quantlab.backend.entity.StrategyRun;
import com.quantlab.backend.mapper.PaperTradeMapper;
import com.quantlab.backend.repository.PaperTradeRepository;
import com.quantlab.backend.repository.StrategyRepository;
import com.quantlab.backend.repository.StrategyRunRepository;
import com.quantlab.backend.strategy.AnalyticsEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for comparing multiple strategies and calculating performance metrics.
 * <p>
 * This service handles:
 * - Comparing multiple strategies side-by-side
 * - Calculating individual strategy metrics
 * - Identifying best performing strategies for each metric
 */
@Service
@Transactional
public class StrategyComparisonService {

    private static final Logger log = LoggerFactory.getLogger(StrategyComparisonService.class);
    private static final BigDecimal DEFAULT_INITIAL_CAPITAL = new BigDecimal("100000");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final StrategyRepository strategyRepository;
    private final StrategyRunRepository strategyRunRepository;
    private final PaperTradeRepository paperTradeRepository;
    private final StrategyRunService strategyRunService;
    private final AnalyticsEngine analyticsEngine;

    public StrategyComparisonService(
            StrategyRepository strategyRepository,
            StrategyRunRepository strategyRunRepository,
            PaperTradeRepository paperTradeRepository,
            StrategyRunService strategyRunService,
            AnalyticsEngine analyticsEngine) {
        this.strategyRepository = strategyRepository;
        this.strategyRunRepository = strategyRunRepository;
        this.paperTradeRepository = paperTradeRepository;
        this.strategyRunService = strategyRunService;
        this.analyticsEngine = analyticsEngine;
    }

    /**
     * Compare multiple strategies and return their performance metrics.
     * <p>
     * This method:
     * 1. Finds or creates strategy runs for each strategy
     * 2. Calculates metrics for each strategy
     * 3. Identifies the best performing strategy for each metric
     *
     * @param request the comparison request containing strategies, market, and date range
     * @return comparison response with metrics for each strategy
     */
    @Transactional(readOnly = true)
    public StrategyComparisonResponse compareStrategies(StrategyComparisonRequest request) {
        log.info("Comparing {} strategies for market: {} from {} to {}",
                request.getStrategyCodes().size(), request.getMarket(),
                request.getStartDate(), request.getEndDate());

        // Parse request parameters
        MarketType market = MarketType.valueOf(request.getMarket().toUpperCase());
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());

        // Validate date range
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        // Calculate metrics for each strategy
        List<StrategyMetrics> metricsList = new ArrayList<>();
        for (String strategyCode : request.getStrategyCodes()) {
            try {
                StrategyMetrics metrics = calculateStrategyMetrics(
                        strategyCode, market, startDate, endDate);
                if (metrics != null) {
                    metricsList.add(metrics);
                }
            } catch (Exception e) {
                log.warn("Failed to calculate metrics for strategy: {}", strategyCode, e);
            }
        }

        // Find best performing for each metric
        List<StrategyComparisonResponse.BestPerformingMetric> bestPerforming =
                findBestPerformingMetrics(metricsList);

        // Build response
        StrategyComparisonResponse response = new StrategyComparisonResponse();
        response.setComparisonDate(LocalDate.now().format(DATE_FORMATTER));
        response.setMarket(request.getMarket());
        response.setMetrics(metricsList);
        response.setBestPerforming(bestPerforming);

        log.info("Comparison completed. Metrics calculated for {} strategies", metricsList.size());
        return response;
    }

    /**
     * Get metrics for a single strategy.
     *
     * @param strategyCode the strategy code
     * @param market the market type
     * @param startDate the start date
     * @param endDate the end date
     * @return strategy metrics
     */
    @Transactional(readOnly = true)
    public StrategyMetrics getStrategyMetrics(String strategyCode, String market,
                                              String startDate, String endDate) {
        log.info("Getting metrics for strategy: {} in market: {} from {} to {}",
                strategyCode, market, startDate, endDate);

        MarketType marketType = MarketType.valueOf(market.toUpperCase());
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        return calculateStrategyMetrics(strategyCode, marketType, start, end);
    }

    /**
     * Calculate metrics for a single strategy.
     * <p>
     * This method attempts to find an existing strategy run for the given parameters.
     * If none exists, it returns null (could be extended to create a new run).
     *
     * @param strategyCode the strategy code
     * @param market the market type
     * @param startDate the start date
     * @param endDate the end date
     * @return strategy metrics or null if no run exists
     */
    private StrategyMetrics calculateStrategyMetrics(String strategyCode, MarketType market,
                                                     LocalDate startDate, LocalDate endDate) {
        // Find strategy entity
        Strategy strategy = strategyRepository.findByCode(strategyCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Strategy not found with code: " + strategyCode));

        // Try to find existing strategy run for these parameters
        Optional<StrategyRun> existingRun = strategyRunRepository
                .findByStrategy_CodeAndMarketAndStartDateAndEndDate(
                        strategyCode, market, startDate, endDate);

        if (existingRun.isEmpty()) {
            log.debug("No existing run found for strategy: {} with dates {} to {}",
                    strategyCode, startDate, endDate);
            // For now, return null if no run exists
            // Could be extended to create a new run on demand
            return createEmptyMetrics(strategyCode, strategy.getName());
        }

        StrategyRun run = existingRun.get();

        // Get paper trades for this run
        List<com.quantlab.backend.entity.PaperTrade> paperTrades =
                paperTradeRepository.findByStrategyRunIdOrderByEntryDateAsc(run.getId());

        if (paperTrades.isEmpty()) {
            log.debug("No paper trades found for strategy run: {}", run.getId());
            return createEmptyMetrics(strategyCode, strategy.getName());
        }

        // Calculate analytics (AnalyticsEngine expects entity PaperTrade, not DTO)
        AnalyticsResponse analytics = analyticsEngine.calculate(paperTrades, DEFAULT_INITIAL_CAPITAL);

        // Calculate additional metrics
        StrategyMetrics metrics = buildStrategyMetrics(
                strategyCode,
                strategy.getName(),
                paperTrades,
                analytics
        );

        log.debug("Calculated metrics for strategy: {} - trades: {}, winRate: {}%, totalPnL: {}",
                strategyCode, metrics.getTotalTrades(),
                metrics.getWinRate(), metrics.getTotalPnl());

        return metrics;
    }

    /**
     * Build StrategyMetrics from paper trades and analytics.
     */
    private StrategyMetrics buildStrategyMetrics(String strategyCode, String strategyName,
                                                List<com.quantlab.backend.entity.PaperTrade> paperTrades,
                                                AnalyticsResponse analytics) {
        // Calculate separate win/loss metrics
        List<com.quantlab.backend.entity.PaperTrade> winningTrades = paperTrades.stream()
                .filter(trade -> trade.getPnl() != null && trade.getPnl().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        List<com.quantlab.backend.entity.PaperTrade> losingTrades = paperTrades.stream()
                .filter(trade -> trade.getPnl() != null && trade.getPnl().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.toList());

        double avgWin = winningTrades.isEmpty() ? 0.0 :
                winningTrades.stream()
                        .mapToDouble(trade -> trade.getPnl().doubleValue())
                        .average()
                        .orElse(0.0);

        double avgLoss = losingTrades.isEmpty() ? 0.0 :
                losingTrades.stream()
                        .mapToDouble(trade -> trade.getPnl().doubleValue())
                        .average()
                        .orElse(0.0);

        // Calculate profit factor: gross profit / gross loss
        double grossProfit = winningTrades.stream()
                .mapToDouble(trade -> trade.getPnl().doubleValue())
                .sum();

        double grossLoss = Math.abs(losingTrades.stream()
                .mapToDouble(trade -> trade.getPnl().doubleValue())
                .sum());

        double profitFactor = grossLoss == 0 ? grossProfit > 0 ? Double.MAX_VALUE : 0.0 :
                grossProfit / grossLoss;

        // Calculate average return per trade
        double avgReturn = paperTrades.isEmpty() ? 0.0 :
                paperTrades.stream()
                        .mapToDouble(trade -> trade.getPnlPct() != null ?
                                trade.getPnlPct().doubleValue() : 0.0)
                        .average()
                        .orElse(0.0);

        // Convert analytics values to appropriate types
        double winRate = analytics.getWinRate() != null ?
                analytics.getWinRate().doubleValue() : 0.0;
        double totalPnl = analytics.getTotalPnl() != null ?
                analytics.getTotalPnl().doubleValue() : 0.0;
        double maxDrawdown = analytics.getMaxDrawdown() != null ?
                analytics.getMaxDrawdown().doubleValue() : 0.0;

        return new StrategyMetrics(
                strategyCode,
                strategyName,
                analytics.getTotalTrades(),
                winRate,
                avgReturn,
                totalPnl,
                maxDrawdown,
                null,  // Sharpe ratio - not currently calculated in AnalyticsEngine
                null,  // Sortino ratio - not currently calculated in AnalyticsEngine
                avgWin,
                avgLoss,
                profitFactor
        );
    }

    /**
     * Create empty metrics when no data is available.
     */
    private StrategyMetrics createEmptyMetrics(String strategyCode, String strategyName) {
        return new StrategyMetrics(
                strategyCode,
                strategyName,
                0,
                0.0,
                0.0,
                0.0,
                0.0,
                null,
                null,
                0.0,
                0.0,
                0.0
        );
    }

    /**
     * Find the best performing strategy for each metric.
     * <p>
     * For metrics like win rate, total PnL, Sharpe ratio: higher is better
     * For metrics like max drawdown: lower is better
     */
    private List<StrategyComparisonResponse.BestPerformingMetric> findBestPerformingMetrics(
            List<StrategyMetrics> metricsList) {

        if (metricsList.isEmpty()) {
            return Collections.emptyList();
        }

        List<StrategyComparisonResponse.BestPerformingMetric> bestPerforming = new ArrayList<>();

        // Best win rate
        metricsList.stream()
                .max(Comparator.comparing(StrategyMetrics::getWinRate))
                .ifPresent(m -> bestPerforming.add(
                        new StrategyComparisonResponse.BestPerformingMetric(
                                "winRate", m.getStrategyCode(), m.getWinRate())));

        // Best total PnL
        metricsList.stream()
                .max(Comparator.comparing(StrategyMetrics::getTotalPnl))
                .ifPresent(m -> bestPerforming.add(
                        new StrategyComparisonResponse.BestPerformingMetric(
                                "totalPnl", m.getStrategyCode(), m.getTotalPnl())));

        // Best Sharpe ratio
        metricsList.stream()
                .filter(m -> m.getSharpeRatio() != null)
                .max(Comparator.comparing(StrategyMetrics::getSharpeRatio))
                .ifPresent(m -> bestPerforming.add(
                        new StrategyComparisonResponse.BestPerformingMetric(
                                "sharpeRatio", m.getStrategyCode(), m.getSharpeRatio())));

        // Lowest max drawdown
        metricsList.stream()
                .min(Comparator.comparing(StrategyMetrics::getMaxDrawdown))
                .ifPresent(m -> bestPerforming.add(
                        new StrategyComparisonResponse.BestPerformingMetric(
                                "maxDrawdown", m.getStrategyCode(), m.getMaxDrawdown())));

        // Best profit factor
        metricsList.stream()
                .filter(m -> m.getProfitFactor() != null && !m.getProfitFactor().isInfinite())
                .max(Comparator.comparing(StrategyMetrics::getProfitFactor))
                .ifPresent(m -> bestPerforming.add(
                        new StrategyComparisonResponse.BestPerformingMetric(
                                "profitFactor", m.getStrategyCode(), m.getProfitFactor())));

        return bestPerforming;
    }
}
