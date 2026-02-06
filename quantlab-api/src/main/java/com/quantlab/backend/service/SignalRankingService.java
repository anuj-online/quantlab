package com.quantlab.backend.service;

import com.quantlab.backend.dto.*;
import com.quantlab.backend.entity.*;
import com.quantlab.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class SignalRankingService {

    private static final double CONFIDENCE_WEIGHT = 0.35;
    private static final double R_MULTIPLE_WEIGHT = 0.25;
    private static final double LIQUIDITY_WEIGHT = 0.15;
    private static final double WIN_RATE_WEIGHT = 0.15;
    private static final double VOLATILITY_WEIGHT = 0.10;

    private final TradeSignalRepository tradeSignalRepository;
    private final PaperTradeRepository paperTradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CandleRepository candleRepository;

    // Liquidity thresholds (average daily volume)
    private static final long LIQUIDITY_THRESHOLD_LOW = 1_000_000L;   // 10L shares
    private static final long LIQUIDITY_THRESHOLD_HIGH = 50_000_000L;  // 5Cr shares

    @Autowired
    public SignalRankingService(TradeSignalRepository tradeSignalRepository,
                               PaperTradeRepository paperTradeRepository,
                               InstrumentRepository instrumentRepository,
                               CandleRepository candleRepository) {
        this.tradeSignalRepository = tradeSignalRepository;
        this.paperTradeRepository = paperTradeRepository;
        this.instrumentRepository = instrumentRepository;
        this.candleRepository = candleRepository;
    }

    /**
     * Calculates and persists rank scores for all PENDING signals on a given date
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    @Transactional
    public void rankDailySignals() {
        LocalDate today = LocalDate.now();

        // Fetch all PENDING signals from today
        List<TradeSignal> pendingSignals = tradeSignalRepository
            .findByStatusAndSignalDate(TradeSignalStatus.PENDING, today);

        for (TradeSignal signal : pendingSignals) {
            double rankScore = calculateRankScore(signal);
            signal.setRankScore(rankScore);
            double rMultiple = calculateRMultiple(signal);
            signal.setRMultiple(rMultiple);
        }

        // Batch save
        tradeSignalRepository.saveAll(pendingSignals);
    }

    private double calculateRankScore(TradeSignal signal) {
        // 1. Confidence Score (from ensemble)
        double confidenceScore = signal.getConfidenceScore() != null
            ? signal.getConfidenceScore()
            : 1.0;

        // 2. R-Multiple: (Target - Entry) / (Entry - StopLoss)
        double rMultiple = calculateRMultiple(signal);

        // 3. Liquidity Score (based on avg volume/turnover)
        double liquidityScore = calculateLiquidityScore(signal.getInstrument());

        // 4. Strategy Win Rate (historical performance)
        double winRate = calculateStrategyWinRate(signal);

        // 5. Volatility Fit (ATR % vs SL distance)
        double volatilityFit = calculateVolatilityFit(signal);

        // Weighted composite score
        return (confidenceScore * CONFIDENCE_WEIGHT)
            + (rMultiple * R_MULTIPLE_WEIGHT)
            + (liquidityScore * LIQUIDITY_WEIGHT)
            + (winRate * WIN_RATE_WEIGHT)
            + (volatilityFit * VOLATILITY_WEIGHT);
    }

    private double calculateRMultiple(TradeSignal signal) {
        if (signal.getStopLoss() == null || signal.getTargetPrice() == null) {
            return 0.0;
        }

        double entry = signal.getEntryPrice().doubleValue();
        double stopLoss = signal.getStopLoss().doubleValue();
        double target = signal.getTargetPrice().doubleValue();

        double risk = entry - stopLoss;
        double reward = target - entry;

        if (risk <= 0) return 0.0;

        return reward / risk;
    }

    private double calculateLiquidityScore(Instrument instrument) {
        // Fetch recent volume data (last 20 trading days) and normalize to 0-1 scale
        List<Candle> recentCandles = candleRepository.findLatestCandles(
            instrument.getId(),
            PageRequest.of(0, 20)
        );

        if (recentCandles.isEmpty()) {
            return 0.5; // Default score if no data available
        }

        // Calculate average daily volume
        double avgVolume = recentCandles.stream()
            .mapToLong(Candle::getVolume)
            .average()
            .orElse(0.0);

        // Normalize to 0-1 scale using logarithmic scale
        // Score 0.0 for very low liquidity, 1.0 for very high liquidity
        if (avgVolume <= LIQUIDITY_THRESHOLD_LOW) {
            return 0.3 + (avgVolume / LIQUIDITY_THRESHOLD_LOW) * 0.3; // 0.3 to 0.6
        } else if (avgVolume <= LIQUIDITY_THRESHOLD_HIGH) {
            double ratio = (avgVolume - LIQUIDITY_THRESHOLD_LOW) / (LIQUIDITY_THRESHOLD_HIGH - LIQUIDITY_THRESHOLD_LOW);
            return 0.6 + ratio * 0.3; // 0.6 to 0.9
        } else {
            return 0.9 + Math.min(0.1, (avgVolume - LIQUIDITY_THRESHOLD_HIGH) / LIQUIDITY_THRESHOLD_HIGH * 0.1); // 0.9 to 1.0
        }
    }

    private double calculateStrategyWinRate(TradeSignal signal) {
        String strategyCode = signal.getStrategyRun().getStrategy().getCode();

        // Query historical performance for this strategy
        List<PaperTrade> historicalTrades = paperTradeRepository
            .findByStrategyCode(strategyCode);

        if (historicalTrades.isEmpty()) {
            return 0.5; // Default to 50% if no history
        }

        long winningTrades = historicalTrades.stream()
            .filter(t -> t.getPnl() != null && t.getPnl().compareTo(BigDecimal.ZERO) > 0)
            .count();

        return (double) winningTrades / historicalTrades.size();
    }

    private double calculateVolatilityFit(TradeSignal signal) {
        // Calculate ATR (Average True Range) for recent period (14 days)
        List<Candle> recentCandles = candleRepository.findCandlesUpToDate(
            signal.getInstrument().getId(),
            signal.getSignalDate(),
            PageRequest.of(0, 15) // Get 15 candles for 14-period ATR
        );

        if (recentCandles.isEmpty() || signal.getStopLoss() == null) {
            return 0.5; // Default score if no data or no SL
        }

        // Calculate ATR
        double atr = calculateATR(recentCandles, 14);
        if (atr <= 0) {
            return 0.5;
        }

        // Calculate stop loss distance as percentage of entry price
        double entryPrice = signal.getEntryPrice().doubleValue();
        double stopLossPrice = signal.getStopLoss().doubleValue();
        double slDistancePct = Math.abs(entryPrice - stopLossPrice) / entryPrice * 100;
        double atrPct = (atr / entryPrice) * 100;

        // Score based on how well SL aligns with volatility
        // Ideal SL distance = 1.5x to 2.5x ATR
        double slToAtrRatio = slDistancePct / atrPct;

        // Score curve:
        // - Ratio < 1.0: SL too tight (score 0.2 to 0.5)
        // - Ratio 1.0 to 1.5: Tight but acceptable (score 0.5 to 0.7)
        // - Ratio 1.5 to 2.5: Optimal (score 0.7 to 1.0)
        // - Ratio > 2.5 to 4.0: Wide but OK (score 1.0 to 0.6)
        // - Ratio > 4.0: Too wide (score 0.6 to 0.3)
        if (slToAtrRatio < 1.0) {
            return 0.2 + (slToAtrRatio * 0.3); // 0.2 to 0.5
        } else if (slToAtrRatio <= 1.5) {
            return 0.5 + ((slToAtrRatio - 1.0) * 0.4); // 0.5 to 0.7
        } else if (slToAtrRatio <= 2.5) {
            return 0.7 + ((slToAtrRatio - 1.5) * 0.3); // 0.7 to 1.0
        } else if (slToAtrRatio <= 4.0) {
            return 1.0 - ((slToAtrRatio - 2.5) * 0.16); // 1.0 to 0.6
        } else {
            return Math.max(0.3, 0.6 - ((slToAtrRatio - 4.0) * 0.1));
        }
    }

    /**
     * Calculate Average True Range (ATR) for a list of candles.
     * ATR measures market volatility by taking the average of true ranges over a specified period.
     *
     * @param candles list of candles (should be in descending order by date)
     * @param period ATR period (typically 14)
     * @return ATR value
     */
    private double calculateATR(List<Candle> candles, int period) {
        if (candles.size() < 2) {
            return 0.0;
        }

        // Calculate True Range for each candle
        // True Range = max(high - low, |high - prev_close|, |low - prev_close|)
        double sumTR = 0.0;
        int count = 0;

        // Use the minimum of available candles and requested period
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

    /**
     * Find top N pending signals by rank score
     */
    public List<TradeSignal> findTopPendingSignalsByRankScore(LocalDate date, int limit) {
        return tradeSignalRepository.findTopPendingSignalsByRankScore(date, limit);
    }
}