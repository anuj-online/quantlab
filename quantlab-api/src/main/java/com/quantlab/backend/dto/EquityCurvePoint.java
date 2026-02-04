package com.quantlab.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Objects;

public class EquityCurvePoint {

    @JsonProperty("date")
    private String date;

    @JsonProperty("equity")
    private BigDecimal equity;

    public EquityCurvePoint() {
    }

    public EquityCurvePoint(String date, BigDecimal equity) {
        this.date = date;
        this.equity = equity;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public BigDecimal getEquity() {
        return equity;
    }

    public void setEquity(BigDecimal equity) {
        this.equity = equity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EquityCurvePoint that = (EquityCurvePoint) o;
        return Objects.equals(date, that.date) &&
               Objects.equals(equity, that.equity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, equity);
    }

    @Override
    public String toString() {
        return "EquityCurvePoint{" +
                "date='" + date + '\'' +
                ", equity=" + equity +
                '}';
    }
}
