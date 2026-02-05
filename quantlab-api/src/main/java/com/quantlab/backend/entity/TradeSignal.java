package com.quantlab.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TradeSignal entity representing generated trading signals
 * Signals != trades
 */
@Entity
@Table(name = "trade_signal")
public class TradeSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_run_id", nullable = false)
    private StrategyRun strategyRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "signal_date", nullable = false)
    private LocalDate signalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Side side;

    @Column(name = "entry_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal entryPrice;

    @Column(name = "stop_loss", precision = 19, scale = 4)
    private BigDecimal stopLoss;

    @Column(name = "target_price", precision = 19, scale = 4)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeSignalStatus status;

    @Column(nullable = false)
    private Integer quantity;

    public TradeSignal() {
    }

    public TradeSignal(Long id, StrategyRun strategyRun, Instrument instrument, LocalDate signalDate, Side side, BigDecimal entryPrice, BigDecimal stopLoss, BigDecimal targetPrice, Integer quantity, TradeSignalStatus status) {
        this.id = id;
        this.strategyRun = strategyRun;
        this.instrument = instrument;
        this.signalDate = signalDate;
        this.side = side;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.targetPrice = targetPrice;
        this.quantity = quantity;
        this.status = status;
    }

    public TradeSignal(Long id, StrategyRun strategyRun, Instrument instrument, LocalDate signalDate, Side side, BigDecimal entryPrice, BigDecimal stopLoss, BigDecimal targetPrice, Integer quantity) {
        this.id = id;
        this.strategyRun = strategyRun;
        this.instrument = instrument;
        this.signalDate = signalDate;
        this.side = side;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.targetPrice = targetPrice;
        this.quantity = quantity;
        this.status = TradeSignalStatus.PENDING;
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

    public LocalDate getSignalDate() {
        return signalDate;
    }

    public void setSignalDate(LocalDate signalDate) {
        this.signalDate = signalDate;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
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

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public TradeSignalStatus getStatus() {
        return status;
    }

    public void setStatus(TradeSignalStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TradeSignal that = (TradeSignal) o;

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
