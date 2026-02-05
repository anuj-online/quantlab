package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for screening operations.
 * Contains a list of strategy codes to run screening for and the date to screen.
 */
public class ScreeningRequest {

    /**
     * List of strategy codes to run screening for (e.g., ["EOD_BREAKOUT", "SMA_CROSSOVER"])
     */
    @JsonProperty("strategyCodes")
    @NotNull(message = "Strategy codes are required")
    private List<String> strategyCodes;

    /**
     * The date to run screening for (typically the latest trading day)
     * Format: yyyy-MM-dd
     */
    @JsonProperty("date")
    @NotNull(message = "Screening date is required")
    private LocalDate date;

    /**
     * Optional market filter to restrict screening to specific market (INDIA or US)
     * If null, screens across all active instruments
     */
    @JsonProperty("market")
    private String market;

    public ScreeningRequest() {
    }

    public ScreeningRequest(List<String> strategyCodes, LocalDate date, String market) {
        this.strategyCodes = strategyCodes;
        this.date = date;
        this.market = market;
    }

    public List<String> getStrategyCodes() {
        return strategyCodes;
    }

    public void setStrategyCodes(List<String> strategyCodes) {
        this.strategyCodes = strategyCodes;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    @Override
    public String toString() {
        return "ScreeningRequest{" +
                "strategyCodes=" + strategyCodes +
                ", date=" + date +
                ", market='" + market + '\'' +
                '}';
    }
}
