package com.quantlab.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * PaperTrade entity representing closed trades
 * Only stores completed/closed trades
 */
@Entity
@Table(name = "paper_trade")
public class PaperTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_run_id", nullable = false)
    private StrategyRun strategyRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "entry_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal entryPrice;

    @Column(name = "exit_date", nullable = false)
    private LocalDate exitDate;

    @Column(name = "exit_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal exitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal pnl;

    @Column(name = "pnl_pct", precision = 10, scale = 4)
    private BigDecimal pnlPct;

    public PaperTrade() {
    }

    public PaperTrade(Long id, StrategyRun strategyRun, Instrument instrument, LocalDate entryDate, BigDecimal entryPrice, LocalDate exitDate, BigDecimal exitPrice, Integer quantity, BigDecimal pnl, BigDecimal pnlPct) {
        this.id = id;
        this.strategyRun = strategyRun;
        this.instrument = instrument;
        this.entryDate = entryDate;
        this.entryPrice = entryPrice;
        this.exitDate = exitDate;
        this.exitPrice = exitPrice;
        this.quantity = quantity;
        this.pnl = pnl;
        this.pnlPct = pnlPct;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StrategyRun getStrategyRun() {
        return strategyRun;
    }

    public void setStrategyRun(StrategyRun strategyRun) {
        this.strategyRun = strategyRun;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public LocalDate getExitDate() {
        return exitDate;
    }

    public void setExitDate(LocalDate exitDate) {
        this.exitDate = exitDate;
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

        PaperTrade that = (PaperTrade) o;

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
