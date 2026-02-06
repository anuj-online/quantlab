package com.quantlab.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "capital_allocation_position")
public class CapitalAllocationPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private CapitalAllocationSnapshot snapshot;

    @Column(name = "symbol", nullable = false, length = 50)
    private String symbol;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "capital_used", nullable = false, precision = 19, scale = 2)
    private BigDecimal capitalUsed;

    @Column(name = "risk_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal riskAmount;

    @Column(name = "expected_r", nullable = false, precision = 10, scale = 4)
    private BigDecimal expectedR;

    @Column(name = "allocation_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal allocationPct;

    public CapitalAllocationPosition() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CapitalAllocationSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(CapitalAllocationSnapshot snapshot) { this.snapshot = snapshot; }

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