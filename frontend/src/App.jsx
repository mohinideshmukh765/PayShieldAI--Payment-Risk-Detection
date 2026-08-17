import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { AuthView } from './views/AuthView';
import { UserDashboard } from './views/UserDashboard';
import { AnalystDashboard } from './views/AnalystDashboard';

function AppContent() {
  const { user, token, primaryRole, loading } = useAuth();
  const [activeTab, setActiveTab] = useState('dashboard');

  // Automatically direct user to appropriate tab according to primaryRole from backend DB
  useEffect(() => {
    if (primaryRole === 'ANALYST') {
      setActiveTab('fraud-evaluator');
    } else {
      setActiveTab('dashboard');
    }
  }, [primaryRole]);

  if (loading) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ textAlign: 'center' }}>
          <div className="pulse-glow" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--accent-primary)' }}>
            Connecting to PayShieldAI Platform...
          </div>
        </div>
      </div>
    );
  }

  // If not logged in, show AuthView (Login / Register)
  if (!token || !user) {
    return <AuthView />;
  }

  // Render Dashboard dynamically based on authenticated role
  const renderDashboard = () => {
    if (primaryRole === 'ANALYST') {
      return <AnalystDashboard />;
    }

    // USER role
    return <UserDashboard />;
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />
      <main style={{ flex: 1 }}>
        {renderDashboard()}
      </main>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
