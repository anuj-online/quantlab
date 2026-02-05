-- ============================================================================
-- QuantLab Database Schema - Flyway Migration V6
-- Adds status fields to trade and paper trade tables for lifecycle management
-- ============================================================================

-- ============================================================================
-- 1. Update trade_signal table
-- Add status field to track signal lifecycle
-- ============================================================================

ALTER TABLE trade_signal
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
CHECK (status IN ('PENDING', 'IGNORED', 'EXECUTED'));

-- Index for status queries
CREATE INDEX idx_trade_signal_status ON trade_signal(status);

-- Update comments
COMMENT ON TABLE trade_signal IS 'Generated signals with lifecycle status - signals are not trades until executed';

-- ============================================================================
-- 2. Update paper_trade table
-- Add status and exit_reason fields
-- ============================================================================

ALTER TABLE paper_trade
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
CHECK (status IN ('OPEN', 'CLOSED'));

ALTER TABLE paper_trade
ADD COLUMN exit_reason VARCHAR(20)
CHECK (exit_reason IN ('STOP_LOSS', 'TARGET', 'TIME', ' MANUAL'));

-- Index for status queries
CREATE INDEX idx_paper_trade_status ON paper_trade(status);
CREATE INDEX idx_paper_trade_exit_reason ON paper_trade(exit_reason);

-- Update comments
COMMENT ON TABLE paper_trade IS 'Paper trades with execution status - tracks both open and closed positions';

-- ============================================================================
-- 3. Add current_price field for market data updates
-- This will be used for real-time price tracking and PnL calculation
-- ============================================================================

ALTER TABLE paper_trade
ADD COLUMN current_price NUMERIC(15, 4);

-- Index for current price queries
CREATE INDEX idx_paper_trade_current_price ON paper_trade(current_price);

-- ============================================================================
-- 4. Add unrealized_pnl and unrealized_pnl_pct fields for open positions
-- ============================================================================

ALTER TABLE paper_trade
ADD COLUMN unrealized_pnl NUMERIC(15, 4);

ALTER TABLE paper_trade
ADD COLUMN unrealized_pnl_pct NUMERIC(10, 4);

-- Index for unrealized PnL queries
CREATE INDEX idx_paper_trade_unrealized_pnl ON paper_trade(unrealized_pnl);

-- ============================================================================
-- 5. Add r_multiple field for risk-reward tracking
-- ============================================================================

ALTER TABLE paper_trade
ADD COLUMN r_multiple NUMERIC(10, 4);

-- Index for R-multiple queries
CREATE INDEX idx_paper_trade_r_multiple ON paper_trade(r_multiple);

-- ============================================================================
-- Comments for new fields
-- ============================================================================
COMMENT ON COLUMN trade_signal.status IS 'Lifecycle status of the trade signal';
COMMENT ON COLUMN paper_trade.status IS 'Execution status of the paper trade';
COMMENT ON COLUMN paper_trade.exit_reason IS 'Reason for trade closure';
COMMENT ON COLUMN paper_trade.current_price IS 'Current market price for tracking';
COMMENT ON COLUMN paper_trade.unrealized_pnl IS 'Unrealized profit/loss for open positions';
COMMENT ON COLUMN paper_trade.unrealized_pnl_pct IS 'Unrealized PnL percentage for open positions';
COMMENT ON COLUMN paper_trade.r_multiple IS 'Risk-reward multiple (reward/risk)';