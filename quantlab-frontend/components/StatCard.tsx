
import React from 'react';

interface StatCardProps {
  label: string;
  value: string | number;
  subValue?: string;
  trend?: 'up' | 'down' | 'neutral';
}

const StatCard: React.FC<StatCardProps> = ({ label, value, subValue, trend }) => {
  const trendColor = trend === 'up' ? 'text-emerald-400' : trend === 'down' ? 'text-rose-400' : 'text-slate-400';

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 hover:border-slate-700 transition-colors">
      <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2">{label}</p>
      <div className="flex items-baseline gap-2">
        <h3 className="text-2xl font-bold mono text-slate-50">{value}</h3>
        {subValue && (
          <span className={`text-sm font-medium ${trendColor}`}>
            {trend === 'up' && '↑'} {trend === 'down' && '↓'} {subValue}
          </span>
        )}
      </div>
    </div>
  );
};

export default StatCard;
