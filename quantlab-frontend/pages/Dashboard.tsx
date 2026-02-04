
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, AreaChart, Area 
} from 'recharts';
import { apiService } from '../services/apiService';
import { AnalyticsSummary, EquityCurvePoint } from '../types';
import StatCard from '../components/StatCard';

interface DashboardProps {
  runId: number | null;
}

const Dashboard: React.FC<DashboardProps> = ({ runId }) => {
  const [analytics, setAnalytics] = useState<AnalyticsSummary | null>(null);
  const [equityData, setEquityData] = useState<EquityCurvePoint[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!runId) return;

    const fetchData = async () => {
      setLoading(true);
      setError(null);
      try {
        const [stats, curve] = await Promise.all([
          apiService.getAnalytics(runId),
          apiService.getEquityCurve(runId)
        ]);
        setAnalytics(stats);
        setEquityData(curve);
      } catch (err) {
        setError('Failed to fetch dashboard data.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [runId]);

  if (!runId) {
    return (
      <div className="flex flex-col items-center justify-center h-[60vh] text-center border-2 border-dashed border-slate-800 rounded-3xl p-12">
        <div className="text-5xl mb-6">🔭</div>
        <h2 className="text-2xl font-bold text-slate-200 mb-2">No Active Strategy Run</h2>
        <p className="text-slate-500 max-w-md mb-8">
          You haven't executed a strategy simulation yet. Head over to the Strategy Configurator to get started.
        </p>
        <Link 
          to="/configure" 
          className="bg-emerald-500 hover:bg-emerald-600 text-slate-950 px-6 py-3 rounded-lg font-bold transition-colors"
        >
          Configure Strategy
        </Link>
      </div>
    );
  }

  if (loading) return <div className="p-8 text-center text-slate-500 animate-pulse">Loading strategy results...</div>;
  if (error) return <div className="p-8 text-rose-400 bg-rose-400/10 rounded-xl border border-rose-400/20">{error}</div>;

  return (
    <div className="space-y-8">
      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard 
          label="Total PnL" 
          value={`$${analytics?.totalPnl.toLocaleString()}`} 
          subValue="+12.4%" 
          trend="up" 
        />
        <StatCard 
          label="Win Rate" 
          value={`${(analytics?.winRate ?? 0 * 100).toFixed(1)}%`} 
        />
        <StatCard 
          label="Number of Trades" 
          value={analytics?.totalTrades ?? 0} 
        />
        <StatCard 
          label="Max Drawdown" 
          value={`${(analytics?.maxDrawdown ?? 0 * 100).toFixed(1)}%`} 
          trend="down" 
        />
      </div>

      {/* Chart Section */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h3 className="text-lg font-bold text-slate-100">Equity Curve</h3>
            <p className="text-xs text-slate-500">Cumulative performance over simulation period</p>
          </div>
          <div className="flex gap-2">
            <button className="text-[10px] px-2 py-1 bg-slate-800 rounded border border-slate-700 font-bold text-slate-400 uppercase">1D</button>
            <button className="text-[10px] px-2 py-1 bg-slate-800 rounded border border-slate-700 font-bold text-slate-400 uppercase">1W</button>
            <button className="text-[10px] px-2 py-1 bg-emerald-500/10 text-emerald-400 rounded border border-emerald-500/20 font-bold uppercase">MAX</button>
          </div>
        </div>
        
        <div className="h-[400px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={equityData}>
              <defs>
                <linearGradient id="colorEquity" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#10b981" stopOpacity={0.3}/>
                  <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#1e293b" />
              <XAxis 
                dataKey="date" 
                stroke="#64748b" 
                fontSize={12} 
                tickLine={false} 
                axisLine={false}
                tickFormatter={(val) => new Date(val).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
              />
              <YAxis 
                stroke="#64748b" 
                fontSize={12} 
                tickLine={false} 
                axisLine={false}
                tickFormatter={(val) => `$${val/1000}k`}
              />
              <Tooltip 
                contentStyle={{ backgroundColor: '#0f172a', border: '1px solid #1e293b', borderRadius: '8px' }}
                itemStyle={{ color: '#10b981' }}
                labelStyle={{ color: '#94a3b8', marginBottom: '4px' }}
              />
              <Area 
                type="monotone" 
                dataKey="equity" 
                stroke="#10b981" 
                strokeWidth={3}
                fillOpacity={1} 
                fill="url(#colorEquity)" 
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
