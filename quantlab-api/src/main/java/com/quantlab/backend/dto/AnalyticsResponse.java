package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

public class AnalyticsResponse {

    @JsonProperty("totalTrades")
    private Integer totalTrades;

    @JsonProperty("winRate")
    private BigDecimal winRate;

    @JsonProperty("totalPnl")
    private BigDecimal totalPnl;

    @JsonProperty("maxDrawdown")
    private BigDecimal maxDrawdown;

    public AnalyticsResponse() {
    }

    public AnalyticsResponse(Integer totalTrades, BigDecimal winRate,
                             BigDecimal totalPnl, BigDecimal maxDrawdown) {
        this.totalTrades = totalTrades;
        this.winRate = winRate;
        this.totalPnl = totalPnl;
        this.maxDrawdown = maxDrawdown;
    }

    public Integer getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    public BigDecimal getWinRate() {
        return winRate;
    }

    public void setWinRate(BigDecimal winRate) {
        this.winRate = winRate;
    }

    public BigDecimal getTotalPnl() {
        return totalPnl;
    }

    public void setTotalPnl(BigDecimal totalPnl) {
        this.totalPnl = totalPnl;
    }

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(BigDecimal maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnalyticsResponse that = (AnalyticsResponse) o;
        return Objects.equals(totalTrades, that.totalTrades) &&
               Objects.equals(winRate, that.winRate) &&
               Objects.equals(totalPnl, that.totalPnl) &&
               Objects.equals(maxDrawdown, that.maxDrawdown);
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalTrades, winRate, totalPnl, maxDrawdown);
    }

    @Override
    public String toString() {
        return "AnalyticsResponse{" +
                "totalTrades=" + totalTrades +
                ", winRate=" + winRate +
                ", totalPnl=" + totalPnl +
                ", maxDrawdown=" + maxDrawdown +
                '}';
    }
}
