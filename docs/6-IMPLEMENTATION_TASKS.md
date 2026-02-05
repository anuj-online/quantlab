# Implementation Tasks - Strategy Library

**Status Legend:**
- ✅ Complete
- 🟡 In Progress
- ⬜ Not Started
- 🔄 Needs Review

---

## Phase 1: Core Framework (✅ Complete)

| Task | Strategy | File | Status | Notes |
|------|----------|------|--------|-------|
| 1.1 | ExecutionMode & StrategyContext | Framework | ✅ | BACKTEST/SCREEN modes |
| 1.2 | screening_results table | Database | ✅ | V2 migration |
| 1.3 | Screening API endpoint | API | ✅ | POST /api/v1/screening/run |
| 1.4 | Screening Dashboard UI | Frontend | ✅ | /screening route |

---

## Phase 2: Existing Strategies - SCREEN Mode (✅ Complete)

| Task | Strategy Code | File | Status | Dependency |
|------|---------------|------|--------|------------|
| 2.1 | EOD_BREAKOUT | EODBreakoutVolStrategy.java | ✅ | None |
| 2.2 | SMA_CROSSOVER | SMACrossoverStrategy.java | ✅ | None |
| 2.3 | NR4_INSIDE_BAR | NR4InsideBarStrategy.java | ✅ | None |
| 2.4 | GAP_UP_MOMENTUM | GapUpMomentumStrategy.java | ✅ | None - SCREEN mode verified |

**Note:** Fixed strategy code mismatches via V4 migration (EOD_BREAKOUT_VOL→EOD_BREAKOUT, SMA_20_50_CROSS→SMA_CROSSOVER, NR4_INSIDE→NR4_INSIDE_BAR)

---

## Phase 3: New Strategies - Independent Implementation (✅ Complete)

### Priority 1: Price Action (Single Candle Patterns)

| Task | Strategy Code | Name | File | Complexity | Status |
|------|---------------|------|------|------------|--------|
| 3.1 | **MOMENTUM_3D** | 3-Day Momentum Burst | Momentum3DayStrategy.java | ⭐ Low | ✅ |
| 3.2 | **GAP_HOLD** | Gap-Up Hold Continuation | GapHoldStrategy.java | ⭐ Low | ✅ |
| 3.3 | **EMA20_PULLBACK** | Trend Pullback to EMA20 | EMA20PullbackStrategy.java | ⭐⭐ Medium | ✅ |

**Description:**
- **MOMENTUM_3D**: 3 consecutive bullish candles + increasing volume + close > 10-day high
- **GAP_HOLD**: Gap up > 1.5% + range holds + close near high
- **EMA20_PULLBACK**: Price above EMA50 + pullback to EMA20 + bullish close

---

### Priority 2: Volatility Patterns

| Task | Strategy Code | Name | File | Complexity | Status |
|------|---------------|------|------|------------|--------|
| 3.4 | **BB_SQUEEZE** | Bollinger Band Squeeze | BollingerBandSqueezeStrategy.java | ⭐⭐ Medium | ✅ |
| 3.5 | **RANGE_BREAK_VOL** | Volume Expansion Breakout | RangeBreakVolumeStrategy.java | ⭐⭐ Medium | ✅ |

**Description:**
- **BB_SQUEEZE**: BB width lowest in 20 days + breakout outside band
- **RANGE_BREAK_VOL**: Tight range 10 days + breakout with volume > 2x

---

### Priority 3: Reversal Patterns

| Task | Strategy Code | Name | File | Complexity | Status |
|------|---------------|------|------|------------|--------|
| 3.6 | **HV_REVERSAL** | High Volume Reversal | HighVolumeReversalStrategy.java | ⭐⭐⭐ High | ✅ |
| 3.7 | **FAILED_BREAKDOWN** | Failed Breakdown (Bear Trap) | FailedBreakdownStrategy.java | ⭐⭐⭐ High | ✅ |

**Description:**
- **HV_REVERSAL**: Volume ≥ 2x + close near low + bullish next day (2-candle pattern)
- **FAILED_BREAKDOWN**: Break support + close above next day + high volume (2-candle pattern)

---

### Priority 4: Structure & Trend

| Task | Strategy Code | Name | File | Complexity | Status |
|------|---------------|------|------|------------|--------|
| 3.8 | **HH_HL_STRUCTURE** | Higher High Higher Low | HigherHighHigherLowStrategy.java | ⭐⭐⭐ High | ✅ |
| 3.9 | **REL_STRENGTH_30D** | Relative Strength Momentum | RelativeStrength30DayStrategy.java | ⭐⭐⭐⭐ Very High | ✅ |

**Description:**
- **HH_HL_STRUCTURE**: 3 swing highs increasing + 3 swing lows increasing (swing detection)
- **REL_STRENGTH_30D**: Stock return > Index return + breaks high (requires index data)

---

## Phase 4: Metadata & UI Enhancements (✅ Complete)

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 4.1 | Add `supportsScreening` boolean to Strategy metadata | ✅ | types.ts updated |
| 4.2 | Add `minLookbackDays` to Strategy metadata | ✅ | types.ts updated |
| 4.3 | Strategy selection UI with metadata display | ✅ | StrategyConfig.tsx, ScreeningDashboard.tsx |
| 4.4 | Strategy comparison view (side-by-side results) | ✅ | New StrategyComparison.tsx page |

**Files Modified:**
- `quantlab-frontend/types.ts` - Added Strategy metadata fields
- `quantlab-frontend/services/apiService.ts` - Added comparison API endpoints
- `quantlab-frontend/pages/StrategyConfig.tsx` - Added metadata display
- `quantlab-frontend/pages/ScreeningDashboard.tsx` - Added screening support indicators
- `quantlab-frontend/pages/StrategyComparison.tsx` - New comparison page (CREATED)
- `quantlab-frontend/App.tsx` - Added /compare route
- `quantlab-frontend/components/Layout.tsx` - Added Compare navigation item

---

## Phase 5: Backend API Integration (✅ Complete)

| Task | Description | Status | Notes |
|------|-------------|--------|-------|
| 5.1 | Add metadata fields to Strategy entity/repository | ✅ | V3 migration, entity updated |
| 5.2 | Update StrategySeeder with new metadata values | ✅ | All 13 strategies seeded |
| 5.3 | Implement GET /api/v1/strategies with metadata | ✅ | Returns enhanced StrategyResponse |
| 5.4 | Implement POST /api/v1/strategies/compare | ✅ | StrategyComparisonService created |
| 5.5 | Implement GET /api/v1/strategies/{code}/metrics | ✅ | Individual metrics endpoint |

**Files Created:**
- `V3__add_strategy_metadata.sql` - Database migration
- `StrategyMetrics.java` - Metrics DTO
- `StrategyComparisonRequest.java` - Comparison request DTO
- `StrategyComparisonResponse.java` - Comparison response DTO
- `StrategyMetricsRequest.java` - Metrics request DTO
- `StrategyComparisonService.java` - Comparison service

**Files Modified:**
- `Strategy.java` - Added supportsScreening, minLookbackDays
- `StrategyResponse.java` - Added metadata fields
- `StrategyMapper.java` - Updated to map metadata
- `StrategySeeder.java` - Seeded metadata for all 13 strategies
- `StrategyRunRepository.java` - Added comparison query
- `StrategyController.java` - Added compare and metrics endpoints

**API Endpoints Added:**
- `GET /api/v1/strategies` - Returns strategies with full metadata
- `POST /api/v1/strategies/compare` - Compare multiple strategies
- `GET /api/v1/strategies/{code}/metrics` - Get individual strategy metrics

---

## Implementation Guidelines

### All Strategies Must:

1. **Extend `Strategy` interface**
   ```java
   public StrategyResult evaluate(StrategyContext context)
   ```

2. **Support Both Modes**
   - `BACKTEST`: Loop through all candles
   - `SCREEN`: Evaluate only latest candle

3. **Return Standard Output**
   - `TradeSignal` with: symbol, signalDate, side, entry, stopLoss, target, strategyCode

4. **Use StrategyResult Factory**
   - `StrategyResult.actionable(signals)` for SCREEN mode
   - `StrategyResult.nonActionable(signals)` for BACKTEST mode

5. **Be Annotated**
   ```java
   @Component
   @Qualifier("uniqueStrategyName")
   ```

### Dependencies for Each Strategy:

| Strategy | External Data | Special Requirements |
|----------|---------------|---------------------|
| MOMENTUM_3D | None | Volume trend detection |
| GAP_HOLD | None | Gap calculation |
| EMA20_PULLBACK | None | EMA calculation |
| BB_SQUEEZE | None | Bollinger Bands calc |
| RANGE_BREAK_VOL | None | Range detection |
| HV_REVERSAL | None | 2-candle pattern |
| FAILED_BREAKDOWN | None | Support level + 2-candle |
| HH_HL_STRUCTURE | None | Swing detection algo |
| REL_STRENGTH_30D | Index data | Index comparison |

---

## Order of Implementation (Recommended)

```
Phase 2: Verify GAP_UP_MOMENTUM has SCREEN mode
    ↓
Phase 3.1: MOMENTUM_3D (easiest)
    ↓
Phase 3.1: GAP_HOLD
    ↓
Phase 3.1: EMA20_PULLBACK
    ↓
Phase 3.2: BB_SQUEEZE
    ↓
Phase 3.2: RANGE_BREAK_VOL
    ↓
Phase 3.3: HV_REVERSAL
    ↓
Phase 3.3: FAILED_BREAKDOWN
    ↓
Phase 3.4: HH_HL_STRUCTURE
    ↓
Phase 3.4: REL_STRENGTH_30D (last - needs index data)
    ↓
Phase 4: Metadata & UI
```

---

## Tracking

- **Total Strategies**: 13
- **Complete (Phase 1-3)**: 13 strategies implemented with SCREEN mode support
  - 4 original: EOD_BREAKOUT_VOL, SMA_20_50_CROSS, NR4_INSIDE, GAP_UP_MOMENTUM
  - 9 new: MOMENTUM_3D, GAP_HOLD, EMA20_PULLBACK, BB_SQUEEZE, RANGE_BREAK_VOL, HV_REVERSAL, FAILED_BREAKDOWN, HH_HL_STRUCTURE, REL_STRENGTH_30D
- **Phase 4 (UI)**: ✅ Complete - Metadata types and comparison UI implemented
- **Phase 5 (Backend API)**: ✅ Complete - Entity updates and comparison endpoints implemented

---

## Next Steps

### Testing & Validation
1. Run `mvn compile` to verify all strategies compile
2. Run `mvn test` to ensure tests pass
3. Start backend: `mvn spring-boot:run`
4. Start frontend: `npm run dev` (in quantlab-frontend/)
5. Test screening dashboard with new strategies
6. Test strategy comparison page
7. Verify metadata displays correctly in StrategyConfig

### Optional Enhancements (Future Phases)
1. Add unit tests for each new strategy
2. Add integration tests for screening/comparison endpoints
3. Performance optimization for strategy comparison
4. Add export functionality for comparison results
5. Add real-time screening updates
6. Add strategy performance charts
7. Add strategy parameter optimization UI

---

## Performance Optimization

**Current Issues:**
- Strategy execution: 5-10 seconds for 1,000 instruments (slow due to duplicate queries)
- Screening: 15-30 seconds for 3 strategies (database hit on every candle load)
- Data loading: 5-10 seconds for 2,000 CSV rows (non-batched inserts)

**Solution:**
See [docs/7-DATABASE_INDEXING_AND_CACHING.md](./7-DATABASE_INDEXING_AND_CACHING.md) for comprehensive performance optimization plan including:
- Database indexing strategy (V5 migration ready)
- Redis caching for hot data
- Code optimizations to eliminate duplicate queries
- Live data preparation

**Quick Win:** V5 migration adds 5 performance indexes for immediate 10-20% improvement.

---

## Documentation Index

| Document | Description |
|----------|-------------|
| [4-BACKTESTIN_AND_SCREENING](./4-BACKTESTIN_AND_SCREENING) | Backtesting and screening overview |
| [5-STRATEGIES_ADDITION.md](./5-STRATEGIES_ADDITION.md) | Strategy addition guidelines |
| [5.1-STRATEGIES_ADDITION.md](./5.1-STRATEGIES_ADDITION.md) | Strategy addition guidelines (alternate) |
| [6-IMPLEMENTATION_TASKS.md](./6-IMPLEMENTATION_TASKS.md) | Implementation task tracker |
| [7-DATABASE_INDEXING_AND_CACHING.md](./7-DATABASE_INDEXING_AND_CACHING.md) | Performance optimization guide |

---

*Last Updated: 2026-02-05*
