
import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import { Strategy, ScreeningSignal, ScreeningResult } from '../types';

const ScreeningDashboard: React.FC = () => {
  const [strategies, setStrategies] = useState<Strategy[]>([]);
  const [selectedStrategies, setSelectedStrategies] = useState<Set<string>>(new Set());
  const [screeningDate, setScreeningDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [results, setResults] = useState<ScreeningResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [initializing, setInitializing] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStrategies = async () => {
      try {
        const data = await apiService.getStrategies();
        setStrategies(data);
        // Default to all strategies selected
        setSelectedStrategies(new Set(data.map(s => s.code)));
      } catch (err) {
        setError('Failed to load available strategies');
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
      newSelected.delete(code);
    } else {
      newSelected.add(code);
    }
    setSelectedStrategies(newSelected);
  };

  const toggleAllStrategies = () => {
    if (selectedStrategies.size === strategies.length) {
      setSelectedStrategies(new Set());
    } else {
      setSelectedStrategies(new Set(strategies.map(s => s.code)));
    }
  };

  const handleRunScreening = async () => {
    if (selectedStrategies.size === 0) {
      setError('Please select at least one strategy');
      return;
    }

    setLoading(true);
    setError(null);
    setResults(null);

    try {
      const data = await apiService.runScreening(
        Array.from(selectedStrategies),
        screeningDate
      );
      setResults(data);
    } catch (err) {
      setError('Failed to run screening. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const renderSignalRow = (signal: ScreeningSignal) => (
    <tr
      key={`${signal.symbol}-${signal.strategyCode}`}
      className="hover:bg-slate-800/30 transition-colors text-sm border-b border-slate-800/50 last:border-0"
    >
      <td className="px-4 py-3 font-bold text-slate-200 mono">{signal.symbol}</td>
      <td className="px-4 py-3">
        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
          signal.side === 'BUY'
            ? 'bg-emerald-500/10 text-emerald-400'
            : 'bg-rose-500/10 text-rose-400'
        }`}>
          {signal.side}
        </span>
      </td>
      <td className="px-4 py-3 text-slate-300 mono">{(signal.entry ?? 0).toFixed(2)}</td>
      <td className="px-4 py-3 text-rose-400/80 mono">{(signal.stopLoss ?? 0).toFixed(2)}</td>
      <td className="px-4 py-3 text-emerald-400/80 mono">{(signal.target ?? 0).toFixed(2)}</td>
    </tr>
  );

  const renderStrategySection = (strategyCode: string, signals: ScreeningSignal[]) => {
    const strategy = strategies.find(s => s.code === strategyCode);
    const displayName = strategy?.name || strategyCode;
    const description = strategy?.description;

    return (
      <div key={strategyCode} className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="px-6 py-4 bg-slate-800/50 border-b border-slate-800">
          <div className="flex items-start justify-between">
            <div>
              <h3 className="text-sm font-bold text-slate-200 uppercase tracking-wider">
                Strategy: {displayName}
              </h3>
              {description && (
                <p className="text-xs text-slate-500 mt-1 max-w-xl">
                  {description}
                </p>
              )}
              <p className="text-xs text-slate-500 mt-1">
                {signals.length} signal{signals.length !== 1 ? 's' : ''} found
              </p>
            </div>
            {strategy?.supportsScreening && (
              <span className="px-2 py-1 bg-blue-500/10 text-blue-400 text-[10px] font-bold uppercase rounded border border-blue-500/20">
                Screening Enabled
              </span>
            )}
          </div>
        </div>

        {signals.length === 0 ? (
          <div className="p-8 text-center text-slate-500 italic">
            No signals generated for this strategy on the selected date.
          </div>
        ) : (
          <table className="w-full text-left">
            <thead>
              <tr className="text-[10px] uppercase tracking-widest text-slate-500 font-bold">
                <th className="px-4 py-3 font-normal">Symbol</th>
                <th className="px-4 py-3 font-normal">Side</th>
                <th className="px-4 py-3 font-normal">Entry</th>
                <th className="px-4 py-3 font-normal">Stop Loss</th>
                <th className="px-4 py-3 font-normal">Target</th>
              </tr>
            </thead>
            <tbody>
              {signals.map(renderSignalRow)}
            </tbody>
          </table>
        )}
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
        <h2 className="text-2xl font-bold text-slate-100 mb-2">Screening Dashboard</h2>
        <p className="text-slate-500 text-sm">
          Generate actionable trading signals for the next trading day using multiple strategies
        </p>
      </div>

      {/* Controls Section */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Date Picker */}
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-2">
              Screening Date
            </label>
            <input
              type="date"
              value={screeningDate}
              onChange={(e) => setScreeningDate(e.target.value)}
              max={new Date().toISOString().split('T')[0]}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2.5 text-slate-100 text-sm mono focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500/50 transition-all"
            />
          </div>

          {/* Strategy Multi-Select */}
          <div className="lg:col-span-2">
            <div className="flex items-center justify-between mb-2">
              <label className="block text-xs font-bold text-slate-500 uppercase">
                Select Strategies
              </label>
              <button
                onClick={toggleAllStrategies}
                className="text-xs text-emerald-400 hover:text-emerald-300 font-medium transition-colors"
              >
                {selectedStrategies.size === strategies.length ? 'Deselect All' : 'Select All'}
              </button>
            </div>
            <div className="flex flex-wrap gap-2 p-3 bg-slate-800 border border-slate-700 rounded-lg min-h-[50px]">
              {strategies.map((strategy) => (
                <button
                  key={strategy.code}
                  onClick={() => toggleStrategy(strategy.code)}
                  title={
                    strategy.supportsScreening
                      ? `${strategy.name} • Min Lookback: ${strategy.minLookbackDays} days`
                      : `${strategy.name} • Screening not supported`
                  }
                  disabled={!strategy.supportsScreening}
                  className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-all flex items-center gap-1.5 ${
                    selectedStrategies.has(strategy.code)
                      ? 'bg-emerald-500 text-slate-950 shadow-lg shadow-emerald-500/20'
                      : strategy.supportsScreening
                        ? 'bg-slate-700 text-slate-400 hover:bg-slate-600 hover:text-slate-200'
                        : 'bg-slate-800/50 text-slate-600 cursor-not-allowed opacity-50'
                  }`}
                >
                  <span>{strategy.code}</span>
                  {strategy.supportsScreening && (
                    <span className="text-[9px] opacity-80" title="Supports Screening">
                      🔍
                    </span>
                  )}
                  {!strategy.supportsScreening && (
                    <span className="text-[9px] opacity-60" title="Screening Not Supported">
                      ✕
                    </span>
                  )}
                </button>
              ))}
              {strategies.length === 0 && (
                <span className="text-slate-500 text-xs italic py-1.5">
                  No strategies available
                </span>
              )}
            </div>
            <p className="text-xs text-slate-500 mt-1.5">
              {selectedStrategies.size} strategy{selectedStrategies.size !== 1 ? 'ies' : ''} selected
            </p>
          </div>
        </div>

        {/* Run Button */}
        <div className="mt-6">
          <button
            onClick={handleRunScreening}
            disabled={loading || selectedStrategies.size === 0}
            className={`w-full sm:w-auto px-8 py-3 rounded-xl font-bold text-base transition-all ${
              loading || selectedStrategies.size === 0
                ? 'bg-slate-800 text-slate-500 cursor-not-allowed border border-slate-700'
                : 'bg-emerald-500 hover:bg-emerald-400 text-slate-950 shadow-xl shadow-emerald-500/10 active:scale-[0.98]'
            }`}
          >
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <span className="animate-spin">⟳</span>
                Running Screening...
              </span>
            ) : (
              'Run Screening'
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
      {results && !loading && (
        <div className="space-y-6">
          {/* Summary Header */}
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-lg font-bold text-slate-100">
                Screening Results
              </h3>
              <p className="text-sm text-slate-500">
                Date: {new Date(screeningDate).toLocaleDateString('en-US', {
                  weekday: 'long',
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric'
                })}
              </p>
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-emerald-400">
                {results.totalSignals}
              </div>
              <div className="text-xs text-slate-500 uppercase font-semibold">
                Total Signals
              </div>
            </div>
          </div>

          {/* Empty State */}
          {results.totalSignals === 0 ? (
            <div className="bg-slate-900 border-2 border-dashed border-slate-800 rounded-2xl p-12 text-center">
              <div className="text-4xl mb-4">📭</div>
              <h4 className="text-lg font-semibold text-slate-300 mb-2">
                No Signals Found
              </h4>
              <p className="text-slate-500 text-sm max-w-md mx-auto">
                No actionable signals were generated for the selected strategies on this date.
                Try a different date or strategy combination.
              </p>
            </div>
          ) : (
            /* Strategy Results Grouped */
            <div className="space-y-6">
              {Object.entries(results.signalsByStrategy).map(([strategyCode, signals]) =>
                renderStrategySection(strategyCode, signals)
              )}
            </div>
          )}
        </div>
      )}

      {/* Empty State - No Results Yet */}
      {!results && !loading && !error && (
        <div className="bg-slate-900/50 border border-slate-800 rounded-2xl p-12 text-center">
          <div className="text-5xl mb-6">🔍</div>
          <h3 className="text-xl font-bold text-slate-200 mb-2">
            Ready to Screen
          </h3>
          <p className="text-slate-500 max-w-md mx-auto mb-6">
            Select strategies and a date to generate actionable trading signals for the next trading day.
          </p>
          <div className="flex items-center justify-center gap-6 text-xs text-slate-600 uppercase font-semibold">
            <span className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              Next-Day Intent
            </span>
            <span className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-blue-500"></span>
              Multi-Strategy
            </span>
            <span className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-amber-500"></span>
              Actionable Signals
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default ScreeningDashboard;
