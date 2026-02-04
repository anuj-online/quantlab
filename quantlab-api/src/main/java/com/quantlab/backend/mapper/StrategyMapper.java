package com.quantlab.backend.mapper;

import com.quantlab.backend.dto.StrategyResponse;
import com.quantlab.backend.entity.Strategy;

/**
 * Mapper for converting Strategy entities to DTOs.
 */
public final class StrategyMapper {

    private StrategyMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert a Strategy entity to a StrategyResponse DTO.
     *
     * @param strategy the strategy entity
     * @return the strategy response DTO
     */
    public static StrategyResponse toDto(Strategy strategy) {
        if (strategy == null) {
            return null;
        }
        return new StrategyResponse(
                strategy.getId(),
                strategy.getCode(),
                strategy.getName()
        );
    }
}
