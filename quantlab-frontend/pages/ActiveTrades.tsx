import React, { useState, useEffect, useMemo } from 'react';
import { apiService } from '../services/apiService';
import { PaperTrade } from '../types';

interface ActiveTradesProps {
  runId: number | null;
}

const ActiveTrades: React.FC<ActiveTradesProps> = ({ runId }) => {
  const [trades, setTrades] = useState<PaperTrade[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [filter, setFilter] = useState<'all' | 'profit' | 'loss'>('all');

  useEffect(() => {
    if (!runId) return;

    const fetchTrades = async () => {
      setLoading(true);
      try {
        // We'll need to update the API service to include active trades endpoint
        const data = await apiService.getActiveTrades(runId);
        setTrades(data);
      } catch (err) {
        console.error('Error fetching active trades:', err);
      } finally {
        setLoading(false);
      }
    };

    // Set up interval for real-time updates
    const interval = setInterval(fetchTrades, 30000); // Update every 30 seconds

    fetchTrades(); // Initial fetch

    return () => clearInterval(interval);
  }, [runId]);

  const filteredTrades = useMemo(() => {
    let filtered = trades;

    // Filter by search term
    if (searchTerm) {
      filtered = filtered.filter(t => t.symbol.toLowerCase().includes(searchTerm.toLowerCase()));
    }

    // Filter by profit/loss
    if (filter === 'profit') {
      filtered = filtered.filter(t => t.currentPrice && t.currentPrice > t.entryPrice);
    } else if (filter === 'loss') {
      filtered = filtered.filter(t => t.currentPrice && t.currentPrice < t.entryPrice);
    }

    return filtered;
  }, [trades, searchTerm, filter]);

  const totalUnrealizedPnl = useMemo(() => {
    return trades.reduce((sum, trade) => sum + (trade.unrealizedPnl || 0), 0);
  }, [trades]);

  const profitPositions = trades.filter(t => t.currentPrice && t.currentPrice > t.entryPrice).length;
  const lossPositions = trades.filter(t => t.currentPrice && t.currentPrice < t.entryPrice).length;

  if (!runId) return <div className="text-center p-20 text-slate-500">Run a strategy first to see active trades.</div>;
  if (loading) return <div className="p-8 text-center text-slate-500">Loading active trades...</div>;

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold">Active Trades</h2>
          <p className="text-sm text-slate-500">Live positions with real-time P&L updates</p>
        </div>
        <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
          <div className="relative w-full sm:w-64">
            <input
              type="text"
              placeholder="Search symbol..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full bg-slate-900 border border-slate-800 rounded-lg px-4 py-2 pl-10 text-sm focus:outline-none focus:ring-1 focus:ring-emerald-500"
            />
            <span className="absolute left-3 top-2.5 text-slate-600">🔍</span>
          </div>
          <select
            value={filter}
            onChange={(e) => setFilter(e.target.value as 'all' | 'profit' | 'loss')}
            className="bg-slate-900 border border-slate-800 rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-1 focus:ring-emerald-500"
          >
            <option value="all">All Positions</option>
            <option value="profit">Profit Positions</option>
            <option value="loss">Loss Positions</option>
          </select>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
          <div className="text-sm text-slate-400">Total Positions</div>
          <div className="text-2xl font-bold text-slate-200">{trades.length}</div>
        </div>
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
          <div className="text-sm text-slate-400">Profit Positions</div>
          <div className="text-2xl font-bold text-emerald-400">{profitPositions}</div>
        </div>
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-4">
          <div className="text-sm text-slate-400">Loss Positions</div>
          <div className="text-2xl font-bold text-rose-400">{lossPositions}</div>
        </div>
        <div className={`bg-slate-900 border border-slate-800 rounded-xl p-4 ${totalUnrealizedPnl >= 0 ? 'border-emerald-500/20' : 'border-rose-500/20'}`}>
          <div className="text-sm text-slate-400">Unrealized P&L</div>
          <div className={`text-2xl font-bold ${totalUnrealizedPnl >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
            {totalUnrealizedPnl >= 0 ? '+' : ''}{totalUnrealizedPnl.toLocaleString()}
          </div>
        </div>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-800/50 text-[10px] uppercase tracking-widest text-slate-500 font-bold border-b border-slate-800">
                <th className="px-6 py-4">Symbol</th>
                <th className="px-6 py-4">Entry</th>
                <th className="px-6 py-4 text-right">Qty</th>
                <th className="px-6 py-4 text-right">Entry Price</th>
                <th className="px-6 py-4 text-right">CMP</th>
                <th className="px-6 py-4 text-right">SL</th>
                <th className="px-6 py-4 text-right">Target</th>
                <th className="px-6 py-4 text-right">Unrealized P&L</th>
                <th className="px-6 py-4 text-right">P&L %</th>
                <th className="px-6 py-4 text-right">R-Mult</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {filteredTrades.map((trade, idx) => {
                const isProfit = trade.currentPrice && trade.currentPrice > trade.entryPrice;
                const pnlPct = trade.unrealizedPnlPct || 0;

                return (
                  <tr key={idx} className="hover:bg-slate-800/30 transition-colors text-sm">
                    <td className="px-6 py-4 font-bold text-slate-200 mono">{trade.symbol}</td>
                    <td className="px-6 py-4 text-slate-400 mono text-xs">{trade.entryDate}</td>
                    <td className="px-6 py-4 text-right text-slate-300 mono">{trade.quantity}</td>
                    <td className="px-6 py-4 text-right text-slate-400 mono">${trade.entryPrice.toFixed(2)}</td>
                    <td className="px-6 py-4 text-right font-bold mono">
                      ${trade.currentPrice?.toFixed(2) || 'N/A'}
                    </td>
                    <td className="px-6 py-4 text-right text-slate-400 mono">
                      {trade.stopLoss ? `$${trade.stopLoss.toFixed(2)}` : 'N/A'}
                    </td>
                    <td className="px-6 py-4 text-right text-slate-400 mono">
                      {trade.targetPrice ? `$${trade.targetPrice.toFixed(2)}` : 'N/A'}
                    </td>
                    <td className={`px-6 py-4 text-right font-bold mono ${isProfit ? 'text-emerald-400' : 'text-rose-400'}`}>
                      {(trade.unrealizedPnl || 0) >= 0 ? '+' : ''}{(trade.unrealizedPnl || 0).toLocaleString()}
                    </td>
                    <td className={`px-6 py-4 text-right font-bold mono ${pnlPct >= 0 ? 'text-emerald-500/80' : 'text-rose-500/80'}`}>
                      {pnlPct.toFixed(2)}%
                    </td>
                    <td className="px-6 py-4 text-right text-slate-400 mono">
                      {trade.rMultiple ? `${trade.rMultiple.toFixed(2)}x` : 'N/A'}
                    </td>
                  </tr>
                );
              })}
              {filteredTrades.length === 0 && (
                <tr>
                  <td colSpan={10} className="px-6 py-12 text-center text-slate-500 italic">
                    {filter === 'all' ? 'No active trades found.' :
                     filter === 'profit' ? 'No profit positions found.' :
                     'No loss positions found.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default ActiveTrades;