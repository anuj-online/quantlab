# QuantLab System Architecture Documentation

## Table of Contents
1. [Overview](#overview)
2. [Frontend Pages](#frontend-pages)
3. [Backend Controllers](#backend-controllers)
4. [Backend Services](#backend-services)
5. [Database Schema](#database-schema)
6. [API Endpoint Mapping](#api-endpoint-mapping)
7. [Data Flow Diagram](#data-flow-diagram)

---

## Overview

QuantLab is a quantitative trading research platform with a React/TypeScript frontend and Spring Boot (Java 17) backend. The platform supports strategy development, backtesting, real-time screening, and paper trading with ensemble voting and capital allocation features.

**Tech Stack:**
- **Frontend:** React + TypeScript + Vite (port 3000)
- **Backend:** Spring Boot 3.5.10 + Java 17 (port 8080)
- **Database:** PostgreSQL with Flyway migrations

---

## Frontend Pages

| Route | Page | Purpose |
|-------|------|---------|
| `/` | **Dashboard** | Analytics summary, equity curve, performance metrics |
| `/configure` | **StrategyConfig** | Configure and execute strategy backtests |
| `/signals` | **Signals** | Display generated BUY/SELL signals with entry/stop/target prices |
| `/active-trades` | **ActiveTrades** | Show open paper trades with unrealized P&L |
| `/trades` | **PaperTrades** | All paper trades (closed + open) with P&L calculations |
| `/screening` | **ScreeningDashboard** | Real-time screening with ensemble voting |
| `/capital-allocation` | **CapitalAllocation** | Risk-based portfolio allocation simulation |
| `/compare` | **StrategyComparison** | Compare multiple strategies side-by-side |

### Page Features

#### Dashboard (`/`)
- Total trades count and win rate
- Total P&L (profit and loss)
- Maximum drawdown visualization
- Equity curve chart

#### StrategyConfig (`/configure`)
- Select market (INDIA/US)
- Select trading strategy
- Configure date range for backtesting
- Set strategy parameters
- Execute backtest runs

#### Signals (`/signals`)
- View all generated signals for a strategy run
- Entry price, stop loss, target price
- Quantity and signal type (BUY/SELL)
- Ensemble voting information (vote count, confidence score)

#### ActiveTrades (`/active-trades`)
- Open positions only
- Current price vs entry price
- Unrealized P&L and percentage

#### PaperTrades (`/trades`)
- All trades (closed and open)
- Entry and exit details
- Realized P&L for closed trades
- Exit reason classification (STOP_LOSS, TARGET_HIT, TIME, MANUAL)
- R-multiple calculations

#### ScreeningDashboard (`/screening`)
- Point-in-time screening across multiple strategies
- Ensemble voting with consensus signals
- Signal ranking by confidence, R-multiple, liquidity
- Grouped results by strategy
- Actionable signals (high-confidence, high liquidity)

#### CapitalAllocation (`/capital-allocation`)
- Risk-based position sizing
- Capital deployment simulation
- Portfolio allocation across top-ranked signals
- Expected R-multiple tracking

#### StrategyComparison (`/compare`)
- Side-by-side strategy comparison
- Performance metrics (win rate, avg return, profit factor)
- Best performing strategy identification

---

## Backend Controllers

| Controller | Base Path | Endpoints |
|------------|-----------|-----------|
| **InstrumentController** | `/api/v1/instruments` | `GET /instruments?market={INDIA\|US}&active=true` |
| **StrategyController** | `/api/v1/strategies` | `GET /strategies`, `POST /run`, `POST /compare`, `GET /{code}/metrics` |
| **StrategyRunController** | `/api/v1/strategy-runs` | `GET /{runId}/signals`, `GET /{runId}/paper-trades`, `GET /{runId}/active-trades`, `GET /{runId}/profit-positions`, `GET /{runId}/analytics`, `GET /{runId}/equity-curve` |
| **ScreeningController** | `/api/v1/screening` | `POST /run`, `GET /history`, `POST /ensemble` |
| **CapitalAllocationController** | `/api/v1/capital-allocation` | `POST /simulate`, `GET /history` |

---

## Backend Services

### Core Business Logic Services

| Service | Responsibility | Key Operations |
|---------|----------------|----------------|
| **StrategyRunService** | Orchestrates backtesting pipeline | Parallel multi-instrument processing, risk-based position sizing |
| **ScreeningService** | Real-time signal generation | Thread pools for parallel processing, stores screening results |
| **EnsembleVotingService** | Multi-strategy consensus | Weighted voting, confidence scoring, signal ranking |
| **CapitalAllocationService** | Risk management | Risk-per-trade calculations, portfolio position sizing |
| **AnalyticsService** | Performance analysis | Metrics calculation, equity curves, drawdown analysis |
| **BhavcopyLoaderService** | Data ingestion | NSE bhavcopy data loading |
| **SignalRankingService** | Signal prioritization | Composite scoring (confidence, R-multiple, liquidity, volatility) |
| **PaperTradingEngine** | Trade simulation | FIFO processing, P&L tracking, exit logic |

### Market Data Integration

**CompositeMarketDataProvider** - Multi-source market data with precedence rules:
1. **Database** (Bhavcopy/Stooq) - Canonical source
2. **Yahoo Finance** - Gap filling fallback
3. **Automatic merge** with database precedence

Features: Parallel candle fetching, date range queries, circuit breaker patterns

---

## Database Schema

### Core Trading Tables

#### `instruments`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| symbol | VARCHAR | Trading symbol (e.g., INFY, AAPL) |
| market | ENUM | INDIA or US |
| active | BOOLEAN | Whether instrument is actively traded |

#### `strategies`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| code | VARCHAR | Unique strategy code |
| name | VARCHAR | Strategy display name |
| description | TEXT | Strategy description |
| min_lookback_days | INT | Minimum historical data required |
| supports_screening | BOOLEAN | Whether strategy supports screening mode |

#### `strategy_runs`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| strategy_id | BIGINT | FK → strategies.id |
| start_date | DATE | Backtest start date |
| end_date | DATE | Backtest end date |
| status | ENUM | COMPLETED, FAILED, RUNNING |
| created_at | TIMESTAMP | Run creation timestamp |

#### `trade_signals`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| run_id | BIGINT | FK → strategy_runs.id |
| symbol | VARCHAR | Trading symbol |
| signal_date | DATE | Signal generation date |
| side | ENUM | BUY or SELL |
| entry_price | DECIMAL | Recommended entry price |
| stop_loss | DECIMAL | Stop loss price |
| target_price | DECIMAL | Target price |
| quantity | INT | Position quantity |
| confidence | DECIMAL | Signal confidence score |
| ensemble_vote_count | INT | Number of strategies voting for signal |
| ensemble_agreement | DECIMAL | Agreement percentage |
| rank_score | DECIMAL | Composite ranking score |
| r_multiple | DECIMAL | Risk-reward multiple |

#### `paper_trades`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| signal_id | BIGINT | FK → trade_signals.id |
| symbol | VARCHAR | Trading symbol |
| entry_date | DATE | Position entry date |
| exit_date | DATE | Position exit date (nullable for open positions) |
| entry_price | DECIMAL | Actual entry price |
| exit_price | DECIMAL | Actual exit price |
| quantity | INT | Position quantity |
| pnl | DECIMAL | Profit/loss amount |
| pnl_pct | DECIMAL | Profit/loss percentage |
| status | ENUM | OPEN or CLOSED |
| exit_reason | ENUM | STOP_LOSS, TARGET_HIT, TIME_EXIT, OPPOSITE_SIGNAL |
| r_multiple | DECIMAL | Risk-reward multiple realized |

### Intelligence Layer Tables

#### `screening_results`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| screening_date | DATE | Date screening was performed |
| strategy_code | VARCHAR | Strategy that generated signal |
| symbol | VARCHAR | Trading symbol |
| signal_type | ENUM | BUY, SELL, HOLD |
| entry_price | DECIMAL | Signal entry price |
| stop_loss | DECIMAL | Signal stop loss |
| target_price | DECIMAL | Signal target |
| confidence | DECIMAL | Signal confidence |
| ensemble_vote_count | INT | Number of ensemble votes |
| rank_score | DECIMAL | Ranking score |

#### `capital_allocation_snapshots`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| run_date | DATE | Allocation simulation date |
| total_capital | DECIMAL | Total capital available |
| deployed_capital | DECIMAL | Capital deployed in positions |
| free_cash | DECIMAL | Unallocated cash |
| expected_r_multiple | DECIMAL | Expected portfolio R-multiple |
| created_at | TIMESTAMP | Snapshot creation time |

#### `capital_allocation_positions`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| snapshot_id | BIGINT | FK → capital_allocation_snapshots.id |
| symbol | VARCHAR | Trading symbol |
| quantity | INT | Position quantity |
| capital_used | DECIMAL | Capital allocated to position |
| risk_amount | DECIMAL | Risk amount (entry - stop loss) |
| expected_r | DECIMAL | Expected R-multiple |
| allocation_pct | DECIMAL | Percentage of capital allocated |

### Market Data Tables

#### `candles`
| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key |
| instrument_id | BIGINT | FK → instruments.id |
| date | DATE | Candle date |
| open | DECIMAL | Opening price |
| high | DECIMAL | High price |
| low | DECIMAL | Low price |
| close | DECIMAL | Closing price |
| volume | BIGINT | Trading volume |
| adjusted_close | DECIMAL | Adjusted closing price |

---

## API Endpoint Mapping

### Instrument Endpoints

| Method | Endpoint | Service | Repository | DB Tables |
|--------|----------|---------|------------|-----------|
| GET | `/api/v1/instruments?market={INDIA\|US}&active=true` | InstrumentController | InstrumentRepository | instruments |

### Strategy Endpoints

| Method | Endpoint | Service | Repository | DB Tables |
|--------|----------|---------|------------|-----------|
| GET | `/api/v1/strategies` | StrategyController | StrategyRepository | strategies |
| POST | `/api/v1/strategies/run` | StrategyController → StrategyRunService | StrategyRunRepository, TradeSignalRepository, PaperTradeRepository | strategy_runs, trade_signals, paper_trades |
| POST | `/api/v1/strategies/compare` | StrategyController | PaperTradeRepository | paper_trades |
| GET | `/api/v1/strategies/{code}/metrics` | StrategyController | PaperTradeRepository | paper_trades |

### Strategy Run Endpoints

| Method | Endpoint | Service | Repository | DB Tables |
|--------|----------|---------|------------|-----------|
| GET | `/api/v1/strategy-runs/{runId}/signals` | StrategyRunController | TradeSignalRepository | trade_signals |
| GET | `/api/v1/strategy-runs/{runId}/paper-trades` | StrategyRunController | PaperTradeRepository | paper_trades |
| GET | `/api/v1/strategy-runs/{runId}/active-trades` | StrategyRunController | PaperTradeRepository | paper_trades |
| GET | `/api/v1/strategy-runs/{runId}/profit-positions` | StrategyRunController | PaperTradeRepository | paper_trades |
| GET | `/api/v1/strategy-runs/{runId}/analytics` | StrategyRunController → AnalyticsService | TradeSignalRepository, PaperTradeRepository | trade_signals, paper_trades |
| GET | `/api/v1/strategy-runs/{runId}/equity-curve` | StrategyRunController → AnalyticsService | PaperTradeRepository | paper_trades |

### Screening Endpoints

| Method | Endpoint | Service | Repository | DB Tables |
|--------|----------|---------|------------|-----------|
| POST | `/api/v1/screening/run` | ScreeningController → ScreeningService | ScreeningResultsRepository | screening_results |
| GET | `/api/v1/screening/history` | ScreeningController | ScreeningResultsRepository | screening_results |
| POST | `/api/v1/screening/ensemble` | ScreeningController → EnsembleVotingService | TradeSignalRepository | trade_signals |

### Capital Allocation Endpoints

| Method | Endpoint | Service | Repository | DB Tables |
|--------|----------|---------|------------|-----------|
| POST | `/api/v1/capital-allocation/simulate` | CapitalAllocationController → CapitalAllocationService | CapitalAllocationPositionRepository, CapitalAllocationSnapshotRepository | capital_allocation_snapshots, capital_allocation_positions |
| GET | `/api/v1/capital-allocation/history` | CapitalAllocationController | CapitalAllocationSnapshotRepository | capital_allocation_snapshots, capital_allocation_positions |

---

## Per-View Data Flow

This section provides a detailed breakdown of each frontend view, showing the exact API calls, services, repositories, and database tables involved.

### 1. Dashboard (`/`)

**Purpose:** Display analytics summary and equity curve for a strategy run.

**Data Flow:**
```
Dashboard Component
    │
    ├── apiService.getAnalytics(runId)
    │       │
    │       └── GET /api/v1/strategy-runs/{runId}/analytics
    │               │
    │               ├── StrategyRunController.getAnalytics()
    │               │       │
    │               │       └── AnalyticsService.getAnalytics(runId)
    │               │               │
    │               │               ├── TradeSignalRepository.countByRunId(runId)
    │               │               │       └── TABLE: trade_signals
    │               │               │
    │               │               └── PaperTradeRepository.findByRunId(runId)
    │               │                       └── TABLE: paper_trades
    │               │
    │               └── Response: { totalTrades, winRate, totalPnl, maxDrawdown }
    │
    └── apiService.getEquityCurve(runId)
            │
            └── GET /api/v1/strategy-runs/{runId}/equity-curve
                    │
                    ├── StrategyRunController.getEquityCurve()
                    │       │
                    │       └── AnalyticsService.getEquityCurve(runId)
                    │               │
                    │               └── PaperTradeRepository.findByRunIdOrderByEntryDate(runId)
                    │                       └── TABLE: paper_trades
                    │
                    └── Response: [{ date, equity }, ...]
```

**Tables Accessed:** `trade_signals`, `paper_trades`

**Services Used:** `AnalyticsService`

---

### 2. StrategyConfig (`/configure`)

**Purpose:** Configure parameters and execute a strategy backtest.

**Data Flow:**
```
StrategyConfig Component
    │
    ├── On Mount (Initial Load)
    │       │
    │       └── apiService.getStrategies()
    │               │
    │               └── GET /api/v1/strategies
    │                       │
    │                       ├── StrategyController.getStrategies()
    │                       │       │
    │                       │       └── StrategyService.getAllStrategies()
    │                       │               │
    │                       │               └── StrategyRepository.findAll()
    │                       │                       └── TABLE: strategies
    │                       │
    │                       └── Response: [{ id, code, name, description, supportsScreening, minLookbackDays }, ...]
    │
    └── On Run Button Click
            │
            └── apiService.runStrategy({ strategyCode, market, startDate, endDate, params })
                    │
                    └── POST /api/v1/strategies/run
                            │
                            ├── StrategyController.runStrategy()
                            │       │
                            │       └── StrategyService.runStrategy(request)
                            │               │
                            │               ├── 1. Create StrategyRun record
                            │               │       └── StrategyRunRepository.save()
                            │               │               └── TABLE: strategy_runs
                            │               │
                            │               ├── 2. Fetch market data for date range
                            │               │       └── CompositeMarketDataProvider.getCandles()
                            │               │               └── TABLE: candles
                            │               │
                            │               ├── 3. Execute strategy (parallel per instrument)
                            │               │       └── Strategy.evaluate()
                            │               │
                            │               ├── 4. Generate and save signals
                            │               │       └── TradeSignalRepository.saveAll()
                            │               │               └── TABLE: trade_signals
                            │               │
                            │               ├── 5. Run paper trading engine
                            │               │       └── PaperTradingEngine.execute()
                            │               │
                            │               └── 6. Save paper trades
                            │                       └── PaperTradeRepository.saveAll()
                            │                               └── TABLE: paper_trades
                            │
                            └── Response: { strategyRunId, status }
```

**Tables Accessed:** `strategies`, `strategy_runs`, `trade_signals`, `paper_trades`, `candles`

**Services Used:** `StrategyService`, `StrategyRunService`, `PaperTradingEngine`, `CompositeMarketDataProvider`

---

### 3. Signals (`/signals`)

**Purpose:** Display all generated trading signals for a strategy run.

**Data Flow:**
```
Signals Component
    │
    └── apiService.getSignals(runId)
            │
            └── GET /api/v1/strategy-runs/{runId}/signals
                    │
                    ├── StrategyRunController.getTradeSignals()
                    │       │
                    │       └── StrategyRunService.getTradeSignals(runId)
                    │               │
                    │               └── TradeSignalRepository.findByRunId(runId)
                    │                       └── TABLE: trade_signals
                    │
                    └── Response: [{ symbol, signalDate, side, entryPrice, stopLoss, targetPrice, quantity, strategy }, ...]
```

**Tables Accessed:** `trade_signals`

**Services Used:** `StrategyRunService`

---

### 4. ActiveTrades (`/active-trades`)

**Purpose:** Show currently open paper trades with unrealized P&L.

**Data Flow:**
```
ActiveTrades Component (polls every 30 seconds)
    │
    └── apiService.getActiveTrades(runId)
            │
            └── GET /api/v1/strategy-runs/{runId}/active-trades
                    │
                    ├── StrategyRunController.getActiveTrades()
                    │       │
                    │       └── StrategyRunService.getActiveTrades(runId)
                    │               │
                    │               └── PaperTradeRepository.findByRunIdAndStatus(runId, 'OPEN')
                    │                       └── TABLE: paper_trades
                    │
                    └── Response: [{ symbol, entryDate, entryPrice, quantity, currentPrice, unrealizedPnl, unrealizedPnlPct, stopLoss, targetPrice }, ...]
```

**Tables Accessed:** `paper_trades`

**Services Used:** `StrategyRunService`

---

### 5. PaperTrades (`/trades`)

**Purpose:** Display all paper trades (closed and open) with P&L.

**Data Flow:**
```
PaperTrades Component
    │
    └── apiService.getPaperTrades(runId)
            │
            └── GET /api/v1/strategy-runs/{runId}/paper-trades
                    │
                    ├── StrategyRunController.getPaperTrades()
                    │       │
                    │       └── StrategyRunService.getPaperTrades(runId)
                    │               │
                    │               └── PaperTradeRepository.findByRunId(runId)
                    │                       └── TABLE: paper_trades
                    │
                    └── Response: [{ symbol, entryDate, exitDate, entryPrice, exitPrice, quantity, pnl, pnlPct, status, exitReason }, ...]
```

**Tables Accessed:** `paper_trades`

**Services Used:** `StrategyRunService`

---

### 6. ScreeningDashboard (`/screening`)

**Purpose:** Run point-in-time screening and ensemble voting across multiple strategies.

**Data Flow:**
```
ScreeningDashboard Component
    │
    ├── On Mount (Initial Load)
    │       │
    │       └── apiService.getStrategies()
    │               │
    │               └── GET /api/v1/strategies (same as StrategyConfig)
    │                       └── TABLE: strategies
    │
    ├── On "Run Screening" Click
    │       │
    │       └── apiService.runScreening(strategyCodes, date)
    │               │
    │               └── POST /api/v1/screening/run
    │                       │
    │                       ├── ScreeningController.runScreening()
    │                       │       │
    │                       │       └── ScreeningService.runScreening(request)
    │                       │               │
    │                       │               ├── 1. For each strategy: run evaluation
    │                       │               │       └── Strategy.evaluate() (SCREEN mode)
    │                       │               │
    │                       │               ├── 2. Fetch latest market data
    │                       │               │       └── CompositeMarketDataProvider.getLatestPrice()
    │                       │               │               └── TABLE: candles
    │                       │               │
    │                       │               ├── 3. Save screening results
    │                       │               │       └── ScreeningResultsRepository.saveAll()
    │                       │               │               └── TABLE: screening_results
    │                       │               │
    │                       │               └── 4. Group results by strategy
    │                       │
    │                       └── Response: { screeningDate, signalsByStrategy: { strategyCode: [signals] }, totalSignals }
    │
    └── On "Ensemble Voting" Click
            │
            └── apiService.runEnsembleScreening(strategyCodes, date)
                    │
                    └── POST /api/v1/screening/ensemble
                            │
                            ├── ScreeningController.runEnsembleScreening()
                            │       │
                            │       └── EnsembleVotingService.generateEnsembleSignals(request)
                            │               │
                            │               ├── 1. Run screening for all strategies (if not cached)
                            │               │       └── ScreeningService.runScreening()
                            │               │
                            │               ├── 2. Collect all BUY signals
                            │               │       └── ScreeningResultsRepository.findByDateAndType()
                            │               │               └── TABLE: screening_results
                            │               │
                            │               ├── 3. Apply voting logic
                            │               │       └── count votes per symbol
                            │               │
                            │               ├── 4. Calculate confidence scores
                            │               │       └── voteCount / totalStrategies
                            │               │
                            │               ├── 5. Rank signals
                            │               │       └── SignalRankingService.rankSignals()
                            │               │
                            │               └── 6. Return ensemble signals
                            │
                            └── Response: { signals: [{ ensembleId, symbol, entryPrice, stopLoss, targetPrice, voteScore, totalStrategies, confidenceScore, rankScore, rMultiple, strategyVotes }], ... }
```

**Tables Accessed:** `strategies`, `screening_results`, `candles`

**Services Used:** `ScreeningService`, `EnsembleVotingService`, `SignalRankingService`, `CompositeMarketDataProvider`

---

### 7. CapitalAllocation (`/capital-allocation`)

**Purpose:** Simulate risk-based capital allocation across ranked signals.

**Data Flow:**
```
CapitalAllocation Component
    │
    ├── On Mount (Initial Load)
    │       │
    │       └── fetch('/api/v1/screening/latest-date')
    │               │
    │               └── GET /api/v1/screening/latest-date
    │                       │
    │                       └── ScreeningController.getLatestScreeningDate()
    │                               │
    │                               └── ScreeningService.getMostRecentScreeningDate()
    │                                       │
    │                                       └── ScreeningResultsRepository.findLatestDate()
    │                                               └── TABLE: screening_results
    │
    └── On "Simulate Capital Allocation" Click
            │
            └── apiService.simulateCapitalAllocation({ date, totalCapital, riskPerTradePct, maxOpenTrades })
                    │
                    └── POST /api/v1/capital-allocation/simulate
                            │
                            ├── CapitalAllocationController.simulateAllocation()
                            │       │
                            │       └── CapitalAllocationService.simulateAllocation(request)
                            │               │
                            │               ├── 1. Fetch top-ranked ensemble signals for date
                            │               │       └── ScreeningResultsRepository.findByDateOrderByRankDesc()
                            │               │               └── TABLE: screening_results
                            │               │
                            │               ├── 2. Calculate position sizes
                            │               │       └── quantity = (totalCapital × riskPerTradePct) / (entry - stop)
                            │               │
                            │               ├── 3. Apply max open trades constraint
                            │               │
                            │               ├── 4. Calculate capital deployment
                            │               │
                            │               ├── 5. Save allocation snapshot
                            │               │       └── CapitalAllocationSnapshotRepository.save()
                                    │                       └── TABLE: capital_allocation_snapshots
                                    │
                                    │               ├── 6. Save allocation positions
                                    │               │       └── CapitalAllocationPositionRepository.saveAll()
                                    │               │               └── TABLE: capital_allocation_positions
                                    │               │
                                    │               └── 7. Return snapshot with positions
                                    │
                                    └── Response: { id, runDate, totalCapital, deployedCapital, freeCash, expectedRMultiple, positions: [{ symbol, quantity, capitalUsed, riskAmount, expectedR, allocationPct }], ... }
```

**Tables Accessed:** `screening_results`, `capital_allocation_snapshots`, `capital_allocation_positions`

**Services Used:** `ScreeningService`, `CapitalAllocationService`

---

### 8. StrategyComparison (`/compare`)

**Purpose:** Compare multiple strategies side-by-side with performance metrics.

**Data Flow:**
```
StrategyComparison Component
    │
    ├── On Mount (Initial Load)
    │       │
    │       └── apiService.getStrategies()
    │               │
    │               └── GET /api/v1/strategies (same as StrategyConfig)
    │                       └── TABLE: strategies
    │
    └── On "Compare Strategies" Click
            │
            └── apiService.compareStrategies({ strategyCodes, market, startDate, endDate })
                    │
                    └── POST /api/v1/strategies/compare
                            │
                            ├── StrategyController.compareStrategies()
                            │       │
                            │       └── StrategyComparisonService.compareStrategies(request)
                            │               │
                            │               ├── For each strategy code:
                            │               │       │
                            │               │       ├── 1. Find completed strategy runs
                            │               │       │       └── StrategyRunRepository.findByStrategyCodeAndMarketAndDateRange()
                                    │               │               └── TABLE: strategy_runs
                                    │               │
                                    │               │       ├── 2. Aggregate paper trades for runs
                                    │               │       │       └── PaperTradeRepository.findByRunId()
                                    │               │       │               └── TABLE: paper_trades
                                    │               │       │
                                    │               │       ├── 3. Calculate metrics
                                    │               │       │       ├── totalTrades = count(trades)
                                    │               │       │       ├── winRate = wins / totalTrades
                                    │               │       │       ├── avgReturn = avg(pnlPct)
                                    │               │       │       ├── totalPnl = sum(pnl)
                                    │               │       │       ├── maxDrawdown = calculateDrawdown()
                                    │               │       │       ├── sharpeRatio = calculateSharpe()
                                    │               │       │       ├── profitFactor = grossWin / grossLoss
                                    │               │       │       └── avgWin/avgLoss = avg(winning/losing trades)
                                    │               │       │
                                    │               │       └── 4. Build StrategyMetrics
                                    │               │
                                    │               ├── Compare metrics across strategies
                                    │               │       └── Identify best performing for each metric
                                    │               │
                                    │               └── Return comparison results
                                    │
                                    └── Response: { comparisonDate, market, metrics: [{ strategyCode, strategyName, totalTrades, winRate, avgReturn, totalPnl, maxDrawdown, sharpeRatio, profitFactor, avgWin, avgLoss }, ...], bestPerforming: [...] }
```

**Tables Accessed:** `strategies`, `strategy_runs`, `paper_trades`

**Services Used:** `StrategyComparisonService`

---

### Summary: Views → Endpoints → Services → Tables

| View | Endpoints Called | Services Invoked | Tables Accessed |
|------|-----------------|------------------|-----------------|
| **Dashboard** | `GET /strategy-runs/{id}/analytics`, `GET /strategy-runs/{id}/equity-curve` | AnalyticsService | trade_signals, paper_trades |
| **StrategyConfig** | `GET /strategies`, `POST /strategies/run` | StrategyService, StrategyRunService, PaperTradingEngine | strategies, strategy_runs, trade_signals, paper_trades, candles |
| **Signals** | `GET /strategy-runs/{id}/signals` | StrategyRunService | trade_signals |
| **ActiveTrades** | `GET /strategy-runs/{id}/active-trades` | StrategyRunService | paper_trades |
| **PaperTrades** | `GET /strategy-runs/{id}/paper-trades` | StrategyRunService | paper_trades |
| **ScreeningDashboard** | `GET /strategies`, `POST /screening/run`, `POST /screening/ensemble` | ScreeningService, EnsembleVotingService, SignalRankingService | strategies, screening_results, candles |
| **CapitalAllocation** | `GET /screening/latest-date`, `POST /capital-allocation/simulate` | ScreeningService, CapitalAllocationService | screening_results, capital_allocation_snapshots, capital_allocation_positions |
| **StrategyComparison** | `GET /strategies`, `POST /strategies/compare` | StrategyComparisonService | strategies, strategy_runs, paper_trades |

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            FRONTEND (React + TypeScript)                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │  Dashboard   │  │StrategyConfig│  │   Signals    │  │ActiveTrades  │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │                 │                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ PaperTrades  │  │  Screening   │  │CapitalAlloc  │  │StrategyCompare│      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │                 │                 │
│         └─────────────────┴─────────────────┴─────────────────┘                 │
│                                       │                                          │
│                         ┌─────────────┴─────────────┐                            │
│                         │       apiService.ts        │                            │
│                         │  BASE_URL = '/api/v1'     │                            │
│                         └─────────────┬─────────────┘                            │
└───────────────────────────────────────┼───────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         BACKEND (Spring Boot + Java 17)                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                              CONTROLLERS                                │   │
│  │  InstrumentController │ StrategyController │ StrategyRunController    │   │
│  │  ScreeningController  │ CapitalAllocationController                   │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                      │                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                               SERVICES                                  │   │
│  │  StrategyRunService │ ScreeningService │ EnsembleVotingService          │   │
│  │  CapitalAllocationService │ AnalyticsService │ PaperTradingEngine       │   │
│  │  SignalRankingService │ BhavcopyLoaderService                         │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                      │                                          │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                        CompositeMarketDataProvider                       │   │
│  │  Database (Bhavcopy/Stooq) ──┳── Yahoo Finance (Fallback)              │   │
│  └───────────────────────────────┼───────────────────────────────────────┘   │
└────────────────────────────────────┼───────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            DATABASE (PostgreSQL)                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  Core Tables                                                             │   │
│  │  instruments │ strategies │ strategy_runs │ trade_signals │ paper_trades │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  Intelligence Layer                                                      │   │
│  │  screening_results │ capital_allocation_snapshots │ capital_allocation_positions│
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  Market Data                                                             │   │
│  │  candles                                                                  │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Trading Strategies

| Strategy | Type | Logic |
|----------|------|-------|
| **SMA Crossover** | Trend | Fast/slow SMA crosses |
| **EMA Breakout** | Momentum | EMA breakout with volume confirmation |
| **Gap Up Momentum** | Gap | Gap trading with time-based exits |
| **Relative Strength** | Momentum | 30-day momentum ranking |
| **Bollinger Band Squeeze** | Volatility | Volatility contraction plays |
| **NR4 Inside Bar** | Pattern | Narrow range pattern recognition |
| **High Volume Reversal** | Reversal | Volume-based reversal signals |

All strategies implement the `Strategy` interface supporting both BACKTEST and SCREEN modes.

---

## Key Design Patterns

1. **Immutable Strategy Runs** - Each execution creates a new record ensuring reproducibility
2. **Risk-Based Position Sizing** - Positions calculated as `(Capital × Risk%) / (Entry - Stop)`
3. **Parallel Processing** - Strategies and instruments processed concurrently
4. **Separation of Concerns** - Screening (point-in-time) vs Backtesting (historical) modes
5. **Data Source Abstraction** - Strategies remain agnostic to underlying market data providers

---

## Infrastructure

- **AsyncConfig**: Dedicated thread pools for screening vs. backtesting
- **DataLoaderOnStartup**: Auto-loads instruments and strategies on application startup
- **Flyway Migrations**: Database version control (V1-V8)
- **Circuit Breaker**: Resilience patterns for external service calls (Yahoo Finance)

---

*Generated: 2026-02-06*
*Version: 1.0*

