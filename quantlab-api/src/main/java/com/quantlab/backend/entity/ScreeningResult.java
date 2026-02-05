package com.quantlab.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ScreeningResult entity representing daily screening outputs
 * Stores historical log of all signals generated across strategy runs
 */
@Entity
@Table(name = "screening_results")
public class ScreeningResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(nullable = false, length = 50)
    private String symbol;

    @Column(name = "strategy_code", nullable = false, length = 50)
    private String strategyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 10)
    private SignalType signalType;

    @Column(precision = 15, scale = 4)
    private BigDecimal entry;

    @Column(name = "stop_loss", precision = 15, scale = 4)
    private BigDecimal stopLoss;

    @Column(precision = 15, scale = 4)
    private BigDecimal target;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public ScreeningResult() {
    }

    public ScreeningResult(LocalDate runDate, String symbol, String strategyCode,
                          SignalType signalType, BigDecimal entry,
                          BigDecimal stopLoss, BigDecimal target) {
        this.runDate = runDate;
        this.symbol = symbol;
        this.strategyCode = strategyCode;
        this.signalType = signalType;
        this.entry = entry;
        this.stopLoss = stopLoss;
        this.target = target;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getRunDate() {
        return runDate;
    }

    public void setRunDate(LocalDate runDate) {
        this.runDate = runDate;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }

    public SignalType getSignalType() {
        return signalType;
    }

    public void setSignalType(SignalType signalType) {
        this.signalType = signalType;
    }

    public BigDecimal getEntry() {
        return entry;
    }

    public void setEntry(BigDecimal entry) {
        this.entry = entry;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScreeningResult that = (ScreeningResult) o;

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ScreeningResult{" +
                "id=" + id +
                ", runDate=" + runDate +
                ", symbol='" + symbol + '\'' +
                ", strategyCode='" + strategyCode + '\'' +
                ", signalType=" + signalType +
                ", entry=" + entry +
                ", stopLoss=" + stopLoss +
                ", target=" + target +
                ", createdAt=" + createdAt +
                '}';
    }
}
