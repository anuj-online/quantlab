package com.quantlab.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AllocationRequest {
    private LocalDate date;
    private BigDecimal totalCapital;
    private double riskPerTradePct;
    private int maxOpenTrades;

    // Constructors
    public AllocationRequest() {}

    public AllocationRequest(LocalDate date, BigDecimal totalCapital,
                           double riskPerTradePct, int maxOpenTrades) {
        this.date = date;
        this.totalCapital = totalCapital;
        this.riskPerTradePct = riskPerTradePct;
        this.maxOpenTrades = maxOpenTrades;
    }

    // Getters and Setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getTotalCapital() { return totalCapital; }
    public void setTotalCapital(BigDecimal totalCapital) { this.totalCapital = totalCapital; }

    public double getRiskPerTradePct() { return riskPerTradePct; }
    public void setRiskPerTradePct(double riskPerTradePct) { this.riskPerTradePct = riskPerTradePct; }

    public int getMaxOpenTrades() { return maxOpenTrades; }
    public void setMaxOpenTrades(int maxOpenTrades) { this.maxOpenTrades = maxOpenTrades; }
}