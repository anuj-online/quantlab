
import React from 'react';
import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { path: '/', label: 'Dashboard', icon: '📊' },
  { path: '/configure', label: 'Strategy Config', icon: '⚙️' },
  { path: '/signals', label: 'Signals', icon: '📡' },
  { path: '/trades', label: 'Paper Trades', icon: '📝' },
  { path: '/screening', label: 'Screening', icon: '🔍' },
  { path: '/compare', label: 'Compare', icon: '📈' },
];

const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const location = useLocation();

  return (
    <div className="flex min-h-screen bg-slate-950 text-slate-50">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 border-r border-slate-800 flex flex-col fixed h-full">
        <div className="p-6 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 bg-emerald-500 rounded flex items-center justify-center font-bold text-slate-950">QL</div>
            <h1 className="text-xl font-bold tracking-tight">QuantLab</h1>
          </div>
          <p className="text-xs text-slate-500 mt-1 uppercase font-semibold">Research Hub v1</p>
        </div>

        <nav className="flex-1 p-4 flex flex-col gap-1">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path;
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                  isActive 
                    ? 'bg-slate-800 text-emerald-400 font-medium' 
                    : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`}
              >
                <span>{item.icon}</span>
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="p-4 border-t border-slate-800">
          <div className="p-3 bg-slate-950/50 rounded-lg border border-slate-800 text-[10px] text-slate-500 mono uppercase">
            Environment: Research
            <br />
            Status: Connected
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <main className="ml-64 flex-1 flex flex-col min-h-screen">
        <header className="h-16 border-b border-slate-800 bg-slate-900/50 backdrop-blur sticky top-0 z-10 flex items-center justify-between px-8">
          <div className="text-sm font-medium text-slate-400">
            {navItems.find(i => i.path === location.pathname)?.label || 'Strategy Details'}
          </div>
          <div className="flex items-center gap-4">
            <div className="text-xs bg-emerald-500/10 text-emerald-400 px-2 py-1 rounded border border-emerald-500/20 font-medium">
              LIVE DATA READY
            </div>
            <div className="w-8 h-8 rounded-full bg-slate-700 border border-slate-600"></div>
          </div>
        </header>
        
        <div className="p-8 max-w-7xl mx-auto w-full">
          {children}
        </div>
      </main>
    </div>
  );
};

export default Layout;
