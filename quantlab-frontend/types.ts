
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
  description?: string;
  supportsScreening: boolean;
  minLookbackDays: number;
  parameters?: StrategyParameter[];
}

export interface StrategyParameter {
  name: string;
  defaultValue: number;
  min?: number;
  max?: number;
  step?: number;
  description?: string;
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
  stopLoss: number | null;
  targetPrice: number | null;
  quantity: number;
  strategy?: string | null;
}

// Ensemble voting types
export interface EnsembleSignal {
  ensembleId: string;
  symbol: string;
  signalDate: string;
  side: string;
  entryPrice: number;
  stopLoss: number | null;
  targetPrice: number | null;
  voteScore: number;
  totalStrategies: number;
  confidenceScore: number;
  strategyVotes: Record<string, string>;
  rankScore?: number;
  rMultiple?: number;
  liquidityScore?: number;
  volatilityFit?: number;
}

export interface EnsembleResult {
  signals: EnsembleSignal[];
}

export interface EnsembleRequest {
  strategyCodes: string[];
  date: string;
  market: MarketType;
}

// Capital allocation types
export interface AllocationPosition {
  id?: number;
  symbol: string;
  quantity: number;
  capitalUsed: number;
  riskAmount: number;
  expectedR: number;
  allocationPct: number;
}

export interface CapitalAllocationSnapshot {
  id?: number;
  runDate: string;
  totalCapital: number;
  deployedCapital: number;
  freeCash: number;
  expectedRMultiple: number;
  positions: AllocationPosition[];
  createdAt?: string;
}

export interface AllocationRequest {
  date: string;
  totalCapital: number;
  riskPerTradePct: number;
  maxOpenTrades: number;
}
}

export interface PaperTrade {
  id: number;
  symbol: string;
  entryDate: string;
  exitDate?: string;
  entryPrice: number;
  exitPrice?: number;
  quantity: number;
  pnl?: number;
  pnlPct?: number;
  status: 'OPEN' | 'CLOSED';
  exitReason?: 'STOP_LOSS' | 'TARGET' | 'TIME' | 'MANUAL';
  currentPrice?: number;
  unrealizedPnl?: number;
  unrealizedPnlPct?: number;
  rMultiple?: number;
  stopLoss?: number;
  targetPrice?: number;
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

// Screening Types
export interface ScreeningRequest {
  strategyCodes: string[];
  date: string;
}

export interface ScreeningSignal {
  symbol: string;
  side: 'BUY' | 'SELL';
  entry: number;
  stopLoss: number;
  target: number;
  strategyCode: string;
}

export interface ScreeningResult {
  screeningDate: string;
  totalSignals: number;
  signalsByStrategy: Record<string, ScreeningSignal[]>;
}

export interface ScreeningStockResult {
  symbol: string;
  market: MarketType;
  signals: {
    strategyCode: string;
    signalType: 'BUY' | 'SELL' | 'HOLD';
    confidence?: number;
    reason?: string;
  }[];
}

// Strategy Comparison Types
export interface StrategyMetrics {
  strategyCode: string;
  strategyName: string;
  totalTrades: number;
  winRate: number;
  avgReturn: number;
  totalPnl: number;
  maxDrawdown: number;
  sharpeRatio?: number;
  sortinoRatio?: number;
  avgWin: number;
  avgLoss: number;
  profitFactor: number;
}

export interface StrategyComparisonRequest {
  strategyCodes: string[];
  market: MarketType;
  startDate: string;
  endDate: string;
}

export interface StrategyComparisonResponse {
  comparisonDate: string;
  market: MarketType;
  metrics: StrategyMetrics[];
  bestPerforming?: {
    metric: keyof StrategyMetrics;
    strategyCode: string;
    value: number;
  }[];
}

export interface StrategyRunMetadata {
  strategyRunId: number;
  strategyCode: string;
  strategyName: string;
  market: MarketType;
  startDate: string;
  endDate: string;
  status: 'COMPLETED' | 'FAILED' | 'RUNNING';
  createdAt: string;
  completedAt?: string;
}
