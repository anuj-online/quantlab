# Position Management Scheduler

## Overview

The Position Management Scheduler is an automated system that manages open positions in QuantLab's paper trading environment. It runs on scheduled intervals to update unrealized P&L, check stop loss/target levels, and execute trades based on trigger conditions.

## Architecture

```
SchedulingConfiguration
    - Enables @EnableScheduling annotation
    - Allows detection of @Scheduled annotations

PositionManagementScheduler
    - Core scheduled task component
    - Uses TradeService for all position operations

PositionManagementController
    - REST endpoints for manual triggering
    - Useful for testing and ad-hoc updates
```

## Scheduled Tasks

### 1. Market Close Position Management
**Schedule:** Daily at 4:00 PM EST (Monday-Friday)
**Cron:** `0 0 16 * * MON-FRI`

**Operations:**
- Updates unrealized P&L for all open positions
- Checks stop loss levels and closes positions if hit
- Checks target levels and closes positions if hit
- Checks pending signals for entry trigger execution

### 2. Intraday Position Monitoring (Optional)
**Schedule:** Every 15 minutes during market hours
**Status:** Disabled by default

**Operations:**
- Updates current prices for open positions
- Checks if positions hit SL/target during trading day

**Enable via application.properties:**
```properties
position.scheduler.intraday.enabled=true
```

### 3. End-of-Day Reconciliation
**Schedule:** Daily at 5:00 PM EST (Monday-Friday)
**Cron:** `0 0 17 * * MON-FRI`

**Operations:**
- Final P&L update for the day
- Verify all SL/target exits were processed
- Log summary of open positions

### 4. Weekly Maintenance
**Schedule:** Every Sunday at 10:00 PM EST
**Cron:** `0 0 22 * * SUN`

**Operations:**
- Update all positions with latest prices
- Weekly data cleanup and preparation

## REST API Endpoints

### Manual Position Update
```
POST /api/v1/positions/update
```
Triggers a complete position management update (same as scheduled task).

### Update P&L Only
```
POST /api/v1/positions/update-pnl
```
Updates unrealized P&L without triggering exits or signal execution.

### Check Exit Conditions
```
POST /api/v1/positions/check-exits
```
Checks stop loss and target levels, closes positions if hit.

### Check Entry Triggers
```
POST /api/v1/positions/check-entries
```
Checks pending signals and executes those that hit entry price.

### Health Check
```
POST /api/v1/positions/health
```
Returns the health status of the position management system.

## Configuration

### Application Properties

```properties
# Market close schedule (default: 4:00 PM EST, Mon-Fri)
position.scheduler.cron=0 0 16 * * MON-FRI

# End-of-day reconciliation schedule (default: 5:00 PM EST, Mon-Fri)
position.scheduler.eod.cron=0 0 17 * * MON-FRI

# Enable intraday monitoring (default: false)
position.scheduler.intraday.enabled=false
```

### Market-Specific Schedules

#### US Market (NYSE/NASDAQ)
- Market Hours: 9:30 AM - 4:00 PM EST
- Market Close: `0 0 16 * * MON-FRI`
- EOD Reconciliation: `0 0 17 * * MON-FRI`

#### India Market (NSE/BSE)
- Market Hours: 9:15 AM - 3:30 PM IST
- Market Close: `0 30 15 * * MON-FRI`
- EOD Reconciliation: `0 0 16 * * MON-FRI`

## TradeService Integration

The scheduler uses the following TradeService methods:

1. **updateUnrealizedPnL()**
   - Fetches current prices for all open positions
   - Calculates unrealized P&L in currency and percentage
   - Updates current price and R-multiple fields

2. **checkStopLossAndTargets()**
   - Fetches current prices for all open positions
   - Compares with stop loss and target levels
   - Closes positions that hit either level

3. **checkEntryTriggers()**
   - Fetches current prices for all pending signals
   - Executes signals where price has reached entry level
   - Converts signals to open paper trades

## Error Handling

All scheduled tasks include comprehensive error handling:
- Errors are logged but don't stop the scheduler
- Individual trade/signal errors don't affect others
- Continues processing remaining items after failures

## Logging

The scheduler uses SLF4J for logging at different levels:
- INFO: Task start/completion summaries
- DEBUG: Detailed operation logging
- ERROR: Error conditions and exceptions

## Monitoring

### Key Metrics to Monitor

1. **Execution Duration**: How long each task takes
2. **Positions Updated**: Number of positions processed
3. **Exits Triggered**: Number of positions closed at SL/target
4. **Signals Executed**: Number of signals converted to trades
5. **Error Rate**: Frequency of errors during execution

### Recommended Monitoring Setup

- Use Spring Actuator metrics endpoint
- Set up alerts for failed tasks
- Monitor market data provider availability
- Track P&L changes over time

## Testing

### Manual Testing

Use the REST endpoints to test manually:
```bash
# Test P&L update
curl -X POST http://localhost:8080/api/v1/positions/update-pnl

# Test exit checks
curl -X POST http://localhost:8080/api/v1/positions/check-exits

# Test complete update
curl -X POST http://localhost:8080/api/v1/positions/update
```

### Automated Testing

Create test scenarios with:
- Known open positions with specific SL/target levels
- Pending signals at various trigger prices
- Mock market data provider returning specific prices

## Best Practices

1. **Schedule During Market Hours**: Run tasks when market data is available
2. **Avoid Overlapping Tasks**: Ensure tasks don't overlap and cause conflicts
3. **Handle Market Holidays**: Consider market calendars in cron expressions
4. **Monitor Performance**: Track execution time and optimize if needed
5. **Test Thoroughly**: Test with various market conditions before production
6. **Log Appropriately**: Balance between too much and too little logging
7. **Handle Failures Gracefully**: Don't let one failure stop the entire system

## Troubleshooting

### Common Issues

1. **Tasks Not Running**
   - Check if @EnableScheduling is present
   - Verify cron expression syntax
   - Check application properties are loaded

2. **Positions Not Updating**
   - Verify market data provider is working
   - Check for errors in application logs
   - Ensure positions are in OPEN status

3. **Exits Not Triggering**
   - Verify stop loss and target prices are set
   - Check current price comparison logic
   - Review trade status changes

4. **Performance Issues**
   - Consider batch processing for large position counts
   - Optimize database queries
   - Add caching for market data

## Future Enhancements

- [ ] Add market calendar integration for holidays
- [ ] Implement parallel processing for large position counts
- [ ] Add email notifications for important events
- [ ] Create dashboard for scheduler status
- [ ] Support multiple market-specific schedules
- [ ] Add position aging alerts for long-held trades
- [ ] Implement trailing stop loss support
- [ ] Add position size management rules
