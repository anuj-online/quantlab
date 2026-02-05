package com.quantlab.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * StrategyRun entity representing one execution of a strategy with params
 * Used for replay & comparison
 * Strategy runs are immutable - re-running = new strategy_run
 */
@Entity
@Table(name = "strategy_run")
public class StrategyRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id", nullable = false)
    private Strategy strategy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MarketType market;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "params_json", nullable = false)
    private String paramsJson;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "run_timestamp", nullable = false)
    private LocalDate runTimestamp = LocalDate.now();

    public StrategyRun() {
    }

    public StrategyRun(Long id, Strategy strategy, MarketType market, String paramsJson, LocalDate startDate, LocalDate endDate, LocalDate runTimestamp) {
        this.id = id;
        this.strategy = strategy;
        this.market = market;
        this.paramsJson = paramsJson;
        this.startDate = startDate;
        this.endDate = endDate;
        this.runTimestamp = runTimestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }

    public MarketType getMarket() {
        return market;
    }

    public void setMarket(MarketType market) {
        this.market = market;
    }

    public String getParamsJson() {
        return paramsJson;
    }

    public void setParamsJson(String paramsJson) {
        this.paramsJson = paramsJson;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getRunTimestamp() {
        return runTimestamp;
    }

    public void setRunTimestamp(LocalDate runTimestamp) {
        this.runTimestamp = runTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StrategyRun that = (StrategyRun) o;

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
