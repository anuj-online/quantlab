package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class RunStrategyResponse {

    @JsonProperty("strategyRunId")
    private Long strategyRunId;

    @JsonProperty("status")
    private String status;

    public RunStrategyResponse() {
    }

    public RunStrategyResponse(Long strategyRunId, String status) {
        this.strategyRunId = strategyRunId;
        this.status = status;
    }

    public Long getStrategyRunId() {
        return strategyRunId;
    }

    public void setStrategyRunId(Long strategyRunId) {
        this.strategyRunId = strategyRunId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunStrategyResponse that = (RunStrategyResponse) o;
        return Objects.equals(strategyRunId, that.strategyRunId) &&
               Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategyRunId, status);
    }

    @Override
    public String toString() {
        return "RunStrategyResponse{" +
                "strategyRunId=" + strategyRunId +
                ", status='" + status + '\'' +
                '}';
    }
}
