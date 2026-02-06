package com.quantlab.backend.controller;

import com.quantlab.backend.dto.EnhancedPaperTradeResponse;
import com.quantlab.backend.entity.PaperTradeStatus;
import com.quantlab.backend.service.TradeService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for trade-related operations
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "http://localhost:3000")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    /**
     * Get active trades for a strategy run
     */
    @GetMapping("/strategy-runs/{runId}/active-trades")
    public List<EnhancedPaperTradeResponse> getActiveTrades(@PathVariable Long runId) {
        return tradeService.findByStrategyRunIdAndStatus(runId, com.quantlab.backend.entity.PaperTradeStatus.OPEN)
                .stream()
                .map(EnhancedPaperTradeResponse::fromEntity)
                .toList();
    }

    /**
     * Get profit positions for a strategy run
     */
    @GetMapping("/strategy-runs/{runId}/profit-positions")
    public List<EnhancedPaperTradeResponse> getProfitPositions(@PathVariable Long runId) {
        return tradeService.getProfitPositions(runId).stream()
                .map(EnhancedPaperTradeResponse::fromEntity)
                .toList();
    }

    /**
     * Execute a pending signal
     */
    @PostMapping("/signals/{signalId}/execute")
    public EnhancedPaperTradeResponse executeSignal(@PathVariable Long signalId,
                                          @RequestParam(required = false) Integer quantity,
                                          @RequestParam(required = false) BigDecimal entryPrice) {
        var paperTrade = tradeService.executeSignal(signalId, quantity, entryPrice);
        return EnhancedPaperTradeResponse.fromEntity(paperTrade);
    }

    /**
     * Manually approve a signal
     */
    @PostMapping("/signals/{signalId}/approve")
    public EnhancedPaperTradeResponse approveSignal(@PathVariable Long signalId) {
        var paperTrade = tradeService.approveSignal(signalId);
        return EnhancedPaperTradeResponse.fromEntity(paperTrade);
    }

    /**
     * Ignore a signal
     */
    @PostMapping("/signals/{signalId}/ignore")
    public void ignoreSignal(@PathVariable Long signalId) {
        tradeService.ignoreSignal(signalId);
    }

    /**
     * Check entry triggers for all pending signals
     */
    @PostMapping("/signals/check-entry-triggers")
    public void checkEntryTriggers() {
        tradeService.checkEntryTriggers();
    }

    /**
     * Update unrealized PnL for all open positions
     */
    @PostMapping("/trades/update-unrealized-pnl")
    public void updateUnrealizedPnL() {
        tradeService.updateUnrealizedPnL();
    }

    /**
     * Check stop loss and target levels for all open positions
     */
    @PostMapping("/trades/check-stop-loss-targets")
    public void checkStopLossAndTargets() {
        tradeService.checkStopLossAndTargets();
    }
}