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

    @Column(name = "stop_loss", precision = 19, scale = 4)
    private BigDecimal stopLoss;

    @Column(name = "target_price", precision = 19, scale = 4)
    private BigDecimal targetPrice;

    @Column(name = "exit_date", nullable = false)
    private LocalDate exitDate;

    @Column(name = "exit_price", precision = 19, scale = 4)
    private BigDecimal exitPrice;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaperTradeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExitReason exitReason;

    @Column(precision = 15, scale = 4)
    private BigDecimal currentPrice;

    @Column(precision = 15, scale = 4)
    private BigDecimal unrealizedPnl;

    @Column(name = "unrealized_pnl_pct", precision = 10, scale = 4)
    private BigDecimal unrealizedPnlPct;

    @Column(precision = 10, scale = 4)
    private BigDecimal rMultiple;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 19, scale = 4)
    private BigDecimal pnl;

    @Column(name = "pnl_pct", precision = 10, scale = 4)
    private BigDecimal pnlPct;

    public PaperTrade() {
    }

    public PaperTrade(Long id, StrategyRun strategyRun, Instrument instrument, LocalDate entryDate, BigDecimal entryPrice, LocalDate exitDate, BigDecimal exitPrice, Integer quantity, BigDecimal pnl, BigDecimal pnlPct, PaperTradeStatus status, ExitReason exitReason, BigDecimal currentPrice, BigDecimal unrealizedPnl, BigDecimal unrealizedPnlPct, BigDecimal rMultiple) {
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
        this.status = status;
        this.exitReason = exitReason;
        this.currentPrice = currentPrice;
        this.unrealizedPnl = unrealizedPnl;
        this.unrealizedPnlPct = unrealizedPnlPct;
        this.rMultiple = rMultiple;
    }

    public PaperTrade(Long id, StrategyRun strategyRun, Instrument instrument, LocalDate entryDate, BigDecimal entryPrice, Integer quantity) {
        this.id = id;
        this.strategyRun = strategyRun;
        this.instrument = instrument;
        this.entryDate = entryDate;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
        this.status = PaperTradeStatus.OPEN;
        this.exitReason = null;
        this.currentPrice = null;
        this.unrealizedPnl = null;
        this.unrealizedPnlPct = null;
        this.rMultiple = null;
        this.exitDate = null;
        this.exitPrice = null;
        this.pnl = null;
        this.pnlPct = null;
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

    public PaperTradeStatus getStatus() {
        return status;
    }

    public void setStatus(PaperTradeStatus status) {
        this.status = status;
    }

    public ExitReason getExitReason() {
        return exitReason;
    }

    public void setExitReason(ExitReason exitReason) {
        this.exitReason = exitReason;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public BigDecimal getUnrealizedPnlPct() {
        return unrealizedPnlPct;
    }

    public void setUnrealizedPnlPct(BigDecimal unrealizedPnlPct) {
        this.unrealizedPnlPct = unrealizedPnlPct;
    }

    public BigDecimal getRMultiple() {
        return rMultiple;
    }

    public void setRMultiple(BigDecimal rMultiple) {
        this.rMultiple = rMultiple;
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
