
export type MarketType = 'INDIA' | 'US';

export interface Instrument {
  id: number;
  symbol: string;
  market: MarketType;
}

export interface Strategy {
  id: number;
  code: string;
  name: string;
}

export interface StrategyRunRequest {
  strategyCode: string;
  market: MarketType;
  startDate: string;
  endDate: string;
  params: Record<string, any>;
}

export interface StrategyRunResponse {
  strategyRunId: number;
  status: 'COMPLETED' | 'FAILED' | 'RUNNING';
}

export interface Signal {
  symbol: string;
  signalDate: string;
  side: 'BUY' | 'SELL';
  entryPrice: number;
  stopLoss: number;
  targetPrice: number;
  quantity: number;
  strategy?: string;
}

export interface PaperTrade {
  symbol: string;
  entryDate: string;
  exitDate: string;
  entryPrice: number;
  exitPrice: number;
  quantity: number;
  pnl: number;
  pnlPct: number;
}

export interface AnalyticsSummary {
  totalTrades: number;
  winRate: number;
  totalPnl: number;
  maxDrawdown: number;
}

export interface EquityCurvePoint {
  date: string;
  equity: number;
}
