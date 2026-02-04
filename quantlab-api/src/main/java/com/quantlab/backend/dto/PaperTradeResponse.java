package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

public class PaperTradeResponse {

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

    public PaperTradeResponse() {
    }

    public PaperTradeResponse(String symbol, String entryDate, String exitDate,
                              BigDecimal entryPrice, BigDecimal exitPrice,
                              Integer quantity, BigDecimal pnl, BigDecimal pnlPct) {
        this.symbol = symbol;
        this.entryDate = entryDate;
        this.exitDate = exitDate;
        this.entryPrice = entryPrice;
        this.exitPrice = exitPrice;
        this.quantity = quantity;
        this.pnl = pnl;
        this.pnlPct = pnlPct;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaperTradeResponse that = (PaperTradeResponse) o;
        return Objects.equals(symbol, that.symbol) &&
               Objects.equals(entryDate, that.entryDate) &&
               Objects.equals(exitDate, that.exitDate) &&
               Objects.equals(entryPrice, that.entryPrice) &&
               Objects.equals(exitPrice, that.exitPrice) &&
               Objects.equals(quantity, that.quantity) &&
               Objects.equals(pnl, that.pnl) &&
               Objects.equals(pnlPct, that.pnlPct);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, entryDate, exitDate, entryPrice, exitPrice, quantity, pnl, pnlPct);
    }

    @Override
    public String toString() {
        return "PaperTradeResponse{" +
                "symbol='" + symbol + '\'' +
                ", entryDate='" + entryDate + '\'' +
                ", exitDate='" + exitDate + '\'' +
                ", entryPrice=" + entryPrice +
                ", exitPrice=" + exitPrice +
                ", quantity=" + quantity +
                ", pnl=" + pnl +
                ", pnlPct=" + pnlPct +
                '}';
    }
}
