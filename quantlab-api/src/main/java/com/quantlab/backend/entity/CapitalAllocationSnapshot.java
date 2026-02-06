package com.quantlab.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "capital_allocation_snapshot")
public class CapitalAllocationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "total_capital", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCapital;

    @Column(name = "deployed_capital", nullable = false, precision = 19, scale = 2)
    private BigDecimal deployedCapital;

    @Column(name = "free_cash", nullable = false, precision = 19, scale = 2)
    private BigDecimal freeCash;

    @Column(name = "expected_r_multiple", nullable = false, precision = 10, scale = 4)
    private BigDecimal expectedRMultiple;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CapitalAllocationPosition> positions;

    public CapitalAllocationSnapshot() {
        this.createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<CapitalAllocationPosition> getPositions() { return positions; }
    public void setPositions(List<CapitalAllocationPosition> positions) { this.positions = positions; }
}