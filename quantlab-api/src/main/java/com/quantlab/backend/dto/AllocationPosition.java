package com.quantlab.backend.dto;

import java.math.BigDecimal;

public class AllocationPosition {
    private Long id;
    private String symbol;
    private Integer quantity;
    private BigDecimal capitalUsed;
    private BigDecimal riskAmount;
    private BigDecimal expectedR;
    private BigDecimal allocationPct;

    // Constructors
    public AllocationPosition() {}

    public AllocationPosition(String symbol, Integer quantity, BigDecimal capitalUsed,
                            BigDecimal riskAmount, BigDecimal expectedR, BigDecimal allocationPct) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.capitalUsed = capitalUsed;
        this.riskAmount = riskAmount;
        this.expectedR = expectedR;
        this.allocationPct = allocationPct;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getCapitalUsed() { return capitalUsed; }
    public void setCapitalUsed(BigDecimal capitalUsed) { this.capitalUsed = capitalUsed; }

    public BigDecimal getRiskAmount() { return riskAmount; }
    public void setRiskAmount(BigDecimal riskAmount) { this.riskAmount = riskAmount; }

    public BigDecimal getExpectedR() { return expectedR; }
    public void setExpectedR(BigDecimal expectedR) { this.expectedR = expectedR; }

    public BigDecimal getAllocationPct() { return allocationPct; }
    public void setAllocationPct(BigDecimal allocationPct) { this.allocationPct = allocationPct; }
}