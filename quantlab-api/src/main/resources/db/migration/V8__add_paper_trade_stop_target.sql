-- Add stop_loss and target_price columns to paper_trade table
-- These columns store the risk management levels for each paper trade

-- Add stop_loss column (nullable, as not all trades may have a stop loss)
ALTER TABLE paper_trade ADD COLUMN IF NOT EXISTS stop_loss NUMERIC(19, 4);

-- Add target_price column (nullable, as not all trades may have a target)
ALTER TABLE paper_trade ADD COLUMN IF NOT EXISTS target_price NUMERIC(19, 4);

-- Add comment for documentation
COMMENT ON COLUMN paper_trade.stop_loss IS 'Stop loss price for risk management';
COMMENT ON COLUMN paper_trade.target_price IS 'Target price for profit taking';
