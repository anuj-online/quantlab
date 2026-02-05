package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * DTO representing performance metrics for a trading strategy.
 * Contains comprehensive statistics calculated from backtesting results.
 */
public class StrategyMetrics {

    @JsonProperty("strategyCode")
    private String strategyCode;

    @JsonProperty("strategyName")
    private String strategyName;

    @JsonProperty("totalTrades")
    private Integer totalTrades;

    @JsonProperty("winRate")
    private Double winRate;

    @JsonProperty("avgReturn")
    private Double avgReturn;

    @JsonProperty("totalPnl")
    private Double totalPnl;

    @JsonProperty("maxDrawdown")
    private Double maxDrawdown;

    @JsonProperty("sharpeRatio")
    private Double sharpeRatio;

    @JsonProperty("sortinoRatio")
    private Double sortinoRatio;

    @JsonProperty("avgWin")
    private Double avgWin;

    @JsonProperty("avgLoss")
    private Double avgLoss;

    @JsonProperty("profitFactor")
    private Double profitFactor;

    public StrategyMetrics() {
    }

    public StrategyMetrics(String strategyCode, String strategyName, Integer totalTrades,
                          Double winRate, Double avgReturn, Double totalPnl, Double maxDrawdown,
                          Double sharpeRatio, Double sortinoRatio, Double avgWin,
                          Double avgLoss, Double profitFactor) {
        this.strategyCode = strategyCode;
        this.strategyName = strategyName;
        this.totalTrades = totalTrades;
        this.winRate = winRate;
        this.avgReturn = avgReturn;
        this.totalPnl = totalPnl;
        this.maxDrawdown = maxDrawdown;
        this.sharpeRatio = sharpeRatio;
        this.sortinoRatio = sortinoRatio;
        this.avgWin = avgWin;
        this.avgLoss = avgLoss;
        this.profitFactor = profitFactor;
    }

    // Getters and Setters
    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public Integer getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    public Double getWinRate() {
        return winRate;
    }

    public void setWinRate(Double winRate) {
        this.winRate = winRate;
    }

    public Double getAvgReturn() {
        return avgReturn;
    }

    public void setAvgReturn(Double avgReturn) {
        this.avgReturn = avgReturn;
    }

    public Double getTotalPnl() {
        return totalPnl;
    }

    public void setTotalPnl(Double totalPnl) {
        this.totalPnl = totalPnl;
    }

    public Double getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(Double maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public Double getSharpeRatio() {
        return sharpeRatio;
    }

    public void setSharpeRatio(Double sharpeRatio) {
        this.sharpeRatio = sharpeRatio;
    }

    public Double getSortinoRatio() {
        return sortinoRatio;
    }

    public void setSortinoRatio(Double sortinoRatio) {
        this.sortinoRatio = sortinoRatio;
    }

    public Double getAvgWin() {
        return avgWin;
    }

    public void setAvgWin(Double avgWin) {
        this.avgWin = avgWin;
    }

    public Double getAvgLoss() {
        return avgLoss;
    }

    public void setAvgLoss(Double avgLoss) {
        this.avgLoss = avgLoss;
    }

    public Double getProfitFactor() {
        return profitFactor;
    }

    public void setProfitFactor(Double profitFactor) {
        this.profitFactor = profitFactor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StrategyMetrics that = (StrategyMetrics) o;
        return Objects.equals(strategyCode, that.strategyCode) &&
               Objects.equals(strategyName, that.strategyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategyCode, strategyName);
    }

    @Override
    public String toString() {
        return "StrategyMetrics{" +
                "strategyCode='" + strategyCode + '\'' +
                ", strategyName='" + strategyName + '\'' +
                ", totalTrades=" + totalTrades +
                ", winRate=" + winRate +
                ", avgReturn=" + avgReturn +
                ", totalPnl=" + totalPnl +
                ", maxDrawdown=" + maxDrawdown +
                ", sharpeRatio=" + sharpeRatio +
                ", profitFactor=" + profitFactor +
                '}';
    }
}
