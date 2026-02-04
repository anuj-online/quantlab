package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class InstrumentResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("market")
    private String market;

    public InstrumentResponse() {
    }

    public InstrumentResponse(Long id, String symbol, String market) {
        this.id = id;
        this.symbol = symbol;
        this.market = market;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstrumentResponse that = (InstrumentResponse) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(symbol, that.symbol) &&
               Objects.equals(market, that.market);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, symbol, market);
    }

    @Override
    public String toString() {
        return "InstrumentResponse{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", market='" + market + '\'' +
                '}';
    }
}
