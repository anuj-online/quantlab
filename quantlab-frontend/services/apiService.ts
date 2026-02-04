
import { 
  Instrument, Strategy, StrategyRunRequest, StrategyRunResponse, 
  Signal, PaperTrade, AnalyticsSummary, EquityCurvePoint, MarketType 
} from '../types';

/**
 * INSTRUCTIONS FOR BACKEND INTEGRATION:
 * 1. Replace the implementation of each function with a standard fetch/axios call.
 * 2. Ensure the base URL matches your API endpoint.
 * 3. The service currently returns mock data to demonstrate functionality.
 */

const BASE_URL = '/api'; // Placeholder for actual backend URL

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

export const apiService = {
  async getInstruments(market: MarketType): Promise<Instrument[]> {
    await delay(300);
    // Mocking response based on market
    const base = market === 'INDIA' 
      ? [{ id: 1, symbol: 'INFY', market }, { id: 2, symbol: 'RELIANCE', market }, { id: 3, symbol: 'TCS', market }]
      : [{ id: 1, symbol: 'AAPL', market }, { id: 2, symbol: 'TSLA', market }, { id: 3, symbol: 'MSFT', market }];
    return base;
  },

  async getStrategies(): Promise<Strategy[]> {
    await delay(300);
    return [
      { id: 1, code: 'EMA_CROSS', name: 'EMA Crossover' },
      { id: 2, code: 'RSI_REVERSAL', name: 'RSI Reversal' },
      { id: 3, code: 'BOLLINGER_BREAK', name: 'Bollinger Band Breakout' }
    ];
  },

  async runStrategy(payload: StrategyRunRequest): Promise<StrategyRunResponse> {
    await delay(1000); // Simulate processing
    return {
      strategyRunId: Math.floor(Math.random() * 1000),
      status: 'COMPLETED'
    };
  },

  async getSignals(runId: number): Promise<Signal[]> {
    await delay(400);
    return [
      { symbol: 'AAPL', signalDate: '2024-05-10', side: 'BUY', entryPrice: 182.3, stopLoss: 175.0, targetPrice: 200.0, quantity: 10, strategy: 'EMA_CROSS' },
      { symbol: 'INFY', signalDate: '2024-05-12', side: 'BUY', entryPrice: 1450.5, stopLoss: 1400, targetPrice: 1550, quantity: 100, strategy: 'EMA_CROSS' },
      { symbol: 'TSLA', signalDate: '2024-05-15', side: 'BUY', entryPrice: 175.2, stopLoss: 165.0, targetPrice: 210.0, quantity: 5, strategy: 'EMA_CROSS' },
    ];
  },

  async getPaperTrades(runId: number): Promise<PaperTrade[]> {
    await delay(500);
    return [
      { symbol: 'AAPL', entryDate: '2024-05-11', exitDate: '2024-06-02', entryPrice: 183.0, exitPrice: 198.0, quantity: 10, pnl: 150.0, pnlPct: 8.2 },
      { symbol: 'TSLA', entryDate: '2024-05-18', exitDate: '2024-06-10', entryPrice: 170.0, exitPrice: 165.0, quantity: 10, pnl: -50.0, pnlPct: -2.94 },
      { symbol: 'MSFT', entryDate: '2024-04-15', exitDate: '2024-05-20', entryPrice: 410.5, exitPrice: 430.2, quantity: 20, pnl: 394.0, pnlPct: 4.8 },
      { symbol: 'NVDA', entryDate: '2024-01-10', exitDate: '2024-03-05', entryPrice: 500.0, exitPrice: 850.0, quantity: 5, pnl: 1750.0, pnlPct: 70.0 },
    ];
  },

  async getAnalytics(runId: number): Promise<AnalyticsSummary> {
    await delay(300);
    return {
      totalTrades: 120,
      winRate: 0.56,
      totalPnl: 12450,
      maxDrawdown: -0.18
    };
  },

  async getEquityCurve(runId: number): Promise<EquityCurvePoint[]> {
    await delay(300);
    return [
      { date: "2024-01-01", equity: 100000 },
      { date: "2024-02-01", equity: 102500 },
      { date: "2024-03-01", equity: 101800 },
      { date: "2024-04-01", equity: 104500 },
      { date: "2024-05-01", equity: 108200 },
      { date: "2024-06-01", equity: 112450 },
    ];
  }
};
