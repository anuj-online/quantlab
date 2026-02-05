package com.quantlab.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for getting metrics for a single strategy.
 * Used by GET /api/v1/strategies/{code}/metrics endpoint.
 */
public class StrategyMetricsRequest {

    @NotBlank(message = "Market is required")
    @Pattern(regexp = "INDIA|US", message = "Market must be either 'INDIA' or 'US'")
    private String market;

    @NotBlank(message = "Start date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Start date must be in yyyy-MM-dd format")
    private String startDate;

    @NotBlank(message = "End date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "End date must be in yyyy-MM-dd format")
    private String endDate;

    public StrategyMetricsRequest() {
    }

    public StrategyMetricsRequest(String market, String startDate, String endDate) {
        this.market = market;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
