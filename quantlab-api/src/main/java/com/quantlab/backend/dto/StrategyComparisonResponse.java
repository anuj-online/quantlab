package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for strategy comparison results.
 * Contains metrics for each strategy being compared.
 */
public class StrategyComparisonResponse {

    @JsonProperty("comparisonDate")
    private String comparisonDate;

    @JsonProperty("market")
    private String market;

    @JsonProperty("metrics")
    private List<StrategyMetrics> metrics;

    @JsonProperty("bestPerforming")
    private List<BestPerformingMetric> bestPerforming;

    public StrategyComparisonResponse() {
    }

    public StrategyComparisonResponse(String comparisonDate, String market,
                                     List<StrategyMetrics> metrics,
                                     List<BestPerformingMetric> bestPerforming) {
        this.comparisonDate = comparisonDate;
        this.market = market;
        this.metrics = metrics;
        this.bestPerforming = bestPerforming;
    }

    // Getters and Setters
    public String getComparisonDate() {
        return comparisonDate;
    }

    public void setComparisonDate(String comparisonDate) {
        this.comparisonDate = comparisonDate;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public List<StrategyMetrics> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<StrategyMetrics> metrics) {
        this.metrics = metrics;
    }

    public List<BestPerformingMetric> getBestPerforming() {
        return bestPerforming;
    }

    public void setBestPerforming(List<BestPerformingMetric> bestPerforming) {
        this.bestPerforming = bestPerforming;
    }

    /**
     * Inner class representing the best performing strategy for a specific metric.
     */
    public static class BestPerformingMetric {

        @JsonProperty("metric")
        private String metric;

        @JsonProperty("strategyCode")
        private String strategyCode;

        @JsonProperty("value")
        private Double value;

        public BestPerformingMetric() {
        }

        public BestPerformingMetric(String metric, String strategyCode, Double value) {
            this.metric = metric;
            this.strategyCode = strategyCode;
            this.value = value;
        }

        public String getMetric() {
            return metric;
        }

        public void setMetric(String metric) {
            this.metric = metric;
        }

        public String getStrategyCode() {
            return strategyCode;
        }

        public void setStrategyCode(String strategyCode) {
            this.strategyCode = strategyCode;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }
    }
}
