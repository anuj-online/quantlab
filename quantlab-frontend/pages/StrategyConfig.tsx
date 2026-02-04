
import React, { useState, useEffect } from 'react';
import { apiService } from '../services/apiService';
import { Strategy, MarketType, StrategyRunRequest } from '../types';

interface StrategyConfigProps {
  onRunCompleted: (id: number) => void;
}

const StrategyConfig: React.FC<StrategyConfigProps> = ({ onRunCompleted }) => {
  const [strategies, setStrategies] = useState<Strategy[]>([]);
  const [market, setMarket] = useState<MarketType>('US');
  const [selectedStrategy, setSelectedStrategy] = useState<string>('');
  const [startDate, setStartDate] = useState('2021-01-01');
  const [endDate, setEndDate] = useState('2024-12-31');
  const [params, setParams] = useState<Record<string, number>>({
    fastEma: 20,
    slowEma: 50,
    riskPerTrade: 0.01
  });
  
  const [loading, setLoading] = useState(false);
  const [running, setRunning] = useState(false);

  useEffect(() => {
    const fetchStrategies = async () => {
      setLoading(true);
      try {
        const data = await apiService.getStrategies();
        setStrategies(data);
        if (data.length > 0) setSelectedStrategy(data[0].code);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchStrategies();
  }, []);

  const handleParamChange = (key: string, value: string) => {
    setParams(prev => ({ ...prev, [key]: parseFloat(value) || 0 }));
  };

  const handleRun = async () => {
    setRunning(true);
    try {
      const payload: StrategyRunRequest = {
        strategyCode: selectedStrategy,
        market,
        startDate,
        endDate,
        params
      };
      const response = await apiService.runStrategy(payload);
      onRunCompleted(response.strategyRunId);
      // Feedback can be added here, but UX rules say no animations/dumb UI
    } catch (err) {
      alert('Failed to run strategy');
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden">
        <div className="p-8 border-b border-slate-800">
          <h2 className="text-xl font-bold mb-2">Backtest Configuration</h2>
          <p className="text-slate-500 text-sm">Define your trading parameters and market selection to run a high-fidelity simulation.</p>
        </div>

        <div className="p-8 space-y-8">
          {/* Market & Strategy Row */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Target Market</label>
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

            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Algorithm</label>
              <select 
                value={selectedStrategy}
                onChange={(e) => setSelectedStrategy(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-slate-100 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
              >
                {strategies.map(s => (
                  <option key={s.id} value={s.code}>{s.name}</option>
                ))}
              </select>
            </div>
          </div>

          {/* Time Frame */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-2">Start Date</label>
              <input 
                type="date" 
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-slate-100 text-sm mono focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-500 uppercase mb-2">End Date</label>
              <input 
                type="date" 
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="w-full bg-slate-800 border border-slate-700 rounded-lg px-4 py-2 text-slate-100 text-sm mono focus:outline-none focus:ring-2 focus:ring-emerald-500/50"
              />
            </div>
          </div>

          {/* Strategy Specific Params */}
          <div className="pt-4 border-t border-slate-800">
            <h3 className="text-sm font-bold text-slate-300 mb-4 uppercase tracking-widest">Strategy Parameters</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {Object.entries(params).map(([key, value]) => (
                <div key={key}>
                  <label className="block text-[10px] font-bold text-slate-500 uppercase mb-1">{key.replace(/([A-Z])/g, ' $1').trim()}</label>
                  <input 
                    type="number"
                    step={key.includes('risk') ? '0.001' : '1'}
                    value={value}
                    onChange={(e) => handleParamChange(key, e.target.value)}
                    className="w-full bg-slate-950 border border-slate-800 rounded px-3 py-2 text-slate-200 text-sm mono focus:border-emerald-500 outline-none"
                  />
                </div>
              ))}
            </div>
          </div>

          <div className="pt-8">
            <button 
              onClick={handleRun}
              disabled={running || !selectedStrategy}
              className={`w-full py-4 rounded-xl font-bold text-lg shadow-xl shadow-emerald-500/10 transition-all ${
                running 
                  ? 'bg-slate-800 text-slate-500 cursor-not-allowed' 
                  : 'bg-emerald-500 hover:bg-emerald-400 text-slate-950 active:scale-[0.98]'
              }`}
            >
              {running ? 'EXECUTING SIMULATION...' : 'RUN STRATEGY SIMULATION'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default StrategyConfig;
