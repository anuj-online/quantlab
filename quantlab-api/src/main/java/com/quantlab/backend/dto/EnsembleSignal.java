package com.quantlab.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class EnsembleSignal {
    private UUID ensembleId;
    private String symbol;
    private LocalDate signalDate;
    private String side;
    private BigDecimal entryPrice;
    private BigDecimal stopLoss;
    private BigDecimal targetPrice;
    private int voteScore;
    private int totalStrategies;
    private double confidenceScore;
    private Map<String, String> strategyVotes;

    // Ranking fields
    private Double rankScore;
    private Double rMultiple;
    private Double liquidityScore;
    private Double volatilityFit;

    // Constructors
    public EnsembleSignal() {}

    public EnsembleSignal(UUID ensembleId, String symbol, LocalDate signalDate, String side,
                         BigDecimal entryPrice, BigDecimal stopLoss, BigDecimal targetPrice,
                         int voteScore, int totalStrategies, double confidenceScore,
                         Map<String, String> strategyVotes) {
        this.ensembleId = ensembleId;
        this.symbol = symbol;
        this.signalDate = signalDate;
        this.side = side;
        this.entryPrice = entryPrice;
        this.stopLoss = stopLoss;
        this.targetPrice = targetPrice;
        this.voteScore = voteScore;
        this.totalStrategies = totalStrategies;
        this.confidenceScore = confidenceScore;
        this.strategyVotes = strategyVotes;
    }

    // Getters and Setters
    public UUID getEnsembleId() { return ensembleId; }
    public void setEnsembleId(UUID ensembleId) { this.ensembleId = ensembleId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public LocalDate getSignalDate() { return signalDate; }
    public void setSignalDate(LocalDate signalDate) { this.signalDate = signalDate; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public BigDecimal getEntryPrice() { return entryPrice; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }

    public BigDecimal getStopLoss() { return stopLoss; }
    public void setStopLoss(BigDecimal stopLoss) { this.stopLoss = stopLoss; }

    public BigDecimal getTargetPrice() { return targetPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }

    public int getVoteScore() { return voteScore; }
    public void setVoteScore(int voteScore) { this.voteScore = voteScore; }

    public int getTotalStrategies() { return totalStrategies; }
    public void setTotalStrategies(int totalStrategies) { this.totalStrategies = totalStrategies; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public Map<String, String> getStrategyVotes() { return strategyVotes; }
    public void setStrategyVotes(Map<String, String> strategyVotes) { this.strategyVotes = strategyVotes; }

    public Double getRankScore() { return rankScore; }
    public void setRankScore(Double rankScore) { this.rankScore = rankScore; }

    public Double getRMultiple() { return rMultiple; }
    public void setRMultiple(Double rMultiple) { this.rMultiple = rMultiple; }

    public Double getLiquidityScore() { return liquidityScore; }
    public void setLiquidityScore(Double liquidityScore) { this.liquidityScore = liquidityScore; }

    public Double getVolatilityFit() { return volatilityFit; }
    public void setVolatilityFit(Double volatilityFit) { this.volatilityFit = volatilityFit; }
}