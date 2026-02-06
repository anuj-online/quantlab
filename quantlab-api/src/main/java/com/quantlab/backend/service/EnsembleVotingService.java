package com.quantlab.backend.service;

import com.quantlab.backend.dto.*;
import com.quantlab.backend.entity.*;
import com.quantlab.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EnsembleVotingService {

    private static final int MIN_STRATEGIES = 2;
    private static final int MIN_BUY_VOTES = 2;
    private static final boolean ALLOW_NEUTRAL = true;

    private static final Map<String, Double> STRATEGY_WEIGHTS = Map.of(
        "EMA_BREAKOUT", 1.0,
        "SMA_CROSSOVER", 1.2,
        "GAP_UP_MOMENTUM", 1.5,
        "REL_STRENGTH_30D", 1.0,
        "EOD_BREAKOUT_VOL", 1.3,
        "RSI_MEAN_REVERSION", 1.1,
        "MORNING_STAR", 1.4,
        "ATR_BREAKOUT", 1.3,
        "RSI_DIVERGENCE", 1.2,
        "DOJI_REVERSAL", 1.0
    );

    private final TradeSignalRepository tradeSignalRepository;
    private final ScreeningService screeningService;
    private final InstrumentRepository instrumentRepository;
    private final CandleRepository candleRepository;

    // Ranking weights (same as SignalRankingService)
    private static final double CONFIDENCE_WEIGHT = 0.35;
    private static final double R_MULTIPLE_WEIGHT = 0.25;
    private static final double LIQUIDITY_WEIGHT = 0.15;
    private static final double VOLATILITY_WEIGHT = 0.10;
    private static final double BASE_STRATEGY_WIN_RATE = 0.50; // Default win rate

    // Liquidity thresholds
    private static final long LIQUIDITY_THRESHOLD_LOW = 1_000_000L;
    private static final long LIQUIDITY_THRESHOLD_HIGH = 50_000_000L;

    @Autowired
    public EnsembleVotingService(TradeSignalRepository tradeSignalRepository,
                                  ScreeningService screeningService,
                                  InstrumentRepository instrumentRepository,
                                  CandleRepository candleRepository) {
        this.tradeSignalRepository = tradeSignalRepository;
        this.screeningService = screeningService;
        this.instrumentRepository = instrumentRepository;
        this.candleRepository = candleRepository;
    }

    /**
     * Processes screening results and generates ensemble signals
     */
    @Transactional
    public EnsembleResult generateEnsembleSignals(EnsembleRequest request) {
        Map<String, List<ScreeningSignal>> signalsByStrategy = new HashMap<>();

        // Get signals from each strategy
        for (String strategyCode : request.getStrategyCodes()) {
            ScreeningRequest screeningRequest = new ScreeningRequest(
                List.of(strategyCode),
                request.getDate(),
                request.getMarket()
            );
            ScreeningResponse response = screeningService.runScreening(screeningRequest);
            signalsByStrategy.put(strategyCode, response.getSignalsByStrategy().getOrDefault(strategyCode, List.of()));
        }

        // Group signals by symbol and side
        Map<String, List<ScreeningSignal>> signalsBySymbol = groupBySymbol(signalsByStrategy);

        List<EnsembleSignal> ensembleSignals = new ArrayList<>();

        for (Map.Entry<String, List<ScreeningSignal>> entry : signalsBySymbol.entrySet()) {
            String symbol = entry.getKey();
            List<ScreeningSignal> symbolSignals = entry.getValue();

            if (symbolSignals.size() < MIN_STRATEGIES) {
                continue; // Skip symbols with insufficient strategy coverage
            }

            // Calculate votes and confidence
            VoteResult voteResult = calculateVotes(symbolSignals);

            if (voteResult.buyVotes >= MIN_BUY_VOTES) {
                // Create ensemble signal
                EnsembleSignal ensemble = createEnsembleSignal(
                    symbol,
                    symbolSignals,
                    voteResult,
                    request.getDate()
                );
                ensembleSignals.add(ensemble);
            }
        }

        return new EnsembleResult(ensembleSignals);
    }

    private Map<String, List<ScreeningSignal>> groupBySymbol(
        Map<String, List<ScreeningSignal>> signalsByStrategy) {
        return signalsByStrategy.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.groupingBy(ScreeningSignal::getSymbol));
    }

    private VoteResult calculateVotes(List<ScreeningSignal> signals) {
        int buyVotes = 0;
        double confidenceScore = 0.0;
        Map<String, String> strategyVotes = new HashMap<>();

        for (ScreeningSignal signal : signals) {
            String strategyCode = signal.getStrategyCode();
            String side = signal.getSide();
            double weight = STRATEGY_WEIGHTS.getOrDefault(strategyCode, 1.0);

            strategyVotes.put(strategyCode, side);

            if (side.equals("BUY")) {
                buyVotes++;
                confidenceScore += weight;
            }
        }

        return new VoteResult(buyVotes, signals.size(), confidenceScore, strategyVotes);
    }

    private EnsembleSignal createEnsembleSignal(
        String symbol,
        List<ScreeningSignal> signals,
        VoteResult voteResult,
        LocalDate screeningDate
    ) {
        // Use average entry, stop loss, and target from voting strategies
        double avgEntry = signals.stream()
            .mapToDouble(s -> s.getEntryPrice().doubleValue())
            .average()
            .orElse(0.0);

        double avgStopLoss = signals.stream()
            .filter(s -> s.getStopLoss() != null)
            .mapToDouble(s -> s.getStopLoss().doubleValue())
            .average()
            .orElse(0.0);

        double avgTarget = signals.stream()
            .filter(s -> s.getTarget() != null)
            .mapToDouble(s -> s.getTarget().doubleValue())
            .average()
            .orElse(0.0);

        // Generate ensemble ID
        java.util.UUID ensembleId = java.util.UUID.randomUUID();

        // Calculate ranking metrics
        Double rMultiple = calculateRMultiple(avgEntry, avgStopLoss, avgTarget);
        Double liquidityScore = calculateLiquidityScore(symbol, screeningDate);
        Double volatilityFit = calculateVolatilityFit(symbol, screeningDate, avgEntry, avgStopLoss);

        // Calculate composite rank score
        Double rankScore = (voteResult.confidenceScore * CONFIDENCE_WEIGHT)
            + (rMultiple * R_MULTIPLE_WEIGHT)
            + (liquidityScore * LIQUIDITY_WEIGHT)
            + (BASE_STRATEGY_WIN_RATE * 0.15) // Using default win rate
            + (volatilityFit * VOLATILITY_WEIGHT);

        EnsembleSignal ensemble = new EnsembleSignal(
            ensembleId,
            symbol,
            screeningDate,
            "BUY",
            BigDecimal.valueOf(avgEntry),
            avgStopLoss > 0 ? BigDecimal.valueOf(avgStopLoss) : null,
            avgTarget > 0 ? BigDecimal.valueOf(avgTarget) : null,
            voteResult.buyVotes,
            voteResult.totalStrategies,
            voteResult.confidenceScore,
            voteResult.strategyVotes
        );

        // Set ranking fields
        ensemble.setRankScore(rankScore);
        ensemble.setRMultiple(rMultiple);
        ensemble.setLiquidityScore(liquidityScore);
        ensemble.setVolatilityFit(volatilityFit);

        return ensemble;
    }

    /**
     * Calculate R-multiple for the ensemble signal.
     */
    private Double calculateRMultiple(double entry, double stopLoss, double target) {
        if (stopLoss <= 0 || target <= 0) {
            return 0.0;
        }

        double risk = entry - stopLoss;
        double reward = target - entry;

        if (risk <= 0) return 0.0;

        return reward / risk;
    }

    /**
     * Calculate liquidity score based on recent trading volume.
     */
    private Double calculateLiquidityScore(String symbol, LocalDate date) {
        // Find instrument by symbol
        Instrument instrument = instrumentRepository.findAll().stream()
            .filter(i -> i.getSymbol().equals(symbol))
            .findFirst()
            .orElse(null);

        if (instrument == null) {
            return 0.5; // Default if instrument not found
        }

        // Fetch recent volume data (last 20 trading days)
        List<Candle> recentCandles = candleRepository.findCandlesUpToDate(
            instrument.getId(),
            date,
            PageRequest.of(0, 20)
        );

        if (recentCandles.isEmpty()) {
            return 0.5;
        }

        // Calculate average daily volume
        double avgVolume = recentCandles.stream()
            .mapToLong(Candle::getVolume)
            .average()
            .orElse(0.0);

        // Normalize to 0-1 scale
        if (avgVolume <= LIQUIDITY_THRESHOLD_LOW) {
            return 0.3 + (avgVolume / LIQUIDITY_THRESHOLD_LOW) * 0.3;
        } else if (avgVolume <= LIQUIDITY_THRESHOLD_HIGH) {
            double ratio = (avgVolume - LIQUIDITY_THRESHOLD_LOW) / (LIQUIDITY_THRESHOLD_HIGH - LIQUIDITY_THRESHOLD_LOW);
            return 0.6 + ratio * 0.3;
        } else {
            return 0.9 + Math.min(0.1, (avgVolume - LIQUIDITY_THRESHOLD_HIGH) / LIQUIDITY_THRESHOLD_HIGH * 0.1);
        }
    }

    /**
     * Calculate volatility fit based on ATR vs stop loss distance.
     */
    private Double calculateVolatilityFit(String symbol, LocalDate date, double entry, double stopLoss) {
        // Find instrument by symbol
        Instrument instrument = instrumentRepository.findAll().stream()
            .filter(i -> i.getSymbol().equals(symbol))
            .findFirst()
            .orElse(null);

        if (instrument == null || stopLoss <= 0) {
            return 0.5;
        }

        // Calculate ATR for recent period (14 days)
        List<Candle> recentCandles = candleRepository.findCandlesUpToDate(
            instrument.getId(),
            date,
            PageRequest.of(0, 15)
        );

        if (recentCandles.isEmpty()) {
            return 0.5;
        }

        double atr = calculateATR(recentCandles, 14);
        if (atr <= 0) {
            return 0.5;
        }

        // Calculate stop loss distance as percentage of entry price
        double slDistancePct = Math.abs(entry - stopLoss) / entry * 100;
        double atrPct = (atr / entry) * 100;

        // Score based on how well SL aligns with volatility
        double slToAtrRatio = slDistancePct / atrPct;

        // Score curve (same as SignalRankingService)
        if (slToAtrRatio < 1.0) {
            return 0.2 + (slToAtrRatio * 0.3);
        } else if (slToAtrRatio <= 1.5) {
            return 0.5 + ((slToAtrRatio - 1.0) * 0.4);
        } else if (slToAtrRatio <= 2.5) {
            return 0.7 + ((slToAtrRatio - 1.5) * 0.3);
        } else if (slToAtrRatio <= 4.0) {
            return 1.0 - ((slToAtrRatio - 2.5) * 0.16);
        } else {
            return Math.max(0.3, 0.6 - ((slToAtrRatio - 4.0) * 0.1));
        }
    }

    /**
     * Calculate ATR for a list of candles.
     */
    private double calculateATR(List<Candle> candles, int period) {
        if (candles.size() < 2) {
            return 0.0;
        }

        double sumTR = 0.0;
        int count = 0;
        int actualPeriod = Math.min(period, candles.size() - 1);

        for (int i = 0; i < actualPeriod; i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i + 1);

            double high = current.getHigh().doubleValue();
            double low = current.getLow().doubleValue();
            double prevClose = previous.getClose().doubleValue();

            double tr = Math.max(
                high - low,
                Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose))
            );

            sumTR += tr;
            count++;
        }

        return count > 0 ? sumTR / count : 0.0;
    }
}