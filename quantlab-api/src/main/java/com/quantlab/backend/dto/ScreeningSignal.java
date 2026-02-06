package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO representing a single screening signal.
 * Contains all the information needed for a trader to act on a signal.
 */
public class ScreeningSignal {

    @JsonProperty("symbol")
    @NotBlank(message = "Symbol is required")
    private String symbol;

    @JsonProperty("signalDate")
    @NotNull(message = "Signal date is required")
    private LocalDate signalDate;

    @JsonProperty("side")
    @NotBlank(message = "Side is required")
    private String side; // BUY or SELL

    @JsonProperty("entry")
    @NotNull(message = "Entry price is required")
    private BigDecimal entryPrice;

    @JsonProperty("stopLoss")
    private BigDecimal stopLoss;

    @JsonProperty("target")
    private BigDecimal target;

    @JsonProperty("strategyCode")
    @NotBlank(message = "Strategy code is required")
    private String strategyCode;

    public ScreeningSignal() {
    }

    public ScreeningSignal(String symbol, LocalDate signalDate, String side,
                          BigDecimal entryPrice, BigDecimal stopLoss,
                          BigDecimal target, String strategyCode) {
        this.symbol = symbol;
        this.signalDate = signalDate;
        this.side = side;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.target = target;
        this.strategyCode = strategyCode;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public LocalDate getSignalDate() {
        return signalDate;
    }

    public void setSignalDate(LocalDate signalDate) {
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

    public BigDecimal getTarget() {
        return target;
    }

    public void setTarget(BigDecimal target) {
        this.target = target;
    }

    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScreeningSignal that = (ScreeningSignal) o;
        return Objects.equals(symbol, that.symbol) &&
                Objects.equals(signalDate, that.signalDate) &&
                Objects.equals(side, that.side) &&
                Objects.equals(entryPrice, that.entryPrice) &&
                Objects.equals(stopLoss, that.stopLoss) &&
                Objects.equals(target, that.target) &&
                Objects.equals(strategyCode, that.strategyCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, signalDate, side, entryPrice, stopLoss, target, strategyCode);
    }

    @Override
    public String toString() {
        return "ScreeningSignal{" +
                "symbol='" + symbol + '\'' +
                ", signalDate=" + signalDate +
                ", side='" + side + '\'' +
                ", entryPrice=" + entryPrice +
                ", stopLoss=" + stopLoss +
                ", target=" + target +
                ", strategyCode='" + strategyCode + '\'' +
                '}';
    }
}
