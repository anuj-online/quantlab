Below is a single clean strategy-definition file.

This is framework-friendly:

Same inputs (candles, indicators)

Same outputs (signals)

Works for both screening + backtesting

No execution assumptions

Highly practical (not academic fluff)

STRATEGIES.md
Strategy Design Rules (applies to all below)

Timeframe: Daily (EOD)

Data: open, high, low, close, volume

Execution modes:

BACKTEST: evaluate every candle

SCREEN: evaluate only latest candle

Signal generated only on confirmation candle

All strategies output:

symbol

signalDate

side (BUY)

entry

stopLoss

target

strategyCode

1. EOD Breakout with Volume

Code: EOD_BREAKOUT_VOL

Logic

Close > highest high of last 20 days

Volume > 1.5 × 20-day average volume

Entry

Close of signal candle

Stop Loss

Lowest low of last 20 days

Target

2 × risk

Best for

Trending markets

Swing trades (5–20 days)

2. SMA 20 / SMA 50 Bullish Crossover

Code: SMA_20_50_CROSS

Logic

SMA20 crosses above SMA50 on current candle

Entry

Close

Stop Loss

SMA50

Target

3 × risk or trailing SMA20

Best for

Medium-term trend capture

Screening

3. NR4 + Inside Bar Breakout

Code: NR4_INSIDE

Logic

Today range = smallest of last 4 days

Today is inside previous candle

Entry

Tomorrow breakout above high

Stop Loss

Low of inside bar

Target

2 × range

Best for

Volatility expansion

Next-day screening

4. High Volume Reversal (Climax)

Code: HV_REVERSAL

Logic

Volume ≥ 2× 20-day avg

Close near low

Followed by bullish candle next day

Entry

Close of confirmation candle

Stop Loss

Recent swing low

Target

Previous resistance

Best for

Mean reversion

Reversal plays

5. Trend Pullback to EMA20

Code: EMA20_PULLBACK

Logic

Price above EMA50

Pullback touches EMA20

Bullish close

Entry

Close

Stop Loss

EMA20 − buffer

Target

Previous swing high

Best for

Strong trends

High probability setups

6. Volume Expansion Breakout (Box Range)

Code: RANGE_BREAK_VOL

Logic

Price in tight range (last 10 days)

Breakout candle with volume > 2×

Entry

Close

Stop Loss

Range low

Target

Range height projection

Best for

Consolidation breakouts

7. Relative Strength Momentum (RS)

Code: REL_STRENGTH_30D

Logic

Stock return (30d) > Index return (30d)

Breaks recent high

Entry

Close

Stop Loss

Last swing low

Target

Trail stop

Best for

Market leaders

Screening

8. Gap-Up Hold (Continuation)

Code: GAP_HOLD

Logic

Gap up > 1.5%

First 30–50% of range holds

Close near high

Entry

Close

Stop Loss

Day’s low

Target

1.5–2× risk

Best for

Strong news momentum

9. Bollinger Band Squeeze + Breakout

Code: BB_SQUEEZE

Logic

Bollinger Band width lowest in 20 days

Breakout close outside band

Entry

Close

Stop Loss

Middle band

Target

2× squeeze range

Best for

Volatility breakouts

10. Higher High Higher Low (Structure)

Code: HH_HL_STRUCTURE

Logic

Last 3 swing highs increasing

Last 3 swing lows increasing

Entry

Pullback close

Stop Loss

Latest swing low

Target

Trail structure

Best for

Clean trend identification

11. Failed Breakdown (Bear Trap)

Code: FAILED_BREAKDOWN

Logic

Price breaks support

Closes back above next day

High volume on recovery

Entry

Close of recovery candle

Stop Loss

Breakdown low

Target

Resistance zone

Best for

Strong reversals

12. 3-Day Momentum Burst

Code: MOMENTUM_3D

Logic

3 consecutive bullish candles

Increasing volume

Close > 10-day high

Entry

Close

Stop Loss

Lowest of last 3 days

Target

1.5–2×

Best for

Short swing bursts

Strategy Metadata (for UI)

Each strategy exposes:

{
  "code": "NR4_INSIDE",
  "name": "NR4 Inside Bar Breakout",
  "type": "BOTH",
  "minLookbackDays": 20,
  "supportsScreening": true
}

How UI Should Show Strategies
Strategy List Page
[✓] EOD Breakout with Volume
[✓] SMA 20/50 Crossover
[✓] NR4 Inside Bar
[✓] EMA20 Pullback
...

Screening Output
DATE: 2026-02-03

NR4_INSIDE
✔ TCS
✔ RELIANCE

EMA20_PULLBACK
✔ INFY

Key Architectural Confirmation

✔ Inputs fixed
✔ Outputs fixed
✔ Strategy logic isolated
✔ Easy Claude generation
✔ Recompile → re-run

This is exactly how institutional strategy libraries are built.