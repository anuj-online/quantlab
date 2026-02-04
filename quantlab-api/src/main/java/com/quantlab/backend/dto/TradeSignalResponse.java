package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

public class TradeSignalResponse {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("signalDate")
    private String signalDate;

    @JsonProperty("side")
    private String side;

    @JsonProperty("entryPrice")
    private BigDecimal entryPrice;

    @JsonProperty("stopLoss")
    private BigDecimal stopLoss;

    @JsonProperty("targetPrice")
    private BigDecimal targetPrice;

    @JsonProperty("quantity")
    private Integer quantity;

    public TradeSignalResponse() {
    }

    public TradeSignalResponse(String symbol, String signalDate, String side,
                               BigDecimal entryPrice, BigDecimal stopLoss,
                               BigDecimal targetPrice, Integer quantity) {
        this.symbol = symbol;
        this.signalDate = signalDate;
        this.side = side;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.targetPrice = targetPrice;
        this.quantity = quantity;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSignalDate() {
        return signalDate;
    }

    public void setSignalDate(String signalDate) {
        this.signalDate = signalDate;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TradeSignalResponse that = (TradeSignalResponse) o;
        return Objects.equals(symbol, that.symbol) &&
               Objects.equals(signalDate, that.signalDate) &&
               Objects.equals(side, that.side) &&
               Objects.equals(entryPrice, that.entryPrice) &&
               Objects.equals(stopLoss, that.stopLoss) &&
               Objects.equals(targetPrice, that.targetPrice) &&
               Objects.equals(quantity, that.quantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, signalDate, side, entryPrice, stopLoss, targetPrice, quantity);
    }

    @Override
    public String toString() {
        return "TradeSignalResponse{" +
                "symbol='" + symbol + '\'' +
                ", signalDate='" + signalDate + '\'' +
                ", side='" + side + '\'' +
                ", entryPrice=" + entryPrice +
                ", stopLoss=" + stopLoss +
                ", targetPrice=" + targetPrice +
                ", quantity=" + quantity +
                '}';
    }
}
