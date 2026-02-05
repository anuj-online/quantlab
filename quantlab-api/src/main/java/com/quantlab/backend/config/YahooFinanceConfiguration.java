package com.quantlab.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Yahoo Finance integration.
 */
@Configuration
@ConfigurationProperties(prefix = "marketdata.yahoo")
public class YahooFinanceConfiguration {

    /**
     * Base URL for Yahoo Finance API.
     */
    private String baseUrl = "https://query1.finance.yahoo.com/v8/finance/chart";

    /**
     * Connection timeout in milliseconds.
     */
    private int timeout = 3000;

    /**
     * Number of retries on failure.
     */
    private int retryCount = 1;

    /**
     * Whether caching is enabled.
     */
    private boolean cacheEnabled = true;

    /**
     * Cache TTL in minutes.
     */
    private int cacheTtlMinutes = 15;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public int getCacheTtlMinutes() {
        return cacheTtlMinutes;
    }

    public void setCacheTtlMinutes(int cacheTtlMinutes) {
        this.cacheTtlMinutes = cacheTtlMinutes;
    }
}
