Yahoo Finance Integration Guide
Overview
This document provides instructions for integrating Yahoo Finance as an auxiliary data source for backtesting and screening operations. Yahoo Finance supplements—but does not replace—the canonical data sources (Bhavcopy and Stooq).

Purpose and Scope
What Yahoo Finance Provides
Capability	Description
Recent data coverage	Days not yet available in Bhavcopy
Live screening	Current day data for "trade tomorrow" decisions
Data validation	Cross-reference for missing data fill
Quick lookup	Fast access without database queries
Data Available from Yahoo Finance API
OHLC (Open, High, Low, Close)
Volume
Timestamps
Corporate actions (partial)
Intraday or daily candles
Known Limitations
Limitation	Impact
No permanence guarantee	Data may become unavailable
No SLA	Uptime not guaranteed
Rate limiting	Requests may be throttled
Data gaps	Occasional missing volume or trading days
Warning: Never use Yahoo Finance as your long-term database source.

Data Source Hierarchy
Canonical Data Rules
Purpose	Authorized Source
Historical backtests	Bhavcopy / Stooq (Database)
Long-term analytics	Database only
Screening (current/recent)	Yahoo Finance → in-memory
Paper trades	Database
Strategy logic	Source-agnostic
Critical: Yahoo Finance data must never directly drive trade execution.

Architecture
Unified Data Access Layer
Implement a single abstraction layer for all market data access.

Interface Definition
Java

public interface MarketDataProvider {
    List<Candle> getDailyCandles(
        String symbol,
        LocalDate from,
        LocalDate to
    );
}
Implementation Classes
Class	Purpose
DbMarketDataProvider	Fetches from Bhavcopy / Stooq
YahooMarketDataProvider	Fetches live / recent data
CompositeMarketDataProvider	Merges data from both sources
Implementation
Composite Provider Logic
Java

class CompositeMarketDataProvider implements MarketDataProvider {

    DbMarketDataProvider db;
    YahooMarketDataProvider yahoo;

    public List<Candle> getDailyCandles(String symbol, LocalDate from, LocalDate to) {

        List<Candle> dbCandles = db.fetch(symbol, from, to);
        LocalDate lastDbDate = maxDate(dbCandles);

        if (lastDbDate == null || lastDbDate.isBefore(to)) {
            List<Candle> yahooCandles = yahoo.fetch(
                symbol, 
                lastDbDate.plusDays(1), 
                to
            );
            return mergeAndSort(dbCandles, yahooCandles);
        }

        return dbCandles;
    }
}
Merge Rules
Database candle takes precedence when dates overlap
Yahoo Finance fills missing dates only
No automatic persistence to database
Yahoo Finance API Specification
Endpoint Pattern
text

https://query1.finance.yahoo.com/v8/finance/chart/{SYMBOL}
Required Parameters
Parameter	Value	Description
interval	1d	Daily candles
range	3mo	Time range (alternative: use period1/period2)
Example Request
text

https://query1.finance.yahoo.com/v8/finance/chart/RELIANCE.NS?interval=1d&range=3mo
Response Mapping
Yahoo Field	Candle Property
timestamp[]	tradeDate
open[]	open
high[]	high
low[]	low
close[]	close
volume[]	volume
Note: Convert timestamp (epoch seconds) to LocalDate using exchange timezone.

Operational Workflows
Screening Flow (Live/Current Day)
Objective: Determine trading candidates for the next session.

text

1. User initiates "Run Screening"
           ↓
2. Load active symbols from database
           ↓
3. For each symbol:
   candles = CompositeMarketDataProvider.getDailyCandles(
       symbol,
       today - lookbackDays,
       today
   )
           ↓
4. Execute strategy in SCREEN mode
           ↓
5. Persist results to screening_results table
UI Requirements:

Label data source clearly: "Data as of YYYY-MM-DD (Yahoo Finance)"
Strategy logic remains unaware of data source
Backtesting Flow (Strict Mode)
Yahoo Finance is used only when database has missing days.

text

1. Initiate backtest
           ↓
2. Fetch candles via Composite provider
           ↓
3. Primary source: Database candles (~99%)
           ↓
4. Fallback: Yahoo Finance (last 1-2 days if needed)
           ↓
5. Execute strategy in BACKTEST mode
           ↓
6. Persist paper trades
Optional Configuration:

YAML

backtest:
  allowYahooFallback: false
Data Persistence Policy
Rules
Action	Permitted
Auto-store Yahoo data	No
Manual import via admin job	Yes
Silent persistence	No
Rationale for Restrictions
Data drift between sources
Corporate action mismatches
Volume inconsistencies
Approved Import Method
Create an explicit administrative job:

text

"Import Yahoo data for [DATE] if Bhavcopy missing"
All imports must be logged and auditable.

Strategy Integration
Requirements
Strategy code must:

Not detect data source
Not branch based on source
Access data through unified interface only
Correct Implementation
Java

List<Candle> candles = context.getCandles();
// Strategy logic operates here
// No source awareness required
User Interface Guidelines
Screening Display
text

DATA SOURCE: DB + Yahoo Finance
AS OF: 2026-02-03
Tooltip Text
"Latest candle sourced from Yahoo Finance. Final prices may vary from exchange Bhavcopy."

Error Handling and Safety
Required Safeguards
Safeguard	Implementation
Caching	In-memory cache keyed by symbol + date
Retry logic	Single retry on failure
Timeout	Maximum 3 seconds
Circuit breaker	Prevent cascade failures
Failure Handling
When Yahoo Finance fails:

Skip the affected symbol
Log warning with details
Continue screening remaining symbols
Never fail the entire run
Configuration Reference
YAML

marketData:
  providers:
    primary: database
    fallback: yahoo
    
  yahoo:
    baseUrl: "https://query1.finance.yahoo.com/v8/finance/chart"
    timeout: 3000
    retryCount: 1
    cacheEnabled: true
    cacheTtlMinutes: 15
    
  backtest:
    allowYahooFallback: true
    
  screening:
    requireLatestData: true
Deliverables Checklist
After implementation, verify the following:

 Unified market data access layer operational
 Zero modifications required to strategy code
 Live screening functional
 Backtesting uses database as primary source
 No paid API dependencies
 Yahoo Finance replaceable with alternative providers (Polygon, AlphaVantage, IBKR)
Summary
Component	Principle
Database	Source of truth
Yahoo Finance	Convenience layer
Strategy	Source-blind
Framework	Controls data flow
