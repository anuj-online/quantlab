package com.quantlab.backend.service;

import com.quantlab.backend.entity.*;
import com.quantlab.backend.repository.PaperTradeRepository;
import com.quantlab.backend.repository.TradeSignalRepository;
import com.quantlab.backend.marketdata.MarketDataProvider;
import com.quantlab.backend.marketdata.CompositeMarketDataProvider;
import com.quantlab.backend.repository.StrategyRunRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing trade signals and paper trades
 * Handles signal lifecycle management and trade execution
 */
@Service
@Transactional
public class TradeService {

    private final TradeSignalRepository tradeSignalRepository;
    private final PaperTradeRepository paperTradeRepository;
    private final MarketDataProvider marketDataProvider;
    private final StrategyRunRepository strategyRunRepository;

    public TradeService(TradeSignalRepository tradeSignalRepository,
                      PaperTradeRepository paperTradeRepository,
                      @Qualifier("compositeMarketDataProvider") MarketDataProvider marketDataProvider,
                      StrategyRunRepository strategyRunRepository) {
        this.tradeSignalRepository = tradeSignalRepository;
        this.paperTradeRepository = paperTradeRepository;
        this.marketDataProvider = marketDataProvider;
        this.strategyRunRepository = strategyRunRepository;
    }

    /**
     * Execute a pending signal - convert to open trade
     */
    @Transactional
    public PaperTrade executeSignal(Long signalId, Integer quantity, BigDecimal entryPrice) {
        TradeSignal signal = tradeSignalRepository.findById(signalId)
                .orElseThrow(() -> new RuntimeException("Signal not found: " + signalId));

        if (signal.getStatus() != TradeSignalStatus.PENDING) {
            throw new RuntimeException("Signal is not in PENDING status");
        }

        // Create open paper trade
        PaperTrade paperTrade = new PaperTrade(
                null,
                signal.getStrategyRun(),
                signal.getInstrument(),
                LocalDate.now(),
                entryPrice != null ? entryPrice : signal.getEntryPrice(),
                quantity != null ? quantity : signal.getQuantity()
        );

        // Set trade details from signal
        paperTrade.setStopLoss(signal.getStopLoss());
        paperTrade.setTargetPrice(signal.getTargetPrice());

        // Save the paper trade
        paperTrade = paperTradeRepository.save(paperTrade);

        // Update signal status to EXECUTED
        signal.setStatus(TradeSignalStatus.EXECUTED);
        tradeSignalRepository.save(signal);

        return paperTrade;
    }

    /**
     * Manually approve a signal for execution
     */
    @Transactional
    public PaperTrade approveSignal(Long signalId) {
        return executeSignal(signalId, null, null);
    }

    /**
     * Ignore a signal - mark as ignored
     */
    @Transactional
    public void ignoreSignal(Long signalId) {
        TradeSignal signal = tradeSignalRepository.findById(signalId)
                .orElseThrow(() -> new RuntimeException("Signal not found: " + signalId));

        signal.setStatus(TradeSignalStatus.IGNORED);
        tradeSignalRepository.save(signal);
    }

    /**
     * Get all pending signals for a strategy run
     */
    public List<TradeSignal> getPendingSignals(Long strategyRunId) {
        return tradeSignalRepository.findByStrategyRunIdAndStatus(strategyRunId, TradeSignalStatus.PENDING);
    }

    /**
     * Get all ignored signals for a strategy run
     */
    public List<TradeSignal> getIgnoredSignals(Long strategyRunId) {
        return tradeSignalRepository.findByStrategyRunIdAndStatus(strategyRunId, TradeSignalStatus.IGNORED);
    }

    /**
     * Find all paper trades for a specific strategy run and status
     */
    public List<PaperTrade> findByStrategyRunIdAndStatus(Long strategyRunId, PaperTradeStatus status) {
        return paperTradeRepository.findByStrategyRunIdAndStatus(strategyRunId, status);
    }

    /**
     * Check if price has reached entry level for pending signals
     */
    @Transactional
    public void checkEntryTriggers() {
        List<TradeSignal> pendingSignals = tradeSignalRepository.findByStatus(TradeSignalStatus.PENDING);

        for (TradeSignal signal : pendingSignals) {
            try {
                BigDecimal currentPrice = marketDataProvider.getCurrentPrice(signal.getInstrument().getSymbol());

                // Check if price has reached entry (for BUY signals)
                if (signal.getSide() == Side.BUY && currentPrice.compareTo(signal.getEntryPrice()) >= 0) {
                    executeSignal(signal.getId(), null, currentPrice);
                }
                // Check if price has reached entry (for SELL signals)
                else if (signal.getSide() == Side.SELL && currentPrice.compareTo(signal.getEntryPrice()) <= 0) {
                    executeSignal(signal.getId(), null, currentPrice);
                }
            } catch (Exception e) {
                // Log error but continue with other signals
                System.err.println("Error checking entry trigger for signal " + signal.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Calculate unrealized PnL for open positions
     */
    @Transactional
    public void updateUnrealizedPnL() {
        List<PaperTrade> openTrades = paperTradeRepository.findByStatus(PaperTradeStatus.OPEN);

        for (PaperTrade trade : openTrades) {
            try {
                BigDecimal currentPrice = marketDataProvider.getCurrentPrice(trade.getInstrument().getSymbol());
                BigDecimal unrealizedPnl = currentPrice.subtract(trade.getEntryPrice())
                        .multiply(new BigDecimal(trade.getQuantity()));

                BigDecimal unrealizedPnlPct = currentPrice.subtract(trade.getEntryPrice())
                        .divide(trade.getEntryPrice(), 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

                trade.setCurrentPrice(currentPrice);
                trade.setUnrealizedPnl(unrealizedPnl);
                trade.setUnrealizedPnlPct(unrealizedPnlPct);

                // Calculate R-multiple (reward/risk)
                if (trade.getTargetPrice() != null && trade.getStopLoss() != null) {
                    BigDecimal risk = trade.getEntryPrice().subtract(trade.getStopLoss());
                    BigDecimal reward = trade.getTargetPrice().subtract(trade.getEntryPrice());

                    if (risk.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal rMultiple = reward.divide(risk, 4, RoundingMode.HALF_UP);
                        trade.setRMultiple(rMultiple);
                    }
                }

                paperTradeRepository.save(trade);
            } catch (Exception e) {
                // Log error but continue with other trades
                System.err.println("Error updating PnL for trade " + trade.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Close positions that hit stop loss or target
     */
    @Transactional
    public void checkStopLossAndTargets() {
        List<PaperTrade> openTrades = paperTradeRepository.findByStatus(PaperTradeStatus.OPEN);

        for (PaperTrade trade : openTrades) {
            try {
                BigDecimal currentPrice = marketDataProvider.getCurrentPrice(trade.getInstrument().getSymbol());

                // Check stop loss
                if (trade.getStopLoss() != null && currentPrice.compareTo(trade.getStopLoss()) <= 0) {
                    closeTrade(trade, trade.getStopLoss(), ExitReason.STOP_LOSS);
                }
                // Check target
                else if (trade.getTargetPrice() != null && currentPrice.compareTo(trade.getTargetPrice()) >= 0) {
                    closeTrade(trade, trade.getTargetPrice(), ExitReason.TARGET);
                }

            } catch (Exception e) {
                // Log error but continue with other trades
                System.err.println("Error checking SL/target for trade " + trade.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Close a trade
     */
    private void closeTrade(PaperTrade trade, BigDecimal exitPrice, ExitReason exitReason) {
        BigDecimal pnl = exitPrice.subtract(trade.getEntryPrice())
                .multiply(new BigDecimal(trade.getQuantity()));

        BigDecimal pnlPct = exitPrice.subtract(trade.getEntryPrice())
                .divide(trade.getEntryPrice(), 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        trade.setExitDate(LocalDate.now());
        trade.setExitPrice(exitPrice);
        trade.setPnl(pnl);
        trade.setPnlPct(pnlPct);
        trade.setStatus(PaperTradeStatus.CLOSED);
        trade.setExitReason(exitReason);

        // Reset unrealized fields
        trade.setCurrentPrice(null);
        trade.setUnrealizedPnl(null);
        trade.setUnrealizedPnlPct(null);

        paperTradeRepository.save(trade);
    }

    /**
     * Get profit positions (open trades where current price > entry price)
     */
    public List<PaperTrade> getProfitPositions(Long strategyRunId) {
        List<PaperTrade> openTrades = paperTradeRepository.findByStrategyRunIdAndStatus(strategyRunId, PaperTradeStatus.OPEN);

        return openTrades.stream()
                .filter(trade -> trade.getCurrentPrice() != null &&
                        trade.getCurrentPrice().compareTo(trade.getEntryPrice()) > 0)
                .toList();
    }

    /**
     * Get losing positions (open trades where current price < entry price)
     */
    public List<PaperTrade> getLosingPositions(Long strategyRunId) {
        List<PaperTrade> openTrades = paperTradeRepository.findByStrategyRunIdAndStatus(strategyRunId, PaperTradeStatus.OPEN);

        return openTrades.stream()
                .filter(trade -> trade.getCurrentPrice() != null &&
                        trade.getCurrentPrice().compareTo(trade.getEntryPrice()) < 0)
                .toList();
    }
}