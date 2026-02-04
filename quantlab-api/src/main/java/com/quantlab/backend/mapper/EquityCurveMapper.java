package com.quantlab.backend.mapper;

import com.quantlab.backend.dto.EquityCurvePoint;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mapper for creating equity curve DTOs.
 */
public final class EquityCurveMapper {

    private EquityCurveMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Create an EquityCurvePoint DTO from a date and equity value.
     *
     * @param date   the date
     * @param equity the equity value
     * @return the equity curve point DTO
     */
    public static EquityCurvePoint toDto(LocalDate date, BigDecimal equity) {
        if (date == null || equity == null) {
            return null;
        }
        return new EquityCurvePoint(date.toString(), equity);
    }
}
