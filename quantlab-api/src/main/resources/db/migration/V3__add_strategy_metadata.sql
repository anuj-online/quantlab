-- ============================================================================
-- QuantLab Database Schema - Flyway Migration V3
-- Adds metadata fields to the strategy table for enhanced strategy information
-- ============================================================================

-- Add supports_screening column to indicate if strategy supports screening mode
ALTER TABLE strategy ADD COLUMN supports_screening BOOLEAN NOT NULL DEFAULT TRUE;

-- Add min_lookback_days column to indicate minimum data required
ALTER TABLE strategy ADD COLUMN min_lookback_days INTEGER NOT NULL DEFAULT 20;

-- Add comments for documentation
COMMENT ON COLUMN strategy.supports_screening IS 'Indicates whether the strategy supports screening mode (true for all strategies)';
COMMENT ON COLUMN strategy.min_lookback_days IS 'Minimum number of days of historical data required for this strategy to generate signals';
