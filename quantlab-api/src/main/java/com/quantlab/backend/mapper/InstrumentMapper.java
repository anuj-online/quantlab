package com.quantlab.backend.mapper;

import com.quantlab.backend.dto.InstrumentResponse;
import com.quantlab.backend.entity.Instrument;

/**
 * Mapper for converting Instrument entities to DTOs.
 */
public final class InstrumentMapper {

    private InstrumentMapper() {
        // Private constructor to prevent instantiation
    }

    /**
     * Convert an Instrument entity to an InstrumentResponse DTO.
     *
     * @param instrument the instrument entity
     * @return the instrument response DTO
     */
    public static InstrumentResponse toDto(Instrument instrument) {
        if (instrument == null) {
            return null;
        }
        return new InstrumentResponse(
                instrument.getId(),
                instrument.getSymbol(),
                instrument.getMarket().name()
        );
    }
}
