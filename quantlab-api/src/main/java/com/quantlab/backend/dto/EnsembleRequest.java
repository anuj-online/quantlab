package com.quantlab.backend.dto;

import java.time.LocalDate;
import java.util.List;

public class EnsembleRequest {
    private List<String> strategyCodes;
    private LocalDate date;
    private String market; // INDIA or US

    // Constructors
    public EnsembleRequest() {}

    public EnsembleRequest(List<String> strategyCodes, LocalDate date, String market) {
        this.strategyCodes = strategyCodes;
        this.date = date;
        this.market = market;
    }

    // Getters and Setters
    public List<String> getStrategyCodes() { return strategyCodes; }
    public void setStrategyCodes(List<String> strategyCodes) { this.strategyCodes = strategyCodes; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getMarket() { return market; }
    public void setMarket(String market) { this.market = market; }
}