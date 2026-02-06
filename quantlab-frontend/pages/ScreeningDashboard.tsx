
import React, { useState, useEffect, useMemo } from 'react';
import { apiService } from '../services/apiService';
import { Strategy, ScreeningSignal, ScreeningResult, EnsembleSignal, EnsembleResult } from '../types';

type SortField = 'rankScore' | 'confidenceScore' | 'rMultiple' | 'symbol';
type SortOrder = 'asc' | 'desc';

const ScreeningDashboard: React.FC = () => {
  const [strategies, setStrategies] = useState<Strategy[]>([]);
  const [selectedStrategies, setSelectedStrategies] = useState<Set<string>>(new Set());
  const [screeningDate, setScreeningDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );
  const [results, setResults] = useState<ScreeningResult | null>(null);
  const [ensembleResults, setEnsembleResults] = useState<EnsembleResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [initializing, setInitializing] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [ensembleLoading, setEnsembleLoading] = useState(false);

  // Sorting and filtering state
  const [sortField, setSortField] = useState<SortField>('rankScore');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [filterTop, setFilterTop] = useState<number | null>(null);
  const [filterMinRank, setFilterMinRank] = useState<number | null>(null);
  const [filterMinConfidence, setFilterMinConfidence] = useState<number | null>(null);

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
    setEnsembleResults(null);

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

  const handleRunEnsembleScreening = async () => {
    if (selectedStrategies.size < 2) {
      setError('Please select at least 2 strategies for ensemble voting');
      return;
    }

    setEnsembleLoading(true);
    setError(null);
    setEnsembleResults(null);

    try {
      const data = await apiService.runEnsembleScreening(
        Array.from(selectedStrategies),
        screeningDate
      );
      setEnsembleResults(data);
    } catch (err) {
      setError('Failed to run ensemble screening. Please try again.');
      console.error(err);
    } finally {
      setEnsembleLoading(false);
    }
  };

  // Filtered and sorted ensemble signals
  const filteredAndSortedSignals = useMemo(() => {
    if (!ensembleResults) return [];

    let signals = [...ensembleResults.signals];

    // Apply filters
    if (filterTop !== null && filterTop > 0) {
      signals = signals.slice(0, filterTop);
    }

    if (filterMinRank !== null) {
      signals = signals.filter(s => (s.rankScore ?? 0) >= filterMinRank!);
    }

    if (filterMinConfidence !== null) {
      signals = signals.filter(s => s.confidenceScore >= filterMinConfidence!);
    }

    // Apply sorting
    signals.sort((a, b) => {
      let aVal: number | string;
      let bVal: number | string;

      switch (sortField) {
        case 'rankScore':
          aVal = a.rankScore ?? 0;
          bVal = b.rankScore ?? 0;
          break;
        case 'confidenceScore':
          aVal = a.confidenceScore;
          bVal = b.confidenceScore;
          break;
        case 'rMultiple':
          aVal = a.rMultiple ?? 0;
          bVal = b.rMultiple ?? 0;
          break;
        case 'symbol':
          aVal = a.symbol;
          bVal = b.symbol;
          break;
        default:
          return 0;
      }

      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return sortOrder === 'asc'
          ? aVal.localeCompare(bVal)
          : bVal.localeCompare(aVal);
      }

      return sortOrder === 'asc'
        ? (aVal as number) - (bVal as number)
        : (bVal as number) - (aVal as number);
    });

    return signals;
  }, [ensembleResults, sortField, sortOrder, filterTop, filterMinRank, filterMinConfidence]);

  const clearFilters = () => {
    setFilterTop(null);
    setFilterMinRank(null);
    setFilterMinConfidence(null);
  };

  const hasActiveFilters = filterTop !== null || filterMinRank !== null || filterMinConfidence !== null;

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

  const renderEnsembleSignalRow = (signal: EnsembleSignal) => (
    <tr
      key={signal.ensembleId}
      className="hover:bg-slate-800/30 transition-colors text-sm border-b border-slate-800/50 last:border-0"
    >
      <td className="px-3 py-3 font-bold text-slate-200 mono">{signal.symbol}</td>
      <td className="px-3 py-3">
        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400">
          BUY
        </span>
      </td>
      <td className="px-3 py-3 text-slate-300 mono text-xs">{signal.entryPrice.toFixed(2)}</td>
      <td className="px-3 py-3 text-rose-400/80 mono text-xs">
        {signal.stopLoss ? signal.stopLoss.toFixed(2) : 'N/A'}
      </td>
      <td className="px-3 py-3 text-emerald-400/80 mono text-xs">
        {signal.targetPrice ? signal.targetPrice.toFixed(2) : 'N/A'}
      </td>
      <td className="px-3 py-3 text-center text-slate-300 mono text-xs">
        {signal.voteScore}/{signal.totalStrategies}
      </td>
      <td className="px-3 py-3 text-center">
        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
          signal.confidenceScore > 3.5 ? 'bg-emerald-500/10 text-emerald-400' : 'bg-yellow-500/10 text-yellow-400'
        }`}>
          {signal.confidenceScore.toFixed(1)}
        </span>
      </td>
      <td className="px-3 py-3 text-center">
        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
          (signal.rankScore ?? 0) > 0.7 ? 'bg-purple-500/10 text-purple-400' : 'bg-slate-700/30 text-slate-400'
        }`}>
          {(signal.rankScore ?? 0).toFixed(2)}
        </span>
      </td>
      <td className="px-3 py-3 text-center mono text-xs">
        <span className={signal.rMultiple && signal.rMultiple >= 2 ? 'text-emerald-400' : 'text-slate-400'}>
          {signal.rMultiple ? signal.rMultiple.toFixed(1) : 'N/A'}R
        </span>
      </td>
      <td className="px-3 py-3 text-xs text-slate-500 max-w-[120px] truncate" title={Object.keys(signal.strategyVotes).filter(k => signal.strategyVotes[k] === 'BUY').join(' + ')}>
        {Object.keys(signal.strategyVotes).filter(k => signal.strategyVotes[k] === 'BUY').slice(0, 2).join(' + ')}
        {Object.keys(signal.strategyVotes).filter(k => signal.strategyVotes[k] === 'BUY').length > 2 && ' + ...'}
      </td>
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
        <div className="flex gap-4 mt-4">
          <button
            onClick={handleRunScreening}
            disabled={loading}
            className="px-4 py-2 bg-emerald-500 hover:bg-emerald-400 disabled:bg-emerald-500/20 text-white font-medium rounded-lg transition-colors flex items-center gap-2"
          >
            {loading ? (
              <>
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                Running...
              </>
            ) : (
              <>
                <span>🎯</span>
                <span>Run Screening</span>
              </>
            )}
          </button>
          <button
            onClick={handleRunEnsembleScreening}
            disabled={ensembleLoading || selectedStrategies.size < 2}
            className="px-4 py-2 bg-purple-500 hover:bg-purple-400 disabled:bg-purple-500/20 text-white font-medium rounded-lg transition-colors flex items-center gap-2"
          >
            {ensembleLoading ? (
              <>
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                Running...
              </>
            ) : (
              <>
                <span>🎪</span>
                <span>Ensemble Voting</span>
              </>
            )}
          </button>
        </div>
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

      {/* Ensemble Signals Section */}
      {ensembleResults && (
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-lg font-bold text-slate-100">
                Ensemble Signals (Multi-Strategy Consensus)
              </h3>
              <p className="text-sm text-slate-500">
                High-confidence signals generated by combining {selectedStrategies.size} strategies
              </p>
            </div>
            <div className="text-right">
              <div className="text-2xl font-bold text-purple-400">
                {filteredAndSortedSignals.length}/{ensembleResults.signals.length}
              </div>
              <div className="text-xs text-slate-500 uppercase font-semibold">
                Showing/Total Signals
              </div>
            </div>
          </div>

          {/* Sort & Filter Controls */}
          <div className="mb-6 p-4 bg-slate-800/50 rounded-xl border border-slate-700/50">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
              {/* Sort By */}
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Sort By</label>
                <select
                  value={sortField}
                  onChange={(e) => setSortField(e.target.value as SortField)}
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                >
                  <option value="rankScore">Rank Score</option>
                  <option value="confidenceScore">Confidence</option>
                  <option value="rMultiple">R-Multiple</option>
                  <option value="symbol">Symbol</option>
                </select>
              </div>

              {/* Sort Order */}
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Order</label>
                <button
                  onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
                  className="w-full bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 flex items-center justify-center gap-2 transition-colors"
                >
                  {sortOrder === 'desc' ? '↓ Descending' : '↑ Ascending'}
                </button>
              </div>

              {/* Top N Filter */}
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Top Signals</label>
                <select
                  value={filterTop ?? 'all'}
                  onChange={(e) => setFilterTop(e.target.value === 'all' ? null : parseInt(e.target.value))}
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                >
                  <option value="all">All</option>
                  <option value="5">Top 5</option>
                  <option value="10">Top 10</option>
                  <option value="20">Top 20</option>
                </select>
              </div>

              {/* Min Rank Filter */}
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Min Rank Score</label>
                <select
                  value={filterMinRank ?? 'all'}
                  onChange={(e) => setFilterMinRank(e.target.value === 'all' ? null : parseFloat(e.target.value))}
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                >
                  <option value="all">All</option>
                  <option value="0.5">0.5+</option>
                  <option value="0.6">0.6+</option>
                  <option value="0.7">0.7+</option>
                  <option value="0.8">0.8+</option>
                </select>
              </div>

              {/* Min Confidence Filter */}
              <div>
                <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Min Confidence</label>
                <select
                  value={filterMinConfidence ?? 'all'}
                  onChange={(e) => setFilterMinConfidence(e.target.value === 'all' ? null : parseFloat(e.target.value))}
                  className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                >
                  <option value="all">All</option>
                  <option value="2.0">2.0+</option>
                  <option value="3.0">3.0+</option>
                  <option value="3.5">3.5+</option>
                  <option value="4.0">4.0+</option>
                </select>
              </div>
            </div>

            {/* Clear Filters */}
            {hasActiveFilters && (
              <div className="mt-3 pt-3 border-t border-slate-700/50 flex items-center justify-between">
                <span className="text-xs text-slate-500">
                  {filteredAndSortedSignals.length} of {ensembleResults.signals.length} signals shown
                </span>
                <button
                  onClick={clearFilters}
                  className="text-xs text-purple-400 hover:text-purple-300 font-medium transition-colors"
                >
                  Clear Filters
                </button>
              </div>
            )}
          </div>

          {/* Empty State */}
          {filteredAndSortedSignals.length === 0 ? (
            <div className="bg-slate-900 border-2 border-dashed border-slate-800 rounded-2xl p-12 text-center">
              <div className="text-4xl mb-4">📭</div>
              <h4 className="text-lg font-semibold text-slate-300 mb-2">
                {hasActiveFilters ? 'No Signals Match Filters' : 'No Ensemble Signals Found'}
              </h4>
              <p className="text-slate-500 text-sm max-w-md mx-auto">
                {hasActiveFilters
                  ? 'Try adjusting your filter criteria to see more results.'
                  : 'No consensus signals were generated. Try selecting more strategies or a different date.'
                }
              </p>
              {hasActiveFilters && (
                <button
                  onClick={clearFilters}
                  className="mt-4 px-4 py-2 bg-purple-500 hover:bg-purple-400 text-white rounded-lg text-sm font-medium transition-colors"
                >
                  Clear Filters
                </button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="text-[9px] uppercase tracking-widest text-slate-500 font-bold">
                    <th className="px-3 py-3 font-normal">Symbol</th>
                    <th className="px-3 py-3 font-normal">Entry</th>
                    <th className="px-3 py-3 font-normal">SL</th>
                    <th className="px-3 py-3 font-normal">Target</th>
                    <th className="px-3 py-3 font-normal text-center">Votes</th>
                    <th className="px-3 py-3 font-normal text-center">Conf</th>
                    <th className="px-3 py-3 font-normal text-center">Rank Score</th>
                    <th className="px-3 py-3 font-normal text-center">R-Multiple</th>
                    <th className="px-3 py-3 font-normal">Strategies</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredAndSortedSignals.map(renderEnsembleSignalRow)}
                </tbody>
              </table>
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
