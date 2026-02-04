package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class RunStrategyRequest {

    @NotBlank(message = "Strategy code is required")
    @JsonProperty("strategyCode")
    private String strategyCode;

    @NotBlank(message = "Market is required")
    @JsonProperty("market")
    private String market;

    @NotBlank(message = "Start date is required")
    @JsonProperty("startDate")
    private String startDate;

    @NotBlank(message = "End date is required")
    @JsonProperty("endDate")
    private String endDate;

    @NotNull(message = "Parameters are required")
    @JsonProperty("params")
    private Map<String, Object> params;

    public RunStrategyRequest() {
    }

    public RunStrategyRequest(String strategyCode, String market, String startDate, String endDate, Map<String, Object> params) {
        this.strategyCode = strategyCode;
        this.market = market;
        this.startDate = startDate;
        this.endDate = endDate;
        this.params = params;
    }

    public String getStrategyCode() {
        return strategyCode;
    }

    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
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

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
