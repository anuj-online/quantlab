package com.quantlab.backend.dto;

import java.util.Map;

public class VoteResult {
    public final int buyVotes;
    public final int totalStrategies;
    public final double confidenceScore;
    public final Map<String, String> strategyVotes;

    public VoteResult(int buyVotes, int totalStrategies, double confidenceScore, Map<String, String> strategyVotes) {
        this.buyVotes = buyVotes;
        this.totalStrategies = totalStrategies;
        this.confidenceScore = confidenceScore;
        this.strategyVotes = strategyVotes;
    }
}