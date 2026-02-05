-- ============================================================================
-- QuantLab Database Schema - Flyway Migration V2
-- Creates screening_results table for storing daily screening outputs
-- ============================================================================

-- ============================================================================
-- Screening Results Table
-- Purpose: Store daily screening results from strategy scans
-- This table acts as a historical log of all signals generated across runs
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS screening_results_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE screening_results (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('screening_results_id_seq'),
    run_date        DATE         NOT NULL,
    symbol          VARCHAR(50)  NOT NULL,
    strategy_code   VARCHAR(50)  NOT NULL,
    signal_type     VARCHAR(10)  NOT NULL CHECK (signal_type IN ('BUY', 'SELL', 'HOLD')),
    entry           NUMERIC(15, 4),
    stop_loss       NUMERIC(15, 4),
    target          NUMERIC(15, 4),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common screening queries
-- Filter by run_date to get screening results for a specific day
CREATE INDEX idx_screening_results_run_date ON screening_results(run_date);

-- Filter by symbol to see all signals for a particular stock
CREATE INDEX idx_screening_results_symbol ON screening_results(symbol);

-- Filter by strategy_code to compare different strategy outputs
CREATE INDEX idx_screening_results_strategy_code ON screening_results(strategy_code);

-- Composite index for date + symbol queries (common pattern: "What signals did INFY generate today?")
CREATE INDEX idx_screening_results_date_symbol ON screening_results(run_date, symbol);

-- Composite index for date + strategy queries (common pattern: "What signals did the RSI strategy generate today?")
CREATE INDEX idx_screening_results_date_strategy ON screening_results(run_date, strategy_code);

-- Index for signal type filtering (e.g., show only BUY signals)
CREATE INDEX idx_screening_results_signal_type ON screening_results(signal_type);

-- ============================================================================
-- Comments for documentation
-- ============================================================================
COMMENT ON TABLE screening_results IS 'Historical log of daily screening results from strategy scans - stores all generated signals for analysis and review';
COMMENT ON COLUMN screening_results.run_date IS 'The date for which the screening was performed (typically the latest trading day)';
COMMENT ON COLUMN screening_results.symbol IS 'The trading symbol that generated the signal';
COMMENT ON COLUMN screening_results.strategy_code IS 'The strategy that generated this signal (e.g., RSI_20x80, MACD_CROSS, SMA_CROSS)';
COMMENT ON COLUMN screening_results.signal_type IS 'Type of signal: BUY, SELL, or HOLD';
COMMENT ON COLUMN screening_results.entry IS 'Recommended entry price for the signal';
COMMENT ON COLUMN screening_results.stop_loss IS 'Recommended stop-loss price for risk management';
COMMENT ON COLUMN screening_results.target IS 'Recommended target price for profit taking';
COMMENT ON COLUMN screening_results.created_at IS 'Timestamp when this screening result was recorded';
