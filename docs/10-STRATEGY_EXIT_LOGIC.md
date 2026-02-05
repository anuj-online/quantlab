# Strategy Exit Logic Reference

## Overview

This document maps each strategy to its exit logic requirements in the `PaperTradingEngine`. When a strategy is not explicitly handled in the switch statement, it falls back to the default stop-loss-only exit, which may not reflect the intended behavior.

---

## Current Status in PaperTradingEngine

As of commit on `feature/strategy-framework-implementation`, the `PaperTradingEngine.java` switch statement handles:

| Strategy Code | Exit Logic Method |
|---------------|-------------------|
| `EMA_BREAKOUT` | `findExitForEmaBreakout()` |
| `SMA_CROSSOVER` | `findExitForSmaCrossover()` |
| `GAP_UP_MOMENTUM` | `findExitForGapUpMomentum()` |
| `REL_STRENGTH_30D` | `findExitForEmaBreakout()` |

---

## All Strategies and Their Exit Logic

### Exit Type A: Stop Loss OR Target Hit

Used by: **EMA20_PULLBACK, EOD_BREAKOUT, HV_REVERSAL, RANGE_BREAK_VOL, MOMENTUM_3D, GAP_HOLD, NR4_INSIDE_BAR, HH_HL_STRUCTURE, FAILED_BREAKDOWN, BB_SQUEEZE, REL_STRENGTH_30D**

**Exit Logic:**
- Exit when `close <= stopLoss` OR `close >= targetPrice`
- Checks each subsequent candle in sequence
- First condition met triggers exit

**Current Handling:** Use `findExitForEmaBreakout()` method (same logic)

| Strategy Code | Stop Loss | Target | Risk:Reward |
|---------------|-----------|--------|-------------|
| `EMA20_PULLBACK` | Below recent swing low (1% buffer) | Entry + 2x risk | 2:1 |
| `EOD_BREAKOUT` | Lowest low of lookback period | Entry + 2x risk | 2:1 |
| `HV_REVERSAL` | Day 1's low (2% buffer) | Entry + 2x risk | 2:1 |
| `RANGE_BREAK_VOL` | Range low (2% buffer) | Entry + 2x range width | varies |
| `MOMENTUM_3D` | 3-day low (2% buffer) | Entry + 2x risk | 2:1 |
| `GAP_HOLD` | Day's low | Entry + 2x risk | 2:1 |
| `NR4_INSIDE_BAR` | Today's low | Entry + 2x range | varies |
| `HH_HL_STRUCTURE` | Most recent swing low (2% buffer) | Entry + 2x risk | 2:1 |
| `FAILED_BREAKDOWN` | Day 2's low (2% buffer) | Entry + 2.5x risk | 2.5:1 |
| `BB_SQUEEZE` | Lower Bollinger Band (2% buffer) | Entry + 2x band width | varies |
| `REL_STRENGTH_30D` | 3% below entry | Entry + 2x risk | 2:1 |

---

### Exit Type B: Stop Loss Only

Used by: **SMA_CROSSOVER**

**Exit Logic:**
- Exit when `close <= stopLoss`
- No target price used
- Waits for opposite signal (not implemented in executor)

**Current Handling:** Use `findExitForSmaCrossover()` method

---

### Exit Type C: Time-Based OR Stop Loss

Used by: **GAP_UP_MOMENTUM**

**Exit Logic:**
- Exit after 3 holding days OR if `close <= stopLoss`
- Whichever comes first

**Current Handling:** Use `findExitForGapUpMomentum()` method

---

## Implementation Recommendations

### Option 1: Group by Exit Type (Recommended)

Add all "Type A" strategies to share the `findExitForEmaBreakout()` method:

```java
// In PaperTradingEngine.java, findExitCandle() method
return switch (strategyCode) {
    // Type A: Stop Loss OR Target Hit
    case "EMA_BREAKOUT",
         "EMA20_PULLBACK",
         "EOD_BREAKOUT",
         "HV_REVERSAL",
         "RANGE_BREAK_VOL",
         "MOMENTUM_3D",
         "GAP_HOLD",
         "NR4_INSIDE_BAR",
         "HH_HL_STRUCTURE",
         "FAILED_BREAKDOWN",
         "BB_SQUEEZE",
         "REL_STRENGTH_30D"
         -> findExitForEmaBreakout(signal, futureCandles);

    // Type B: Stop Loss Only
    case "SMA_CROSSOVER" -> findExitForSmaCrossover(signal, futureCandles);

    // Type C: Time-Based OR Stop Loss
    case "GAP_UP_MOMENTUM" -> findExitForGapUpMomentum(signal, futureCandles);

    default -> {
        logger.warn("Unknown strategy type: {}, using default stop loss exit", strategyCode);
        yield findExitByStopLoss(signal, futureCandles);
    }
};
```

### Option 2: Individual Methods (Future Flexibility)

If future strategies need unique exit logic beyond the three types above, create individual methods for each strategy.

---

## Warning Details

When a strategy is not in the switch statement, the following warning is logged:

```
WARN ... PaperTradingEngine : Unknown strategy type: {CODE}, using default stop loss exit
```

This means:
- Only stop-loss exits are honored
- Target profit exits are ignored
- Paper trading results will not reflect intended strategy behavior

---

## Strategy Exit Logic Quick Reference

```
┌─────────────────────────────────────┬───────────────────┬────────────────────┐
│ Strategy Code                       │ Exit Type         │ Method to Use      │
├─────────────────────────────────────┼───────────────────┼────────────────────┤
│ EMA_BREAKOUT                        │ SL or Target      │ findExitForEma...  │
│ SMA_CROSSOVER                       │ SL Only           │ findExitForSma...  │
│ GAP_UP_MOMENTUM                     │ SL or Time (3d)   │ findExitForGap...  │
│ REL_STRENGTH_30D                    │ SL or Target      │ findExitForEma...  │
├─────────────────────────────────────┼───────────────────┼────────────────────┤
│ EMA20_PULLBACK                      │ SL or Target      │ findExitForEma...  │
│ EOD_BREAKOUT                        │ SL or Target      │ findExitForEma...  │
│ HV_REVERSAL                         │ SL or Target      │ findExitForEma...  │
│ RANGE_BREAK_VOL                     │ SL or Target      │ findExitForEma...  │
│ MOMENTUM_3D                         │ SL or Target      │ findExitForEma...  │
│ GAP_HOLD                            │ SL or Target      │ findExitForEma...  │
│ NR4_INSIDE_BAR                      │ SL or Target      │ findExitForEma...  │
│ HH_HL_STRUCTURE                     │ SL or Target      │ findExitForEma...  │
│ FAILED_BREAKDOWN                    │ SL or Target      │ findExitForEma...  │
│ BB_SQUEEZE                          │ SL or Target      │ findExitForEma...  │
└─────────────────────────────────────┴───────────────────┴────────────────────┘
```

---

## Related Files

- `quantlab-api/src/main/java/com/quantlab/backend/strategy/PaperTradingEngine.java`
- `quantlab-api/src/main/java/com/quantlab/backend/strategy/impl/*.java`

---

## Document Version

- Created: 2026-02-05
- For: QuantLab Strategy Framework
