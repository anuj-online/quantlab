package com.quantlab.backend.marketdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.quantlab.backend.config.YahooFinanceConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for fetching data from Yahoo Finance API.
 * <p>
 * API Endpoint: https://query1.finance.yahoo.com/v8/finance/chart/{SYMBOL}
 * <p>
 * Response format:
 * - timestamp[]: Unix timestamps in seconds
 * - open[], high[], low[], close[], volume[]: OHLCV data
 */
@Service
public class YahooFinanceApiService {

    private static final Logger log = LoggerFactory.getLogger(YahooFinanceApiService.class);

    private final RestTemplate restTemplate;
    private final YahooFinanceConfiguration config;

    public YahooFinanceApiService(RestTemplateBuilder restTemplateBuilder,
                                  YahooFinanceConfiguration config) {
        Duration timeout = Duration.ofMillis(config.getTimeout());
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
        this.config = config;
    }

    /**
     * Fetch daily candles for a symbol within a date range.
     * Uses "1d" interval and "3mo" range to get recent data.
     * <p>
     * For more precise date ranges, use period1/period2 parameters with Unix timestamps.
     *
     * @param symbol The trading symbol (e.g., "RELIANCE.NS" for NSE, "AAPL" for US)
     * @param from   Start date (inclusive)
     * @param to     End date (inclusive)
     * @return List of YahooCandleData, empty list if fetch fails
     */
    @Cacheable(value = "yahooCandles", key = "#symbol + '-' + #from + '-' + #to",
               unless = "#result.isEmpty()")
    public List<YahooCandleData> fetchDailyCandles(String symbol, LocalDate from, LocalDate to) {
        String url = buildUrl(symbol, from, to);

        try {
            log.debug("Fetching Yahoo Finance data for {} from {} to {}", symbol, from, to);
            YahooChartResponse response = restTemplate.getForObject(url, YahooChartResponse.class);

            if (response == null || response.chart == null || response.chart.result == null ||
                    response.chart.result.isEmpty()) {
                log.warn("No data returned from Yahoo Finance for {}", symbol);
                return new ArrayList<>();
            }

            YahooChartResult result = response.chart.result.get(0);
            return mapToCandleData(result, from, to);

        } catch (Exception e) {
            log.warn("Failed to fetch Yahoo Finance data for {}: {}", symbol, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Build the Yahoo Finance API URL with date range parameters.
     */
    private String buildUrl(String symbol, LocalDate from, LocalDate to) {
        // Convert dates to Unix timestamps in seconds
        long fromTimestamp = from.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long toTimestamp = to.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toEpochSecond();

        return String.format("%s/%s?interval=1d&period1=%d&period2=%d",
                config.getBaseUrl(), symbol, fromTimestamp, toTimestamp);
    }

    /**
     * Map Yahoo Finance API response to YahooCandleData list.
     * Filters by date range and handles null values.
     */
    private List<YahooCandleData> mapToCandleData(YahooChartResult result, LocalDate from, LocalDate to) {
        List<YahooCandleData> candles = new ArrayList<>();

        if (result.timestamp == null || result.indicator == null ||
                result.indicator.quote == null) {
            return candles;
        }

        YahooQuote quote = result.indicator.quote;
        int[] timestamps = result.timestamp;

        for (int i = 0; i < timestamps.length; i++) {
            LocalDate tradeDate = LocalDate.ofInstant(
                    Instant.ofEpochSecond(timestamps[i]), ZoneId.systemDefault());

            // Filter by date range
            if (tradeDate.isBefore(from) || tradeDate.isAfter(to)) {
                continue;
            }

            // Skip candles with missing OHLC data
            if (quote.open == null || quote.high == null || quote.low == null ||
                    quote.close == null || i >= quote.open.length) {
                continue;
            }

            YahooCandleData candle = new YahooCandleData(
                    tradeDate,
                    quote.open[i],
                    quote.high[i],
                    quote.low[i],
                    quote.close[i],
                    quote.volume != null && i < quote.volume.length ? quote.volume[i] : 0L
            );

            candles.add(candle);
        }

        log.debug("Mapped {} candles from Yahoo Finance response", candles.size());
        return candles;
    }

    /**
     * Check if Yahoo Finance API is available.
     */
    public boolean isAvailable() {
        try {
            // Simple health check with a popular stock
            String url = String.format("%s/AAPL?interval=1d&range=1d", config.getBaseUrl());
            YahooChartResponse response = restTemplate.getForObject(url, YahooChartResponse.class);
            return response != null && response.chart != null &&
                   response.chart.result != null && !response.chart.result.isEmpty();
        } catch (Exception e) {
            log.debug("Yahoo Finance API unavailable: {}", e.getMessage());
            return false;
        }
    }

    // ==================== DTOs for Yahoo Finance API Response ====================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooChartResponse(
            @JsonProperty("chart") YahooChart chart
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooChart(
            @JsonProperty("result") List<YahooChartResult> result
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooChartResult(
            @JsonProperty("timestamp") int[] timestamp,
            @JsonProperty("indicators") YahooIndicators indicator
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooIndicators(
            @JsonProperty("quote") YahooQuote quote
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record YahooQuote(
            @JsonProperty("open") BigDecimal[] open,
            @JsonProperty("high") BigDecimal[] high,
            @JsonProperty("low") BigDecimal[] low,
            @JsonProperty("close") BigDecimal[] close,
            @JsonProperty("volume") Long[] volume
    ) {}

    /**
     * Candle data from Yahoo Finance.
     * Temporary holder before conversion to Candle entity.
     */
    public record YahooCandleData(
            LocalDate tradeDate,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            long volume
    ) {}
}
