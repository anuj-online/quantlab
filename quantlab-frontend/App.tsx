
import React, { useState, useEffect } from 'react';
import { HashRouter as Router, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import StrategyConfig from './pages/StrategyConfig';
import Signals from './pages/Signals';
import PaperTrades from './pages/PaperTrades';

const App: React.FC = () => {
  const [activeRunId, setActiveRunId] = useState<number | null>(null);

  // Optionally load a default run if one exists in localstorage
  useEffect(() => {
    const saved = localStorage.getItem('last_strategy_run_id');
    if (saved) {
      setActiveRunId(parseInt(saved, 10));
    }
  }, []);

  const handleRunStrategy = (id: number) => {
    setActiveRunId(id);
    localStorage.setItem('last_strategy_run_id', id.toString());
  };

  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard runId={activeRunId} />} />
          <Route path="/configure" element={<StrategyConfig onRunCompleted={handleRunStrategy} />} />
          <Route path="/signals" element={<Signals runId={activeRunId} />} />
          <Route path="/trades" element={<PaperTrades runId={activeRunId} />} />
        </Routes>
      </Layout>
    </Router>
  );
};

export default App;
