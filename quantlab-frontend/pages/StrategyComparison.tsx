
import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import { Strategy, MarketType, StrategyComparisonResponse, StrategyMetrics } from '../types';

type MetricKey = keyof StrategyMetrics;

interface MetricConfig {
  key: MetricKey;
  label: string;
  format: (value: number) => string;
  higherIsBetter: boolean;
  color: string;
}

const METRICS: MetricConfig[] = [
  { key: 'totalTrades', label: 'Total Trades', format: (v) => v.toString(), higherIsBetter: false, color: 'slate' },
  { key: 'winRate', label: 'Win Rate', format: (v) => `${v.toFixed(1)}%`, higherIsBetter: true, color: 'emerald' },
  { key: 'avgReturn', label: 'Avg Return', format: (v) => `${v.toFixed(2)}%`, higherIsBetter: true, color: 'blue' },
  { key: 'totalPnl', label: 'Total P&L', format: (v) => `$${v.toFixed(2)}`, higherIsBetter: true, color: 'violet' },
  { key: 'maxDrawdown', label: 'Max Drawdown', format: (v) => `${v.toFixed(2)}%`, higherIsBetter: false, color: 'rose' },
  { key: 'sharpeRatio', label: 'Sharpe Ratio', format: (v) => (v ?? 0).toFixed(2), higherIsBetter: true, color: 'amber' },
  { key: 'profitFactor', label: 'Profit Factor', format: (v) => v.toFixed(2), higherIsBetter: true, color: 'cyan' },
  { key: 'avgWin', label: 'Avg Win', format: (v) => `$${v.toFixed(2)}`, higherIsBetter: true, color: 'emerald' },
  { key: 'avgLoss', label: 'Avg Loss', format: (v) => `$${v.toFixed(2)}`, higherIsBetter: false, color: 'rose' },
];

const StrategyComparison: React.FC = () => {
  const [strategies, setStrategies] = useState<Strategy[]>([]);
  const [selectedStrategies, setSelectedStrategies] = useState<Set<string>>(new Set());
  const [market, setMarket] = useState<MarketType>('US');
  const [startDate, setStartDate] = useState('2023-01-01');
  const [endDate, setEndDate] = useState('2024-12-31');

  const [comparisonData, setComparisonData] = useState<StrategyComparisonResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [initializing, setInitializing] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStrategies = async () => {
      try {
        const data = await apiService.getStrategies();
        setStrategies(data);
      } catch (err) {
        setError('Failed to load strategies');
        console.error(err);
      } finally {
        setInitializing(false);
      }
    };
    fetchStrategies();
  }, []);

  const toggleStrategy = (code: string) => {
    const newSelected = new Set(selectedStrategies);
    if (newSelected.has(code)) {
      if (selectedStrategies.size > 1) {
        newSelected.delete(code);
      }
    } else {
      newSelected.add(code);
    }
    setSelectedStrategies(newSelected);
  };

  const handleRunComparison = async () => {
    if (selectedStrategies.size < 2) {
      setError('Please select at least 2 strategies to compare');
      return;
    }

    setLoading(true);
    setError(null);
    setComparisonData(null);

    try {
      const data = await apiService.compareStrategies({
        strategyCodes: Array.from(selectedStrategies),
        market,
        startDate,
        endDate
      });
      setComparisonData(data);
    } catch (err) {
      setError('Failed to run comparison. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const getBestValue = (metricKey: MetricKey): number | null => {
    if (!comparisonData || comparisonData.metrics.length === 0) return null;

    const values = comparisonData.metrics.map(m => (m[metricKey] as number) ?? 0);
    const metric = METRICS.find(m => m.key === metricKey);

    if (metric?.higherIsBetter) {
      return Math.max(...values);
    } else {
      return Math.min(...values);
    }
  };

  const renderMetricCell = (metrics: StrategyMetrics, metricConfig: MetricConfig) => {
    const value = metrics[metricConfig.key] as number;
    const bestValue = getBestValue(metricConfig.key);
    const isBest = bestValue !== null && Math.abs(value - bestValue) < 0.0001;

    const valueColor = value > 0 ? 'text-emerald-400' : value < 0 ? 'text-rose-400' : 'text-slate-400';
    const bgClass = isBest ? 'bg-emerald-500/5 border-emerald-500/30' : 'bg-slate-800/30 border-slate-700/50';

    return (
      <div
        key={metricConfig.key}
        className={`px-4 py-3 border rounded-lg ${bgClass} ${isBest ? 'ring-1 ring-emerald-500/30' : ''}`}
      >
        <div className="text-[10px] uppercase text-slate-500 font-semibold mb-1">
          {metricConfig.label}
        </div>
        <div className={`text-lg font-bold ${valueColor}`}>
          {metricConfig.format(value ?? 0)}
        </div>
        {isBest && (
          <div className="text-[9px] text-emerald-400 font-semibold mt-0.5">
            BEST
          </div>
        )}
      </div>
    );
  };

  const renderStrategyCard = (metrics: StrategyMetrics) => {
    const strategy = strategies.find(s => s.code === metrics.strategyCode);

    return (
      <div
        key={metrics.strategyCode}
        className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden"
      >
        <div className="px-6 py-4 bg-slate-800/50 border-b border-slate-800">
          <div className="flex items-start justify-between">
            <div>
              <h3 className="text-lg font-bold text-slate-100">
                {metrics.strategyName}
              </h3>
              <p className="text-xs text-slate-500 font-mono mt-0.5">
                {metrics.strategyCode}
              </p>
            </div>
            {strategy?.supportsScreening && (
              <span className="px-2 py-1 bg-blue-500/10 text-blue-400 text-[10px] font-bold uppercase rounded border border-blue-500/20">
                Screening
              </span>
            )}
          </div>
        </div>

        <div className="p-6">
          <div className="grid grid-cols-2 lg:grid-cols-3 gap-4">
            {METRICS.map(metricConfig => renderMetricCell(metrics, metricConfig))}
          </div>
        </div>
      </div>
    );
  };

  const renderComparisonTable = () => {
    if (!comparisonData) return null;

    return (
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="px-6 py-4 bg-slate-800/50 border-b border-slate-800">
          <h3 className="text-sm font-bold text-slate-200 uppercase tracking-wider">
            Side-by-Side Comparison
          </h3>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left">
            <thead>
              <tr className="text-[10px] uppercase tracking-widest text-slate-500 font-bold border-b border-slate-800">
                <th className="px-4 py-3 font-normal sticky left-0 bg-slate-900 z-10 border-r border-slate-800">
                  Metric
                </th>
                {comparisonData.metrics.map(metrics => (
                  <th key={metrics.strategyCode} className="px-4 py-3 font-normal min-w-[140px]">
                    {metrics.strategyCode}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {METRICS.map(metricConfig => {
                const bestValue = getBestValue(metricConfig.key);

                return (
                  <tr key={metricConfig.key} className="border-b border-slate-800/50 last:border-0">
                    <td className="px-4 py-3 text-sm font-semibold text-slate-300 sticky left-0 bg-slate-900 z-10 border-r border-slate-800">
                      {metricConfig.label}
                    </td>
                    {comparisonData.metrics.map(metrics => {
                      const value = metrics[metricConfig.key] as number;
                      const isBest = bestValue !== null && Math.abs(value - bestValue) < 0.0001;
                      const valueColor = value > 0 ? 'text-emerald-400' : value < 0 ? 'text-rose-400' : 'text-slate-400';

                      return (
                        <td
                          key={metrics.strategyCode}
                          className={`px-4 py-3 text-sm mono ${valueColor} ${isBest ? 'bg-emerald-500/5 font-bold' : ''}`}
                        >
                          {metricConfig.format(value ?? 0)}
                          {isBest && ' ★'}
                        </td>
                      );
                    })}
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    );
  };

  if (initializing) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <div className="text-slate-500 animate-pulse">Loading strategies...</div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header Section */}
      <div>
        <h2 className="text-2xl font-bold text-slate-100 mb-2">Strategy Comparison</h2>
        <p className="text-slate-500 text-sm">
          Compare multiple strategies side-by-side to identify the best performing approach
        </p>
      </div>

      {/* Controls Section */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Market Selection */}
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-2">
              Market
            </label>
            <div className="grid grid-cols-2 gap-2">
              {(['INDIA', 'US'] as MarketType[]).map(m => (
                <button
                  key={m}
                  onClick={() => setMarket(m)}
                  className={`px-4 py-2 rounded-lg border text-sm font-semibold transition-colors ${
                    market === m
                      ? 'bg-emerald-500 border-emerald-500 text-slate-950'
                      : 'bg-slate-800 border-slate-700 text-slate-400 hover:border-slate-600'
                  }`}
                >
                  {m}
                </button>
              ))}
            </div>
          </div>

          {/* Date Range */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-2">
                Start Date
              </label>
              <input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-slate-100 text-sm mono focus:outline-none focus:ring-2 focus:ring-emerald-500/50 transition-all"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-2">
                End Date
              </label>
              <input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-slate-100 text-sm mono focus:outline-none focus:ring-2 focus:ring-emerald-500/50 transition-all"
              />
            </div>
          </div>
        </div>

        {/* Strategy Selection */}
        <div className="mt-6">
          <label className="block text-xs font-bold text-slate-500 uppercase mb-2">
            Select Strategies to Compare (min 2)
          </label>
          <div className="flex flex-wrap gap-2 p-4 bg-slate-800 border border-slate-700 rounded-lg min-h-[60px]">
            {strategies.map((strategy) => (
              <button
                key={strategy.code}
                onClick={() => toggleStrategy(strategy.code)}
                disabled={!selectedStrategies.has(strategy.code) && selectedStrategies.size >= 4}
                className={`px-3 py-2 rounded-md text-xs font-semibold transition-all flex items-center gap-2 ${
                  selectedStrategies.has(strategy.code)
                    ? 'bg-emerald-500 text-slate-950 shadow-lg shadow-emerald-500/20'
                    : 'bg-slate-700 text-slate-400 hover:bg-slate-600 hover:text-slate-200'
                }`}
              >
                <span>{strategy.code}</span>
                {strategy.supportsScreening && (
                  <span className="text-[9px] opacity-70">🔍</span>
                )}
              </button>
            ))}
            {strategies.length === 0 && (
              <span className="text-slate-500 text-xs italic py-2">
                No strategies available
              </span>
            )}
          </div>
          <p className="text-xs text-slate-500 mt-1.5">
            {selectedStrategies.size} strategy{selectedStrategies.size !== 1 ? 'ies' : ''} selected
          </p>
        </div>

        {/* Run Button */}
        <div className="mt-6">
          <button
            onClick={handleRunComparison}
            disabled={loading || selectedStrategies.size < 2}
            className={`w-full sm:w-auto px-8 py-3 rounded-xl font-bold text-base transition-all ${
              loading || selectedStrategies.size < 2
                ? 'bg-slate-800 text-slate-500 cursor-not-allowed border border-slate-700'
                : 'bg-emerald-500 hover:bg-emerald-400 text-slate-950 shadow-xl shadow-emerald-500/10 active:scale-[0.98]'
            }`}
          >
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <span className="animate-spin">⟳</span>
                Running Comparison...
              </span>
            ) : (
              'Compare Strategies'
            )}
          </button>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div className="bg-rose-500/10 border border-rose-500/20 rounded-xl p-4 text-rose-400 text-sm">
          {error}
        </div>
      )}

      {/* Results Section */}
      {comparisonData && !loading && (
        <div className="space-y-8">
          {/* Summary Header */}
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-bold text-slate-100">
                Comparison Results
              </h3>
              <p className="text-sm text-slate-500">
                {market} Market • {new Date(startDate).toLocaleDateString()} - {new Date(endDate).toLocaleDateString()}
              </p>
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-emerald-400">
                {comparisonData.metrics.length}
              </div>
              <div className="text-xs text-slate-500 uppercase font-semibold">
                Strategies Compared
              </div>
            </div>
          </div>

          {/* Strategy Cards */}
          <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
            {comparisonData.metrics.map(renderStrategyCard)}
          </div>

          {/* Comparison Table */}
          {renderComparisonTable()}
        </div>
      )}

      {/* Empty State */}
      {!comparisonData && !loading && !error && (
        <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-12 text-center">
          <div className="text-5xl mb-6">📊</div>
          <h3 className="text-xl font-bold text-slate-200 mb-2">
            Ready to Compare
          </h3>
          <p className="text-slate-500 max-w-md mx-auto mb-6">
            Select 2 or more strategies, choose your market and date range, then run a comparison to see detailed performance metrics side-by-side.
          </p>
          <div className="flex items-center justify-center gap-6 text-xs text-slate-600 uppercase font-semibold">
            <span className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              Best Value Highlighted
            </span>
            <span className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-blue-500"></span>
              Screening Support
            </span>
            <span className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-amber-500"></span>
              Multi-Strategy
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default StrategyComparison;
