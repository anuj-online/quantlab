import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import { CapitalAllocationSnapshot, AllocationRequest, MarketType } from '../types';

const CapitalAllocation: React.FC = () => {
  const [allocation, setAllocation] = useState<CapitalAllocationSnapshot | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [initializing, setInitializing] = useState(true);

  const [params, setParams] = useState<AllocationRequest>({
    date: new Date().toISOString().split('T')[0],
    totalCapital: 1000000,
    riskPerTradePct: 1.0,
    maxOpenTrades: 5
  });

  useEffect(() => {
    // Load default date from latest screening
    const fetchLatestDate = async () => {
      try {
        const response = await fetch(`/api/v1/screening/latest-date`);
        if (response.ok) {
          const latestDate = await response.text();
          if (latestDate) {
            setParams(prev => ({ ...prev, date: latestDate }));
          }
        }
      } catch (err) {
        console.error('Failed to fetch latest date:', err);
      } finally {
        setInitializing(false);
      }
    };
    fetchLatestDate();
  }, []);

  const handleSimulate = async () => {
    setLoading(true);
    setError(null);

    try {
      const result = await apiService.simulateCapitalAllocation(params);
      setAllocation(result);
    } catch (err) {
      setError('Failed to simulate allocation. Please try again.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number) => {
    return `₹${value.toLocaleString()}`;
  };

  const MetricCard: React.FC<{ label: string; value: string; color?: string }> = ({
    label,
    value,
    color = 'text-slate-300'
  }) => (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
      <div className="text-xs text-slate-500 uppercase font-semibold mb-1">{label}</div>
      <div className={`text-lg font-bold ${color}`}>{value}</div>
    </div>
  );

  if (initializing) {
    return (
      <div className="flex items-center justify-center h-[60vh]">
        <div className="text-slate-500 animate-pulse">Loading...</div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header Section */}
      <div>
        <h2 className="text-2xl font-bold text-slate-100 mb-2">Capital Allocation Simulator</h2>
        <p className="text-slate-500 text-sm">
          Simulate how to deploy capital across ranked trading signals using risk-based position sizing
        </p>
      </div>

      {/* Input Form */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
        <h3 className="text-lg font-bold text-slate-100 mb-4">Capital Allocation Parameters</h3>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-4 mb-6">
          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-2">
              Date
            </label>
            <input
              type="date"
              value={params.date}
              onChange={e => setParams({...params, date: e.target.value})}
              max={new Date().toISOString().split('T')[0]}
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2.5 text-slate-100 text-sm mono focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500/50 transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-2">
              Total Capital (₹)
            </label>
            <input
              type="number"
              value={params.totalCapital}
              onChange={e => setParams({...params, totalCapital: +e.target.value})}
              min="10000"
              step="100000"
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2.5 text-slate-100 text-sm mono focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500/50 transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-2">
              Risk Per Trade (%)
            </label>
            <input
              type="number"
              step="0.1"
              value={params.riskPerTradePct}
              onChange={e => setParams({...params, riskPerTradePct: +e.target.value})}
              min="0.1"
              max="10"
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2.5 text-slate-100 text-sm mono focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500/50 transition-all"
            />
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-500 uppercase mb-2">
              Max Open Trades
            </label>
            <input
              type="number"
              value={params.maxOpenTrades}
              onChange={e => setParams({...params, maxOpenTrades: +e.target.value})}
              min="1"
              max="20"
              className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2.5 text-slate-100 text-sm mono focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500/50 transition-all"
            />
          </div>
        </div>

        <button
          onClick={handleSimulate}
          disabled={loading}
          className="w-full py-3 bg-emerald-500 hover:bg-emerald-400 disabled:bg-emerald-500/20 text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <span className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
              Simulating...
            </>
          ) : (
            <>
              <span>🧮</span>
              <span>Simulate Capital Allocation</span>
            </>
          )}
        </button>

        {error && (
          <div className="mt-4 p-3 bg-rose-500/10 border border-rose-500/30 rounded-lg text-rose-400 text-sm">
            {error}
          </div>
        )}
      </div>

      {/* Results */}
      {allocation && (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 mb-8">
            <MetricCard
              label="Total Capital"
              value={formatCurrency(allocation.totalCapital)}
            />
            <MetricCard
              label="Deployed"
              value={formatCurrency(allocation.deployedCapital)}
            />
            <MetricCard
              label="Free Cash"
              value={formatCurrency(allocation.freeCash)}
            />
            <MetricCard
              label="Expected R"
              value={`+${allocation.expectedRMultiple.toFixed(1)}R`}
              color="text-emerald-400"
            />
          </div>

          {/* Positions Table */}
          <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
            <div className="px-6 py-4 bg-slate-800/50 border-b border-slate-800">
              <h3 className="text-lg font-bold text-slate-100">
                Recommended Positions
              </h3>
              <p className="text-sm text-slate-500">
                Ranked signals deployed using risk-based position sizing
              </p>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left">
                <thead>
                  <tr className="text-[10px] uppercase tracking-widest text-slate-500 font-bold border-b border-slate-800">
                    <th className="px-4 py-3 font-normal">Symbol</th>
                    <th className="px-4 py-3 font-normal">Quantity</th>
                    <th className="px-4 py-3 font-normal">Capital Used</th>
                    <th className="px-4 py-3 font-normal">Risk Amount</th>
                    <th className="px-4 py-3 font-normal">Expected R</th>
                    <th className="px-4 py-3 font-normal text-right">Allocation %</th>
                  </tr>
                </thead>
                <tbody>
                  {allocation.positions.map((pos, index) => (
                    <tr
                      key={pos.id || index}
                      className="hover:bg-slate-800/30 transition-colors text-sm border-b border-slate-800/50 last:border-0"
                    >
                      <td className="px-4 py-3 font-bold text-slate-200 mono">{pos.symbol}</td>
                      <td className="px-4 py-3 text-slate-300 mono">{pos.quantity}</td>
                      <td className="px-4 py-3 text-slate-300 mono">
                        {formatCurrency(pos.capitalUsed)}
                      </td>
                      <td className="px-4 py-3 text-rose-400/80 mono">
                        {formatCurrency(pos.riskAmount)}
                      </td>
                      <td className="px-4 py-3 text-emerald-400 mono">
                        {pos.expectedR.toFixed(1)}R
                      </td>
                      <td className="px-4 py-3 text-slate-300 mono text-right">
                        {pos.allocationPct.toFixed(1)}%
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {allocation.positions.length === 0 && (
              <div className="p-8 text-center text-slate-500 italic">
                No positions allocated. Try adjusting the parameters or select a different date.
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default CapitalAllocation;