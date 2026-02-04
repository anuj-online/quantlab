package com.quantlab.backend.mapper;

import com.quantlab.backend.dto.TradeSignalResponse;
import com.quantlab.backend.entity.TradeSignal;

/**
 * Mapper for converting TradeSignal entities to DTOs.
 */
public final class TradeSignalMapper {

    private TradeSignalMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert a TradeSignal entity to a TradeSignalResponse DTO.
     *
     * @param tradeSignal the trade signal entity
     * @return the trade signal response DTO
     */
    public static TradeSignalResponse toDto(TradeSignal tradeSignal) {
        if (tradeSignal == null) {
            return null;
        }
        return new TradeSignalResponse(
                tradeSignal.getInstrument().getSymbol(),
                tradeSignal.getSignalDate().toString(),
                tradeSignal.getSide().name(),
                tradeSignal.getEntryPrice(),
                tradeSignal.getStopLoss(),
                tradeSignal.getTargetPrice(),
                tradeSignal.getQuantity()
        );
    }
}
