package com.quantlab.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * Request DTO for comparing multiple strategies.
 * Contains the list of strategies to compare along with market and date range.
 */
public class StrategyComparisonRequest {

    @NotEmpty(message = "Strategy codes cannot be empty")
    private List<@NotBlank String> strategyCodes;

    @NotBlank(message = "Market is required")
    @Pattern(regexp = "INDIA|US", message = "Market must be either 'INDIA' or 'US'")
    private String market;

    @NotBlank(message = "Start date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Start date must be in yyyy-MM-dd format")
    private String startDate;

    @NotBlank(message = "End date is required")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "End date must be in yyyy-MM-dd format")
    private String endDate;

    public StrategyComparisonRequest() {
    }

    public StrategyComparisonRequest(List<String> strategyCodes, String market, String startDate, String endDate) {
        this.strategyCodes = strategyCodes;
        this.market = market;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters
    public List<String> getStrategyCodes() {
        return strategyCodes;
    }

    public void setStrategyCodes(List<String> strategyCodes) {
        this.strategyCodes = strategyCodes;
    }

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
