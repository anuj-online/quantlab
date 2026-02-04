package com.quantlab.backend.controller;

import com.quantlab.backend.dto.InstrumentResponse;
import com.quantlab.backend.service.InstrumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for Instrument management.
 * Handles API endpoints for retrieving instrument information.
 */
@RestController
@RequestMapping("/api/v1")
public class InstrumentController {

    private final InstrumentService instrumentService;

    /**
     * Constructor-based dependency injection.
     *
     * @param instrumentService the instrument service
     */
    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    /**
     * Get list of instruments with optional filtering.
     *
     * @param market optional market filter (INDIA or US)
     * @param active optional active status filter (true or false)
     * @return list of instruments matching the criteria
     */
    @GetMapping("/instruments")
    public ResponseEntity<List<InstrumentResponse>> getInstruments(
            @RequestParam(value = "market", required = false) String market,
            @RequestParam(value = "active", required = false) Boolean active) {

        List<InstrumentResponse> instruments = instrumentService.getInstruments(market, active);
        return ResponseEntity.ok(instruments);
    }
}
