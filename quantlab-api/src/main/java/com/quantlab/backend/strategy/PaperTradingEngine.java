package com.quantlab.backend.strategy;

import com.quantlab.backend.entity.Candle;
import com.quantlab.backend.entity.Instrument;
import com.quantlab.backend.entity.PaperTrade;
import com.quantlab.backend.entity.StrategyRun;
import com.quantlab.backend.entity.TradeSignal;
import com.quantlab.backend.repository.PaperTradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PaperTradingEngine - Executes paper trades based on signals with strategy-specific exit logic.
 *
 * <p>This engine simulates actual trading by:
 * <ul>
 *   <li>Finding entry candles matching signal dates</li>
 *   <li>Walking forward to find exit candles based on strategy type</li>
 *   <li>Calculating P&L and P&L percentage</li>
 *   <li>Creating and persisting PaperTrade entities</li>
 * </ul>
 *
 * <p>Exit Logic by Strategy Type:
 * <ul>
 *   <li><b>EMA_BREAKOUT</b>: Exit if close <= stopLoss OR close >= targetPrice</li>
 *   <li><b>SMA_CROSSOVER</b>: Exit on opposite signal (fast SMA < slow SMA for longs)</li>
 *   <li><b>GAP_UP_MOMENTUM</b>: Exit after 3 holding days OR if close <= stopLoss</li>
 * </ul>
 *
 * <p>Position Management:
 * <ul>
 *   <li>FIFO (First In, First Out) for multiple signals on same instrument</li>
 *   <li>Skips signals where entry candle is not found</li>
 *   <li>Skips signals with no exit found (open positions)</li>
 * </ul>
 */
@Component
public class PaperTradingEngine {

    private static final Logger logger = LoggerFactory.getLogger(PaperTradingEngine.class);

    private static final int GAP_UP_MAX_HOLDING_DAYS = 3;

    private final PaperTradeRepository paperTradeRepository;

    @Autowired
    public PaperTradingEngine(PaperTradeRepository paperTradeRepository) {
        this.paperTradeRepository = paperTradeRepository;
    }

    /**
     * Executes paper trades based on provided signals and candles.
     *
     * @param signals List of trade signals to execute
     * @param candles List of candles for price data
     * @param params Map of parameters (unused currently, reserved for future)
     * @return List of executed paper trades
     */
    public List<PaperTrade> executePaperTrades(List<TradeSignal> signals, List<Candle> candles, Map<String, Object> params) {
        if (signals == null || signals.isEmpty()) {
            logger.info("No signals provided for paper trading execution");
            return Collections.emptyList();
        }

        if (candles == null || candles.isEmpty()) {
            logger.warn("No candles provided for paper trading execution");
            return Collections.emptyList();
        }

        logger.info("Executing paper trades for {} signals with {} candles", signals.size(), candles.size());

        // Index candles by instrument and date for efficient lookup
        Map<Long, Map<LocalDate, Candle>> candleIndex = indexCandlesByInstrumentAndDate(candles);

        // Group signals by instrument for FIFO processing
        Map<Long, List<TradeSignal>> signalsByInstrument = signals.stream()
                .collect(Collectors.groupingBy(signal -> signal.getInstrument().getId()));

        List<PaperTrade> executedTrades = new ArrayList<>();

        // Process each instrument's signals in FIFO order
        for (Map.Entry<Long, List<TradeSignal>> entry : signalsByInstrument.entrySet()) {
            Long instrumentId = entry.getKey();
            List<TradeSignal> instrumentSignals = entry.getValue();

            // Sort by signal date to ensure FIFO order
            instrumentSignals.sort(Comparator.comparing(TradeSignal::getSignalDate));

            for (TradeSignal signal : instrumentSignals) {
                PaperTrade trade = processSignal(signal, candleIndex);
                if (trade != null) {
                    executedTrades.add(trade);
                }
            }
        }

        // Save all trades to database
        if (!executedTrades.isEmpty()) {
            List<PaperTrade> savedTrades = paperTradeRepository.saveAll(executedTrades);
            logger.info("Successfully saved {} paper trades to database", savedTrades.size());
        }

        return executedTrades;
    }

    /**
     * Indexes candles by instrument ID and trade date for O(1) lookup.
     *
     * @param candles List of candles to index
     * @return Map of instrument ID to map of dates to candles
     */
    private Map<Long, Map<LocalDate, Candle>> indexCandlesByInstrumentAndDate(List<Candle> candles) {
        Map<Long, Map<LocalDate, Candle>> index = new HashMap<>();

        for (Candle candle : candles) {
            Long instrumentId = candle.getInstrument().getId();
            LocalDate tradeDate = candle.getTradeDate();

            index.computeIfAbsent(instrumentId, k -> new TreeMap<>()).put(tradeDate, candle);
        }

        return index;
    }

    /**
     * Processes a single trade signal to find entry and exit, then creates a PaperTrade.
     *
     * @param signal The trade signal to process
     * @param candleIndex Indexed candles for lookup
     * @return PaperTrade if trade completed, null if skipped (no entry/exit found)
     */
    private PaperTrade processSignal(TradeSignal signal, Map<Long, Map<LocalDate, Candle>> candleIndex) {
        Long instrumentId = signal.getInstrument().getId();
        LocalDate signalDate = signal.getSignalDate();

        // Get candle map for this instrument
        Map<LocalDate, Candle> instrumentCandles = candleIndex.get(instrumentId);
        if (instrumentCandles == null) {
            logger.warn("No candles found for instrument ID {} at signal date {}", instrumentId, signalDate);
            return null;
        }

        // Find entry candle
        Candle entryCandle = instrumentCandles.get(signalDate);
        if (entryCandle == null) {
            logger.warn("Entry candle not found for instrument ID {} at signal date {}", instrumentId, signalDate);
            return null;
        }

        // Find exit candle based on strategy type
        ExitResult exitResult = findExitCandle(signal, entryCandle, instrumentCandles);

        if (exitResult == null) {
            logger.debug("No exit found for signal on instrument ID {} at {} - skipping (open position)",
                    instrumentId, signalDate);
            return null;
        }

        // Calculate P&L
        BigDecimal pnl = calculatePnl(signal, exitResult.exitPrice);
        BigDecimal pnlPct = calculatePnlPct(signal.getEntryPrice(), exitResult.exitPrice);

        // Create PaperTrade entity
        PaperTrade trade = new PaperTrade();
        trade.setStrategyRun(signal.getStrategyRun());
        trade.setInstrument(signal.getInstrument());
        trade.setEntryDate(signalDate);
        trade.setEntryPrice(signal.getEntryPrice());
        trade.setExitDate(exitResult.exitDate);
        trade.setExitPrice(exitResult.exitPrice);
        trade.setQuantity(signal.getQuantity());
        trade.setPnl(pnl);
        trade.setPnlPct(pnlPct);

        logger.debug("Created paper trade: entry={}, exit={}, pnl={}, pnlPct={}%",
                signalDate, exitResult.exitDate, pnl, pnlPct);

        return trade;
    }

    /**
     * Finds the exit candle based on the strategy type from the signal's strategy run.
     *
     * @param signal The trade signal
     * @param entryCandle The entry candle
     * @param candles Map of dates to candles for the instrument
     * @return ExitResult containing exit date and price, or null if no exit found
     */
    private ExitResult findExitCandle(TradeSignal signal, Candle entryCandle, Map<LocalDate, Candle> candles) {
        String strategyCode = signal.getStrategyRun().getStrategy().getCode();
        LocalDate entryDate = signal.getSignalDate();

        // Get all candles after entry date
        List<Map.Entry<LocalDate, Candle>> futureCandles = candles.entrySet().stream()
                .filter(e -> e.getKey().isAfter(entryDate))
                .sorted(Map.Entry.comparingByKey())
                .toList();

        if (futureCandles.isEmpty()) {
            return null;
        }

        // Dispatch to strategy-specific exit logic
        return switch (strategyCode) {
            case "EMA_BREAKOUT" -> findExitForEmaBreakout(signal, futureCandles);
            case "SMA_CROSSOVER" -> findExitForSmaCrossover(signal, futureCandles);
            case "GAP_UP_MOMENTUM" -> findExitForGapUpMomentum(signal, futureCandles);
            default -> {
                logger.warn("Unknown strategy type: {}, using default stop loss exit", strategyCode);
                yield findExitByStopLoss(signal, futureCandles);
            }
        };
    }

    /**
     * EMA_BREAKOUT exit logic: Exit if close <= stopLoss OR close >= targetPrice.
     * Checks each subsequent candle in sequence.
     */
    private ExitResult findExitForEmaBreakout(TradeSignal signal, List<Map.Entry<LocalDate, Candle>> futureCandles) {
        BigDecimal stopLoss = signal.getStopLoss();
        BigDecimal targetPrice = signal.getTargetPrice();

        if (stopLoss == null && targetPrice == null) {
            // No exit conditions defined, use last candle
            Map.Entry<LocalDate, Candle> lastEntry = futureCandles.get(futureCandles.size() - 1);
            return new ExitResult(lastEntry.getKey(), lastEntry.getValue().getClose());
        }

        for (Map.Entry<LocalDate, Candle> entry : futureCandles) {
            LocalDate date = entry.getKey();
            BigDecimal close = entry.getValue().getClose();

            // Check stop loss hit
            if (stopLoss != null && close.compareTo(stopLoss) <= 0) {
                logger.debug("EMA_BREAKOUT: Stop loss hit at {} - close: {}, stop: {}", date, close, stopLoss);
                return new ExitResult(date, close);
            }

            // Check target hit
            if (targetPrice != null && close.compareTo(targetPrice) >= 0) {
                logger.debug("EMA_BREAKOUT: Target hit at {} - close: {}, target: {}", date, close, targetPrice);
                return new ExitResult(date, close);
            }
        }

        // No exit condition met by end of data
        return null;
    }

    /**
     * SMA_CROSSOVER exit logic: Look for opposite signal (fast SMA < slow SMA for longs).
     * This requires calculating SMAs for each candle to detect crossover.
     */
    private ExitResult findExitForSmaCrossover(TradeSignal signal, List<Map.Entry<LocalDate, Candle>> futureCandles) {
        // For SMA_CROSSOVER, we primarily exit on stop loss
        // The opposite signal detection would require historical indicator calculation
        // which is typically done by the signal generator, not the executor

        BigDecimal stopLoss = signal.getStopLoss();
        if (stopLoss != null) {
            for (Map.Entry<LocalDate, Candle> entry : futureCandles) {
                LocalDate date = entry.getKey();
                BigDecimal close = entry.getValue().getClose();

                if (close.compareTo(stopLoss) <= 0) {
                    logger.debug("SMA_CROSSOVER: Stop loss hit at {} - close: {}, stop: {}", date, close, stopLoss);
                    return new ExitResult(date, close);
                }
            }
        }

        // No stop loss hit, wait for opposite signal (not implemented here as it requires pre-calculated signals)
        // Return null to indicate open position
        return null;
    }

    /**
     * GAP_UP_MOMENTUM exit logic: Exit after 3 holding days OR if close <= stopLoss.
     */
    private ExitResult findExitForGapUpMomentum(TradeSignal signal, List<Map.Entry<LocalDate, Candle>> futureCandles) {
        LocalDate entryDate = signal.getSignalDate();
        BigDecimal stopLoss = signal.getStopLoss();
        LocalDate targetExitDate = entryDate.plusDays(GAP_UP_MAX_HOLDING_DAYS);

        for (Map.Entry<LocalDate, Candle> entry : futureCandles) {
            LocalDate date = entry.getKey();
            BigDecimal close = entry.getValue().getClose();

            // Check stop loss hit first
            if (stopLoss != null && close.compareTo(stopLoss) <= 0) {
                logger.debug("GAP_UP_MOMENTUM: Stop loss hit at {} - close: {}, stop: {}", date, close, stopLoss);
                return new ExitResult(date, close);
            }

            // Check if we've reached max holding days
            if (!date.isBefore(targetExitDate)) {
                logger.debug("GAP_UP_MOMENTUM: Max holding days reached at {} - close: {}", date, close);
                return new ExitResult(date, close);
            }
        }

        // No exit found within data range
        return null;
    }

    /**
     * Default exit logic: Exit only on stop loss hit.
     */
    private ExitResult findExitByStopLoss(TradeSignal signal, List<Map.Entry<LocalDate, Candle>> futureCandles) {
        BigDecimal stopLoss = signal.getStopLoss();
        if (stopLoss == null) {
            return null;
        }

        for (Map.Entry<LocalDate, Candle> entry : futureCandles) {
            LocalDate date = entry.getKey();
            BigDecimal close = entry.getValue().getClose();

            if (close.compareTo(stopLoss) <= 0) {
                return new ExitResult(date, close);
            }
        }

        return null;
    }

    /**
     * Calculates profit/loss in currency terms.
     * P&L = (exitPrice - entryPrice) * quantity
     *
     * @param signal The trade signal
     * @param exitPrice The exit price
     * @return P&L in currency units
     */
    private BigDecimal calculatePnl(TradeSignal signal, BigDecimal exitPrice) {
        BigDecimal entryPrice = signal.getEntryPrice();
        Integer quantity = signal.getQuantity();

        BigDecimal priceDiff = exitPrice.subtract(entryPrice);
        return priceDiff.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Calculates profit/loss as a percentage.
     * P&L % = (exitPrice - entryPrice) / entryPrice
     *
     * @param entryPrice The entry price
     * @param exitPrice The exit price
     * @return P&L percentage (e.g., 0.0525 for 5.25%)
     */
    private BigDecimal calculatePnlPct(BigDecimal entryPrice, BigDecimal exitPrice) {
        if (entryPrice.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Entry price cannot be zero for P&L percentage calculation");
        }

        BigDecimal priceDiff = exitPrice.subtract(entryPrice);
        return priceDiff.divide(entryPrice, 4, RoundingMode.HALF_UP);
    }

    /**
     * Internal record to hold exit calculation results.
     */
    private record ExitResult(LocalDate exitDate, BigDecimal exitPrice) {
    }
}
