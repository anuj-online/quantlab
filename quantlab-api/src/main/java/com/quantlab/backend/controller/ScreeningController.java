package com.quantlab.backend.controller;

import com.quantlab.backend.dto.ScreeningRequest;
import com.quantlab.backend.dto.ScreeningResponse;
import com.quantlab.backend.service.ScreeningService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for screening operations.
 * <p>
 * Screening allows traders to scan the market for actionable signals generated
 * by multiple strategies. Unlike backtesting (StrategyRunController), screening
 * focuses on point-in-time analysis for the latest trading opportunities.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Run multiple strategies in a single request</li>
 *   <li>Filter by market (INDIA/US)</li>
 *   <li>Get historical screening results</li>
 *   <li>Results are grouped by strategy for easy comparison</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/screening")
public class ScreeningController {

    private static final Logger log = LoggerFactory.getLogger(ScreeningController.class);

    private final ScreeningService screeningService;

    /**
     * Constructor-based dependency injection.
     *
     * @param screeningService the screening service
     */
    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    /**
     * Run screening for specified strategies on a given date.
     * <p>
     * POST /api/v1/screening/run
     * </p>
     * <p>
     * Request body:
     * <pre>
     * {
     *   "strategyCodes": ["EOD_BREAKOUT", "SMA_CROSSOVER"],
     *   "date": "2024-01-15",
     *   "market": "INDIA"  // optional
     * }
     * </pre>
     * </p>
     * <p>
     * Response:
     * <pre>
     * {
     *   "screeningDate": "2024-01-15",
     *   "market": "INDIA",
     *   "signalsByStrategy": {
     *     "EOD_BREAKOUT": [
     *       {
     *         "symbol": "INFY",
     *         "signalDate": "2024-01-15",
     *         "side": "BUY",
     *         "entryPrice": 1450.50,
     *         "stopLoss": 1420.00,
     *         "target": 1520.00,
     *         "strategyCode": "EOD_BREAKOUT"
     *       }
     *     ],
     *     "SMA_CROSSOVER": [...]
     *   },
     *   "totalSignals": 5,
     *   "timestamp": "2024-01-15T18:30:00"
     * }
     * </pre>
     * </p>
     *
     * @param request the screening request
     * @return screening response with signals grouped by strategy
     */
    @PostMapping("/run")
    public ResponseEntity<ScreeningResponse> runScreening(@Valid @RequestBody ScreeningRequest request) {
        log.info("Received screening request: {}", request);

        try {
            ScreeningResponse response = screeningService.runScreening(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Invalid screening request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error running screening: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get historical screening results for a specific date.
     * <p>
     * GET /api/v1/screening/results?date=2024-01-15
     * </p>
     * <p>
     * This endpoint retrieves previously saved screening results without
     * re-running the strategies. Useful for viewing past screening results.
     * </p>
     *
     * @param date the screening date (format: yyyy-MM-dd)
     * @return screening response with historical signals
     */
    @GetMapping("/results")
    public ResponseEntity<ScreeningResponse> getHistoricalScreening(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Fetching historical screening results for date: {}", date);

        try {
            ScreeningResponse response = screeningService.getHistoricalScreening(date);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching historical screening: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get the most recent screening date available.
     * <p>
     * GET /api/v1/screening/latest-date
     * </p>
     * <p>
     * Returns the date of the most recent screening run.
     * Useful for default date selection in the UI.
     * </p>
     *
     * @return the most recent screening date, or 204 No Content if no screenings exist
     */
    @GetMapping("/latest-date")
    public ResponseEntity<LocalDate> getLatestScreeningDate() {
        log.info("Fetching latest screening date");

        try {
            LocalDate latestDate = screeningService.getMostRecentScreeningDate();
            if (latestDate == null) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(latestDate);
        } catch (Exception e) {
            log.error("Error fetching latest screening date: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available screening dates.
     * <p>
     * GET /api/v1/screening/dates
     * </p>
     * <p>
     * Returns a list of all dates for which screening results are available.
     * Useful for populating date filters in the UI.
     * </p>
     *
     * @return list of screening dates in descending order (most recent first)
     */
    @GetMapping("/dates")
    public ResponseEntity<List<LocalDate>> getAvailableScreeningDates() {
        log.info("Fetching available screening dates");

        try {
            List<LocalDate> dates = screeningService.getAvailableScreeningDates();
            return ResponseEntity.ok(dates);
        } catch (Exception e) {
            log.error("Error fetching available screening dates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get screening results for a specific strategy on a specific date.
     * <p>
     * GET /api/v1/screening/results/{strategyCode}?date=2024-01-15
     * </p>
     * <p>
     * Returns screening results filtered by a single strategy.
     * </p>
     *
     * @param strategyCode the strategy code
     * @param date the screening date
     * @return screening response with signals for the specified strategy
     */
    @GetMapping("/results/{strategyCode}")
    public ResponseEntity<ScreeningResponse> getStrategyScreening(
            @PathVariable("strategyCode") String strategyCode,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Fetching screening results for strategy: {} on date: {}", strategyCode, date);

        try {
            ScreeningResponse response = screeningService.getHistoricalScreening(date);

            // Filter by strategy code
            if (response.getSignalsByStrategy() != null &&
                    response.getSignalsByStrategy().containsKey(strategyCode)) {

                ScreeningResponse filteredResponse = new ScreeningResponse();
                filteredResponse.setScreeningDate(response.getScreeningDate());
                filteredResponse.setMarket(response.getMarket());
                filteredResponse.setSignalsByStrategy(
                        Map.of(strategyCode, response.getSignalsByStrategy().get(strategyCode))
                );
                filteredResponse.setTotalSignals(
                        response.getSignalsByStrategy().get(strategyCode).size()
                );
                filteredResponse.setTimestamp(response.getTimestamp());

                return ResponseEntity.ok(filteredResponse);
            }

            // Strategy not found in results
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error fetching strategy screening: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
