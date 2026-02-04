
import React, { useState, useEffect, useMemo } from 'react';
import { apiService } from '../services/apiService';
import { PaperTrade } from '../types';

interface PaperTradesProps {
  runId: number | null;
}

const PaperTrades: React.FC<PaperTradesProps> = ({ runId }) => {
  const [trades, setTrades] = useState<PaperTrade[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    if (!runId) return;
    const fetchTrades = async () => {
      setLoading(true);
      try {
        const data = await apiService.getPaperTrades(runId);
        setTrades(data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchTrades();
  }, [runId]);

  const filteredTrades = useMemo(() => {
    return trades.filter(t => t.symbol.toLowerCase().includes(searchTerm.toLowerCase()));
  }, [trades, searchTerm]);

  if (!runId) return <div className="text-center p-20 text-slate-500">Run a strategy first to see paper trades.</div>;
  if (loading) return <div className="p-8 text-center text-slate-500">Analyzing trade history...</div>;

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h2 className="text-2xl font-bold">Paper Trades</h2>
          <p className="text-sm text-slate-500">Comprehensive list of closed execution positions</p>
        </div>
        <div className="relative w-full md:w-64">
          <input 
            type="text" 
            placeholder="Search symbol..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-slate-900 border border-slate-800 rounded-lg px-4 py-2 pl-10 text-sm focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
          <span className="absolute left-3 top-2.5 text-slate-600">🔍</span>
        </div>
      </div>

      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-800/50 text-[10px] uppercase tracking-widest text-slate-500 font-bold border-b border-slate-800">
                <th className="px-6 py-4">Symbol</th>
                <th className="px-6 py-4">Entry</th>
                <th className="px-6 py-4">Exit</th>
                <th className="px-6 py-4 text-right">Qty</th>
                <th className="px-6 py-4 text-right">Entry Price</th>
                <th className="px-6 py-4 text-right">Exit Price</th>
                <th className="px-6 py-4 text-right">PnL</th>
                <th className="px-6 py-4 text-right">PnL %</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {filteredTrades.map((trade, idx) => (
                <tr key={idx} className="hover:bg-slate-800/30 transition-colors text-sm">
                  <td className="px-6 py-4 font-bold text-slate-200 mono">{trade.symbol}</td>
                  <td className="px-6 py-4 text-slate-400 mono text-xs">{trade.entryDate}</td>
                  <td className="px-6 py-4 text-slate-400 mono text-xs">{trade.exitDate}</td>
                  <td className="px-6 py-4 text-right text-slate-300 mono">{trade.quantity}</td>
                  <td className="px-6 py-4 text-right text-slate-400 mono">${trade.entryPrice.toFixed(2)}</td>
                  <td className="px-6 py-4 text-right text-slate-400 mono">${trade.exitPrice.toFixed(2)}</td>
                  <td className={`px-6 py-4 text-right font-bold mono ${trade.pnl >= 0 ? 'text-emerald-400' : 'text-rose-400'}`}>
                    {trade.pnl >= 0 ? '+' : ''}{trade.pnl.toLocaleString()}
                  </td>
                  <td className={`px-6 py-4 text-right font-bold mono ${trade.pnlPct >= 0 ? 'text-emerald-500/80' : 'text-rose-500/80'}`}>
                    {trade.pnlPct.toFixed(2)}%
                  </td>
                </tr>
              ))}
              {filteredTrades.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-6 py-12 text-center text-slate-500 italic">No matching closed trades found.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default PaperTrades;
