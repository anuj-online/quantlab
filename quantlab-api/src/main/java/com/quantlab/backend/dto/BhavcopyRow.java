package com.quantlab.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for mapping Bhavcopy CSV rows.
 * Maps columns from NSE Bhavcopy file format.
 */
public class BhavcopyRow {

    private String symbol;
    private String series;
    private LocalDate date;
    private BigDecimal prevClose;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal lastPrice;
    private BigDecimal closePrice;
    private BigDecimal avgPrice;
    private Long totalTradedQuantity;
    private BigDecimal turnoverLacs;
    private Long numberOfTrades;
    private Long deliverableQuantity;
    private BigDecimal deliverablePercentage;

    public BhavcopyRow() {
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public BigDecimal getPrevClose() {
        return prevClose;
    }

    public void setPrevClose(BigDecimal prevClose) {
        this.prevClose = prevClose;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }

    public BigDecimal getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(BigDecimal lastPrice) {
        this.lastPrice = lastPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice;
    }

    public Long getTotalTradedQuantity() {
        return totalTradedQuantity;
    }

    public void setTotalTradedQuantity(Long totalTradedQuantity) {
        this.totalTradedQuantity = totalTradedQuantity;
    }

    public BigDecimal getTurnoverLacs() {
        return turnoverLacs;
    }

    public void setTurnoverLacs(BigDecimal turnoverLacs) {
        this.turnoverLacs = turnoverLacs;
    }

    public Long getNumberOfTrades() {
        return numberOfTrades;
    }

    public void setNumberOfTrades(Long numberOfTrades) {
        this.numberOfTrades = numberOfTrades;
    }

    public Long getDeliverableQuantity() {
        return deliverableQuantity;
    }

    public void setDeliverableQuantity(Long deliverableQuantity) {
        this.deliverableQuantity = deliverableQuantity;
    }

    public BigDecimal getDeliverablePercentage() {
        return deliverablePercentage;
    }

    public void setDeliverablePercentage(BigDecimal deliverablePercentage) {
        this.deliverablePercentage = deliverablePercentage;
    }

    @Override
    public String toString() {
        return "BhavcopyRow{" +
                "symbol='" + symbol + '\'' +
                ", series='" + series + '\'' +
                ", date=" + date +
                ", closePrice=" + closePrice +
                ", totalTradedQuantity=" + totalTradedQuantity +
                '}';
    }
}
