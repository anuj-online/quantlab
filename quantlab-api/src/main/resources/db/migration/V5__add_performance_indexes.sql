-- =====================================================
-- Performance Optimization Indexes
-- Version: 5.0
-- Date: 2026-02-05
-- Purpose: Add indexes to optimize query performance for
--          strategy execution, screening, and analytics
-- =====================================================

-- 1. Composite index for instrument market+active queries
-- Optimizes: SELECT * FROM instrument WHERE market = ? AND active = true
-- Used by: StrategyRunService, ScreeningService for fetching active instruments
CREATE INDEX IF NOT EXISTS idx_instrument_market_active
ON instrument(market, active)
WHERE active = true;

-- 2. Index for candle queries by instrument and date
-- Optimizes: Screening queries and date range lookups
-- Used by: ScreeningService for recent candle lookups
-- Note: Partial index removed because CURRENT_DATE is not IMMUTABLE
CREATE INDEX IF NOT EXISTS idx_candle_instrument_date
ON candle(instrument_id, trade_date DESC);

-- 3. Covering index for analytics queries
-- Enables index-only scans for P&L aggregations
-- Used by: PaperTradeRepository for analytics (sum, count, avg)
CREATE INDEX IF NOT EXISTS idx_paper_trade_analytics
ON paper_trade(strategy_run_id, pnl, pnl_pct);

-- 4. Index for latest price lookups (live data preparation)
-- Optimizes: SELECT * FROM candle WHERE instrument_id = ? ORDER BY trade_date DESC LIMIT 1
-- Used by: Future live data integration for latest price queries
CREATE INDEX IF NOT EXISTS idx_candle_latest
ON candle(instrument_id, trade_date DESC);

-- 5. Screening date queries optimization
-- Optimizes: SELECT * FROM screening_results WHERE run_date = ? ORDER BY symbol, strategy_code
-- Used by: ScreeningService for fetching historical screening results
CREATE INDEX IF NOT EXISTS idx_screening_results_date_symbol
ON screening_results(run_date DESC, symbol, strategy_code);

-- =====================================================
-- Index Statistics and Monitoring
-- =====================================================

-- Comment for reference - check index usage with:
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE tablename IN ('candle', 'instrument', 'paper_trade', 'screening_results')
-- ORDER BY idx_scan DESC;
