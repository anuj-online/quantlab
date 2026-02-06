-- Add intelligence layer fields for ensemble voting, signal ranking, and capital allocation
-- This combines V7, V8, V9 from the original plan for efficiency

-- Add intelligence fields to trade_signal table
ALTER TABLE trade_signal
ADD COLUMN ensemble_id UUID,
ADD COLUMN strategy_votes JSONB,
ADD COLUMN vote_score INT,
ADD COLUMN confidence_score FLOAT,
ADD COLUMN rank_score FLOAT,
ADD COLUMN r_multiple FLOAT,
ADD COLUMN liquidity_score FLOAT,
ADD COLUMN volatility_fit FLOAT,
ADD COLUMN strategy_win_rate FLOAT;

-- Add indices for efficient querying
CREATE INDEX idx_trade_signal_ensemble_id ON trade_signal(ensemble_id);
CREATE INDEX idx_trade_signal_rank_score ON trade_signal(rank_score DESC);
CREATE INDEX idx_trade_signal_confidence_score ON trade_signal(confidence_score DESC);
CREATE INDEX idx_trade_signal_status_date ON trade_signal(status, signal_date);

-- Create capital allocation tables
CREATE TABLE capital_allocation_snapshot (
    id BIGSERIAL PRIMARY KEY,
    run_date DATE NOT NULL,
    total_capital DECIMAL(19,2) NOT NULL,
    deployed_capital DECIMAL(19,2) NOT NULL,
    free_cash DECIMAL(19,2) NOT NULL,
    expected_r_multiple DECIMAL(10,4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE capital_allocation_position (
    id BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT REFERENCES capital_allocation_snapshot(id),
    symbol VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    capital_used DECIMAL(19,2) NOT NULL,
    risk_amount DECIMAL(19,2) NOT NULL,
    expected_r DECIMAL(10,4) NOT NULL,
    allocation_pct DECIMAL(5,2) NOT NULL
);

-- Add index for capital allocation queries
CREATE INDEX idx_capital_allocation_snapshot_date ON capital_allocation_snapshot(run_date);