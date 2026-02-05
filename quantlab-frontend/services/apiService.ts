
import {
  Instrument, Strategy, StrategyRunRequest, StrategyRunResponse,
  Signal, PaperTrade, AnalyticsSummary, EquityCurvePoint, MarketType,
  ScreeningRequest, ScreeningResult, StrategyComparisonRequest,
  StrategyComparisonResponse, StrategyMetrics
} from '../types';

const BASE_URL = '/api/v1';

interface ApiError {
  message: string;
  status?: number;
}

class ApiServiceError extends Error implements ApiError {
  status?: number;

  constructor(message: string, status?: number) {
    super(message);
    this.name = 'ApiServiceError';
    this.status = status;
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const errorMessage = await response.text().catch(() => 'Unknown error');
    throw new ApiServiceError(
      errorMessage || `HTTP error! status: ${response.status}`,
      response.status
    );
  }
  return response.json();
}

export const apiService = {
  async getInstruments(market: MarketType): Promise<Instrument[]> {
    const response = await fetch(`${BASE_URL}/instruments?market=${market}&active=true`);
    return handleResponse<Instrument[]>(response);
  },

  async getStrategies(): Promise<Strategy[]> {
    const response = await fetch(`${BASE_URL}/strategies`);
    return handleResponse<Strategy[]>(response);
  },

  async runStrategy(payload: StrategyRunRequest): Promise<StrategyRunResponse> {
    const response = await fetch(`${BASE_URL}/strategies/run`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });
    return handleResponse<StrategyRunResponse>(response);
  },

  async getSignals(runId: number): Promise<Signal[]> {
    const response = await fetch(`${BASE_URL}/strategy-runs/${runId}/signals`);
    return handleResponse<Signal[]>(response);
  },

  async getPaperTrades(runId: number): Promise<PaperTrade[]> {
    const response = await fetch(`${BASE_URL}/strategy-runs/${runId}/paper-trades`);
    return handleResponse<PaperTrade[]>(response);
  },

  async getAnalytics(runId: number): Promise<AnalyticsSummary> {
    const response = await fetch(`${BASE_URL}/strategy-runs/${runId}/analytics`);
    return handleResponse<AnalyticsSummary>(response);
  },

  async getEquityCurve(runId: number): Promise<EquityCurvePoint[]> {
    const response = await fetch(`${BASE_URL}/strategy-runs/${runId}/equity-curve`);
    return handleResponse<EquityCurvePoint[]>(response);
  },

  async runScreening(strategyCodes: string[], date: string): Promise<ScreeningResult> {
    const payload: ScreeningRequest = { strategyCodes, date };
    const response = await fetch(`${BASE_URL}/screening/run`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });
    return handleResponse<ScreeningResult>(response);
  },

  async compareStrategies(payload: StrategyComparisonRequest): Promise<StrategyComparisonResponse> {
    const response = await fetch(`${BASE_URL}/strategies/compare`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });
    return handleResponse<StrategyComparisonResponse>(response);
  },

  async getStrategyMetrics(
    strategyCode: string,
    market: MarketType,
    startDate: string,
    endDate: string
  ): Promise<StrategyMetrics> {
    const params = new URLSearchParams({
      market,
      startDate,
      endDate
    });
    const response = await fetch(`${BASE_URL}/strategies/${strategyCode}/metrics?${params}`);
    return handleResponse<StrategyMetrics>(response);
  }
};
