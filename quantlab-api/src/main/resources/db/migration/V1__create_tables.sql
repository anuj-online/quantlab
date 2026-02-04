-- ============================================================================
-- QuantLab Database Schema - Flyway Migration V1
-- Creates all 7 tables: instrument, candle, strategy, strategy_run,
-- trade_signal, paper_trade, paper_position
-- ============================================================================

-- ============================================================================
-- 1. Instrument Table
-- Purpose: Symbol universe control
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS instrument_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE instrument (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('instrument_id_seq'),
    symbol          VARCHAR(50)  NOT NULL,
    name            VARCHAR(255),
    market          VARCHAR(20)  NOT NULL CHECK (market IN ('INDIA', 'US')),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_instrument_symbol_market UNIQUE (symbol, market)
);

-- Index for market queries
CREATE INDEX idx_instrument_market ON instrument(market);
CREATE INDEX idx_instrument_active ON instrument(active);

-- ============================================================================
-- 2. Candle Table (Daily OHLCV)
-- Purpose: End-of-day price data
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS candle_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE candle (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('candle_id_seq'),
    instrument_id   BIGINT       NOT NULL,
    trade_date      DATE         NOT NULL,
    open            NUMERIC(15, 4),
    high            NUMERIC(15, 4),
    low             NUMERIC(15, 4),
    close           NUMERIC(15, 4),
    volume          BIGINT,
    CONSTRAINT fk_candle_instrument FOREIGN KEY (instrument_id)
        REFERENCES instrument(id) ON DELETE CASCADE,
    CONSTRAINT uq_candle_instrument_date UNIQUE (instrument_id, trade_date)
);

-- Indexes for common queries
CREATE INDEX idx_candle_instrument ON candle(instrument_id);
CREATE INDEX idx_candle_trade_date ON candle(trade_date);
CREATE INDEX idx_candle_instrument_date ON candle(instrument_id, trade_date);

-- ============================================================================
-- 3. Strategy Table
-- Purpose: Static reference table for strategy definitions
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS strategy_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE strategy (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('strategy_id_seq'),
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Index for active strategy queries
CREATE INDEX idx_strategy_active ON strategy(active);
CREATE INDEX idx_strategy_code ON strategy(code);

-- ============================================================================
-- 4. Strategy Run Table
-- Purpose: Represents one execution of a strategy with params
-- Used for replay & comparison
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS strategy_run_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE strategy_run (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('strategy_run_id_seq'),
    strategy_id     BIGINT       NOT NULL,
    market          VARCHAR(20)  NOT NULL CHECK (market IN ('INDIA', 'US')),
    params_json     JSONB        NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    run_timestamp   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_strategy_run_strategy FOREIGN KEY (strategy_id)
        REFERENCES strategy(id) ON DELETE CASCADE
);

-- Indexes for strategy run queries
CREATE INDEX idx_strategy_run_strategy ON strategy_run(strategy_id);
CREATE INDEX idx_strategy_run_market ON strategy_run(market);
CREATE INDEX idx_strategy_run_timestamp ON strategy_run(run_timestamp);

-- ============================================================================
-- 5. Trade Signal Table
-- Purpose: Generated signals (signals != trades)
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS trade_signal_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE trade_signal (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('trade_signal_id_seq'),
    strategy_run_id BIGINT       NOT NULL,
    instrument_id   BIGINT       NOT NULL,
    signal_date     DATE         NOT NULL,
    side            VARCHAR(10)  NOT NULL CHECK (side IN ('BUY', 'SELL')),
    entry_price     NUMERIC(15, 4),
    stop_loss       NUMERIC(15, 4),
    target_price    NUMERIC(15, 4),
    quantity        BIGINT,
    CONSTRAINT fk_trade_signal_strategy_run FOREIGN KEY (strategy_run_id)
        REFERENCES strategy_run(id) ON DELETE CASCADE,
    CONSTRAINT fk_trade_signal_instrument FOREIGN KEY (instrument_id)
        REFERENCES instrument(id) ON DELETE CASCADE
);

-- Indexes for signal queries
CREATE INDEX idx_trade_signal_strategy_run ON trade_signal(strategy_run_id);
CREATE INDEX idx_trade_signal_instrument ON trade_signal(instrument_id);
CREATE INDEX idx_trade_signal_date ON trade_signal(signal_date);
CREATE INDEX idx_trade_signal_side ON trade_signal(side);

-- ============================================================================
-- 6. Paper Trade Table
-- Purpose: Closed trades only
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS paper_trade_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE paper_trade (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('paper_trade_id_seq'),
    strategy_run_id BIGINT       NOT NULL,
    instrument_id   BIGINT       NOT NULL,
    entry_date      DATE         NOT NULL,
    entry_price     NUMERIC(15, 4) NOT NULL,
    exit_date       DATE         NOT NULL,
    exit_price      NUMERIC(15, 4) NOT NULL,
    quantity        BIGINT       NOT NULL,
    pnl             NUMERIC(15, 4),
    pnl_pct         NUMERIC(10, 4),
    CONSTRAINT fk_paper_trade_strategy_run FOREIGN KEY (strategy_run_id)
        REFERENCES strategy_run(id) ON DELETE CASCADE,
    CONSTRAINT fk_paper_trade_instrument FOREIGN KEY (instrument_id)
        REFERENCES instrument(id) ON DELETE CASCADE
);

-- Indexes for paper trade queries
CREATE INDEX idx_paper_trade_strategy_run ON paper_trade(strategy_run_id);
CREATE INDEX idx_paper_trade_instrument ON paper_trade(instrument_id);
CREATE INDEX idx_paper_trade_entry_date ON paper_trade(entry_date);
CREATE INDEX idx_paper_trade_exit_date ON paper_trade(exit_date);

-- ============================================================================
-- 7. Paper Position Table
-- Purpose: Open trades (optional)
-- ============================================================================
CREATE SEQUENCE IF NOT EXISTS paper_position_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE paper_position (
    id              BIGINT       PRIMARY KEY DEFAULT nextval('paper_position_id_seq'),
    strategy_run_id BIGINT       NOT NULL,
    instrument_id   BIGINT       NOT NULL,
    entry_date      DATE         NOT NULL,
    entry_price     NUMERIC(15, 4) NOT NULL,
    quantity        BIGINT       NOT NULL,
    stop_loss       NUMERIC(15, 4),
    target_price    NUMERIC(15, 4),
    status          VARCHAR(20)  NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT fk_paper_position_strategy_run FOREIGN KEY (strategy_run_id)
        REFERENCES strategy_run(id) ON DELETE CASCADE,
    CONSTRAINT fk_paper_position_instrument FOREIGN KEY (instrument_id)
        REFERENCES instrument(id) ON DELETE CASCADE
);

-- Indexes for paper position queries
CREATE INDEX idx_paper_position_strategy_run ON paper_position(strategy_run_id);
CREATE INDEX idx_paper_position_instrument ON paper_position(instrument_id);
CREATE INDEX idx_paper_position_status ON paper_position(status);
CREATE INDEX idx_paper_position_entry_date ON paper_position(entry_date);

-- ============================================================================
-- Comments for documentation
-- ============================================================================
COMMENT ON TABLE instrument IS 'Symbol universe control for trading instruments';
COMMENT ON TABLE candle IS 'Daily OHLCV price data - EOD only';
COMMENT ON TABLE strategy IS 'Static reference table for strategy definitions';
COMMENT ON TABLE strategy_run IS 'Represents one execution of a strategy with params - used for replay & comparison';
COMMENT ON TABLE trade_signal IS 'Generated signals - signals are not the same as trades';
COMMENT ON TABLE paper_trade IS 'Closed paper trades with realized P&L';
COMMENT ON TABLE paper_position IS 'Open paper trades that are still active';
