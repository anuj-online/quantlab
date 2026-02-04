package com.quantlab.backend.mapper;

import com.quantlab.backend.dto.PaperTradeResponse;
import com.quantlab.backend.entity.PaperTrade;

/**
 * Mapper for converting PaperTrade entities to DTOs.
 */
public final class PaperTradeMapper {

    private PaperTradeMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert a PaperTrade entity to a PaperTradeResponse DTO.
     *
     * @param paperTrade the paper trade entity
     * @return the paper trade response DTO
     */
    public static PaperTradeResponse toDto(PaperTrade paperTrade) {
        if (paperTrade == null) {
            return null;
        }
        return new PaperTradeResponse(
                paperTrade.getInstrument().getSymbol(),
                paperTrade.getEntryDate().toString(),
                paperTrade.getExitDate().toString(),
                paperTrade.getEntryPrice(),
                paperTrade.getExitPrice(),
                paperTrade.getQuantity(),
                paperTrade.getPnl(),
                paperTrade.getPnlPct()
        );
    }
}
