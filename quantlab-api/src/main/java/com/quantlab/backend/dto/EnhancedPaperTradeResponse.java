package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.quantlab.backend.entity.PaperTradeStatus;
import com.quantlab.backend.entity.ExitReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Enhanced DTO for PaperTrade response with new fields
 */
public class EnhancedPaperTradeResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("entryDate")
    private String entryDate;

    @JsonProperty("exitDate")
    private String exitDate;

    @JsonProperty("entryPrice")
    private BigDecimal entryPrice;

    @JsonProperty("exitPrice")
    private BigDecimal exitPrice;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("pnl")
    private BigDecimal pnl;

    @JsonProperty("pnlPct")
    private BigDecimal pnlPct;

    @JsonProperty("status")
    private PaperTradeStatus status;

    @JsonProperty("exitReason")
    private ExitReason exitReason;

    @JsonProperty("currentPrice")
    private BigDecimal currentPrice;

    @JsonProperty("unrealizedPnl")
    private BigDecimal unrealizedPnl;

    @JsonProperty("unrealizedPnlPct")
    private BigDecimal unrealizedPnlPct;

    @JsonProperty("rMultiple")
    private BigDecimal rMultiple;

    @JsonProperty("stopLoss")
    private BigDecimal stopLoss;

    @JsonProperty("targetPrice")
    private BigDecimal targetPrice;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Static factory method
    public static EnhancedPaperTradeResponse fromEntity(com.quantlab.backend.entity.PaperTrade paperTrade) {
        EnhancedPaperTradeResponse response = new EnhancedPaperTradeResponse();
        response.setId(paperTrade.getId());
        response.setSymbol(paperTrade.getInstrument().getSymbol());
        response.setEntryDate(paperTrade.getEntryDate().format(DATE_FORMATTER));
        response.setExitDate(paperTrade.getExitDate() != null ? paperTrade.getExitDate().format(DATE_FORMATTER) : null);
        response.setEntryPrice(paperTrade.getEntryPrice());
        response.setExitPrice(paperTrade.getExitPrice());
        response.setQuantity(paperTrade.getQuantity());
        response.setPnl(paperTrade.getPnl());
        response.setPnlPct(paperTrade.getPnlPct());
        response.setStatus(paperTrade.getStatus());
        response.setExitReason(paperTrade.getExitReason());
        response.setCurrentPrice(paperTrade.getCurrentPrice());
        response.setUnrealizedPnl(paperTrade.getUnrealizedPnl());
        response.setUnrealizedPnlPct(paperTrade.getUnrealizedPnlPct());
        response.setRMultiple(paperTrade.getRMultiple());
        response.setStopLoss(paperTrade.getStopLoss());
        response.setTargetPrice(paperTrade.getTargetPrice());
        return response;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }

    public String getExitDate() {
        return exitDate;
    }

    public void setExitDate(String exitDate) {
        this.exitDate = exitDate;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public BigDecimal getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(BigDecimal exitPrice) {
        this.exitPrice = exitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPnl() {
        return pnl;
    }

    public void setPnl(BigDecimal pnl) {
        this.pnl = pnl;
    }

    public BigDecimal getPnlPct() {
        return pnlPct;
    }

    public void setPnlPct(BigDecimal pnlPct) {
        this.pnlPct = pnlPct;
    }

    public PaperTradeStatus getStatus() {
        return status;
    }

    public void setStatus(PaperTradeStatus status) {
        this.status = status;
    }

    public ExitReason getExitReason() {
        return exitReason;
    }

    public void setExitReason(ExitReason exitReason) {
        this.exitReason = exitReason;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public BigDecimal getUnrealizedPnlPct() {
        return unrealizedPnlPct;
    }

    public void setUnrealizedPnlPct(BigDecimal unrealizedPnlPct) {
        this.unrealizedPnlPct = unrealizedPnlPct;
    }

    public BigDecimal getRMultiple() {
        return rMultiple;
    }

    public void setRMultiple(BigDecimal rMultiple) {
        this.rMultiple = rMultiple;
    }

    public BigDecimal getStopLoss() {
        return stopLoss;
    }

    public void setStopLoss(BigDecimal stopLoss) {
        this.stopLoss = stopLoss;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnhancedPaperTradeResponse that = (EnhancedPaperTradeResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(entryDate, that.entryDate) &&
               Objects.equals(exitDate, that.exitDate) &&
               Objects.equals(entryPrice, that.entryPrice) &&
               Objects.equals(exitPrice, that.exitPrice) &&
               Objects.equals(quantity, that.quantity) &&
               Objects.equals(pnl, that.pnl) &&
               Objects.equals(pnlPct, that.pnlPct) &&
               Objects.equals(status, that.status) &&
               Objects.equals(exitReason, that.exitReason) &&
               Objects.equals(currentPrice, that.currentPrice) &&
               Objects.equals(unrealizedPnl, that.unrealizedPnl) &&
               Objects.equals(unrealizedPnlPct, that.unrealizedPnlPct) &&
               Objects.equals(rMultiple, that.rMultiple) &&
               Objects.equals(stopLoss, that.stopLoss) &&
               Objects.equals(targetPrice, that.targetPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, entryDate, exitDate, entryPrice, exitPrice, quantity, pnl, pnlPct,
                          status, exitReason, currentPrice, unrealizedPnl, unrealizedPnlPct, rMultiple,
                          stopLoss, targetPrice);
    }

    @Override
    public String toString() {
        return "EnhancedPaperTradeResponse{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", entryDate='" + entryDate + '\'' +
                ", exitDate='" + exitDate + '\'' +
                ", entryPrice=" + entryPrice +
                ", exitPrice=" + exitPrice +
                ", quantity=" + quantity +
                ", pnl=" + pnl +
                ", pnlPct=" + pnlPct +
                ", status=" + status +
                ", exitReason=" + exitReason +
                ", currentPrice=" + currentPrice +
                ", unrealizedPnl=" + unrealizedPnl +
                ", unrealizedPnlPct=" + unrealizedPnlPct +
                ", rMultiple=" + rMultiple +
                ", stopLoss=" + stopLoss +
                ", targetPrice=" + targetPrice +
                '}';
    }
}