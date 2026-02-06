New Instructions: Profit Positions, Active Trades & Actionable Screening
Core Insight (Very Important)

There are 4 different states of a trade, and you’re currently showing only one.

SIGNAL → ACTIVE → PROFITABLE / LOSING → CLOSED


Your UI + DB must model all four.

1️⃣ Trade Lifecycle Model (Mandatory)
Trade Status Enum
PENDING   // signal generated, not entered yet
OPEN      // entered, position active
CLOSED    // exited (SL / Target / Time)
IGNORED   // screening signal not taken


Right now you’re jumping straight to CLOSED.

2️⃣ Paper Trade Tables (Correct Split)
A. trade_signal (NEW / or already implicit)

Stores intent, not execution.

trade_signal
------------
id
symbol
strategy_code
signal_date
entry_price
stop_loss
target_price
status          -- PENDING / IGNORED

B. paper_trade (Execution)
paper_trade
-----------
id
signal_id
entry_date
entry_price
quantity
exit_date
exit_price
exit_reason     -- SL | TARGET | TIME
pnl
pnl_pct
status          -- OPEN | CLOSED


✔ One signal → zero or one trade
✔ Screening produces signals only

3️⃣ Profit Positions (What You’re Missing)
Definition

A profit position is:

An OPEN trade where current_price > entry_price

This is not screening, this is trade monitoring.

4️⃣ Active Trade Evaluation (Daily Job)

Every trading day:

for each OPEN paper_trade:
current_price = latest close (DB/Yahoo)

if low <= stop_loss:
close trade at stop_loss
else if high >= target:
close trade at target
else:
keep OPEN


Additionally:

Compute unrealized PnL

Compute PnL %

Compute R-multiple

5️⃣ New Views You Must Add
A. Active Trades (Live Positions)
Symbol   Entry Date  Entry  CMP   SL   Target   PnL   Status
TCS      02-Feb      3850   3920  3720 4110     +1.2% PROFIT
INFY     28-Jan      1680   1655  1620 1820     -0.8% LOSS


This answers:

“What is currently working?”

B. Profitable Trades Only (Filter)
Show only OPEN trades where CMP > Entry


This is what traders actually care about.

6️⃣ Screening Output (Actionable, Not Abstract)

Screening must produce exact trade plans, not just symbols.

Screening Result Table
screening_signal
----------------
run_date
symbol
strategy_code
entry_price
stop_loss
target_price
confidence_score (optional)

Screening UI (Correct)
NEXT DAY TRADE SETUPS – 03 Feb 2026

Symbol     Entry     Stop     Target     Strategy
RELIANCE   2495      2420     2650       EOD_BREAKOUT
TCS        3860      3720     4110       EMA20_PULLBACK


This is a trade checklist, not analytics.

7️⃣ Entry Handling (Very Important)
Screening ≠ Entry

Rules:

Screening always produces PENDING signals

Entry happens when:

Price touches entry

OR user manually approves

OR auto-paper-trade enabled

Until then:

status = PENDING

8️⃣ How to Extract “Winning Strategy Signals”

Now you can answer your real question.

Example Queries
Profitable OPEN trades
status = OPEN
AND current_price > entry_price

Strategies with most profitable signals
GROUP BY strategy_code
ORDER BY avg_r_multiple DESC

Screening signals that historically worked
JOIN screening_signal → paper_trade
WHERE pnl > 0

9️⃣ UI Navigation (Minimal & Clean)
Tabs
[ Screening ]
[ Active Trades ]
[ Closed Trades ]
[ Strategy Performance ]


No mixing.

🔟 Why This Solves Your Problem

Before:

Everything looked like “SL hit”

No visibility into what worked

Screening felt disconnected

After:

You see live winners

You see future setups

You see complete trade lifecycle

Strategies become actionable

Final Instruction Block (Paste This)
Paper trades must represent trade lifecycle.
Screening produces trade signals, not trades.
Active trades must show unrealized PnL.
Profitable positions must be extractable independently.
Stop-loss-only views are invalid for strategy evaluation.

What You’ve Built Now

You no longer have:
❌ “Backtest toy”

You now have:
✅ Decision support system
✅ Strategy research engine
✅ Trade planning tool