package com.quantlab.backend.service;

import com.quantlab.backend.dto.InstrumentResponse;
import com.quantlab.backend.entity.Instrument;
import com.quantlab.backend.entity.MarketType;
import com.quantlab.backend.mapper.InstrumentMapper;
import com.quantlab.backend.repository.InstrumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for instrument-related operations.
 */
@Service
@Transactional(readOnly = true)
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    /**
     * Get all instruments for a specific market.
     *
     * @param market the market type (INDIA or US)
     * @return list of instrument response DTOs
     */
    public List<InstrumentResponse> getInstrumentsByMarket(MarketType market) {
        List<Instrument> instruments = instrumentRepository.findByMarket(market);
        return instruments.stream()
                .map(InstrumentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all active instruments for a specific market.
     *
     * @param market the market type (INDIA or US)
     * @return list of active instrument response DTOs
     */
    public List<InstrumentResponse> getActiveInstrumentsByMarket(MarketType market) {
        List<Instrument> instruments = instrumentRepository.findByMarketAndActiveOrderBySymbolAsc(market, true);
        return instruments.stream()
                .map(InstrumentMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Find an instrument by its symbol.
     *
     * @param symbol the trading symbol
     * @return the instrument if found, null otherwise
     */
    public InstrumentResponse getInstrumentBySymbol(String symbol) {
        Instrument instrument = instrumentRepository.findBySymbol(symbol);
        return instrument != null ? InstrumentMapper.toDto(instrument) : null;
    }

    /**
     * Get instruments with optional filtering by market and active status.
     *
     * @param market optional market filter (INDIA or US)
     * @param active optional active status filter (true or false)
     * @return list of instrument response DTOs
     */
    public List<InstrumentResponse> getInstruments(String market, Boolean active) {
        List<Instrument> instruments;

        if (market != null && active != null) {
            // Both filters specified
            MarketType marketType = MarketType.valueOf(market.toUpperCase());
            instruments = instrumentRepository.findByMarketAndActiveOrderBySymbolAsc(marketType, active);
        } else if (market != null) {
            // Only market specified
            MarketType marketType = MarketType.valueOf(market.toUpperCase());
            instruments = instrumentRepository.findByMarket(marketType);
        } else if (active != null) {
            // Only active specified
            instruments = instrumentRepository.findByActiveOrderBySymbolAsc(active);
        } else {
            // No filters - return all
            instruments = instrumentRepository.findAll();
        }

        return instruments.stream()
                .map(InstrumentMapper::toDto)
                .collect(Collectors.toList());
    }
}
