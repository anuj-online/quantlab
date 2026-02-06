package com.quantlab.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CapitalAllocationSnapshot {
    private Long id;
    private LocalDate runDate;
    private BigDecimal totalCapital;
    private BigDecimal deployedCapital;
    private BigDecimal freeCash;
    private BigDecimal expectedRMultiple;
    private List<AllocationPosition> positions;
    private java.time.LocalDateTime createdAt;

    // Constructors
    public CapitalAllocationSnapshot() {}

    public CapitalAllocationSnapshot(LocalDate runDate, BigDecimal totalCapital,
                                   BigDecimal deployedCapital, BigDecimal freeCash,
                                   BigDecimal expectedRMultiple, List<AllocationPosition> positions) {
        this.runDate = runDate;
        this.totalCapital = totalCapital;
        this.deployedCapital = deployedCapital;
        this.freeCash = freeCash;
        this.expectedRMultiple = expectedRMultiple;
        this.positions = positions;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getRunDate() { return runDate; }
    public void setRunDate(LocalDate runDate) { this.runDate = runDate; }

    public BigDecimal getTotalCapital() { return totalCapital; }
    public void setTotalCapital(BigDecimal totalCapital) { this.totalCapital = totalCapital; }

    public BigDecimal getDeployedCapital() { return deployedCapital; }
    public void setDeployedCapital(BigDecimal deployedCapital) { this.deployedCapital = deployedCapital; }

    public BigDecimal getFreeCash() { return freeCash; }
    public void setFreeCash(BigDecimal freeCash) { this.freeCash = freeCash; }

    public BigDecimal getExpectedRMultiple() { return expectedRMultiple; }
    public void setExpectedRMultiple(BigDecimal expectedRMultiple) { this.expectedRMultiple = expectedRMultiple; }

    public List<AllocationPosition> getPositions() { return positions; }
    public void setPositions(List<AllocationPosition> positions) { this.positions = positions; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
}