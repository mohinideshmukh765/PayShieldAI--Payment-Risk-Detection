import React from 'react';
import { useAuth } from '../context/AuthContext';
import { Shield, User, ShieldAlert, LogOut } from 'lucide-react';

export function Navbar({ activeTab, setActiveTab }) {
  const { user, primaryRole, logout } = useAuth();

  const getRoleBadgeClass = (role) => {
    switch (role) {
      case 'ANALYST': return 'badge-role-analyst';
      default: return 'badge-role-user';
    }
  };

  const getRoleIcon = (role) => {
    switch (role) {
      case 'ANALYST': return <ShieldAlert size={14} />;
      default: return <User size={14} />;
    }
  };

  return (
    <header className="glass-card" style={{ borderRadius: 0, borderTop: 0, borderLeft: 0, borderRight: 0, marginBottom: '24px' }}>
      <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '16px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        
        {/* Brand Logo */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div style={{ 
              background: 'linear-gradient(135deg, #6366f1, #06b6d4)', 
              padding: '10px', 
              borderRadius: '12px', 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center',
              boxShadow: '0 0 15px rgba(99, 102, 241, 0.4)'
            }}>
              <Shield size={22} color="#fff" />
            </div>
            <div>
              <h1 style={{ fontSize: '1.25rem', fontWeight: 800, letterSpacing: '-0.02em', background: 'linear-gradient(to right, #fff, #94a3b8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                PayShield<span style={{ color: '#06b6d4' }}>AI</span>
              </h1>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#10b981', display: 'inline-block' }} className="pulse-glow"></span>
                <span>Connected to Backend API</span>
              </div>
            </div>
          </div>
        </div>

        {/* Dedicated Navigation Tab for each Role */}
        <nav style={{ display: 'flex', gap: '8px', background: 'rgba(0, 0, 0, 0.2)', padding: '4px', borderRadius: '10px', border: '1px solid var(--border-subtle)' }}>
          {primaryRole === 'USER' && (
            <button 
              onClick={() => setActiveTab('dashboard')} 
              className={`btn ${activeTab === 'dashboard' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ fontSize: '0.8rem', padding: '8px 16px' }}
            >
              <User size={14} /> My Wallet & Payments
            </button>
          )}

          {primaryRole === 'ANALYST' && (
            <button 
              onClick={() => setActiveTab('fraud-evaluator')} 
              className={`btn ${activeTab === 'fraud-evaluator' ? 'btn-primary' : 'btn-secondary'}`}
              style={{ fontSize: '0.8rem', padding: '8px 16px' }}
            >
              <ShieldAlert size={14} /> Fraud & Risk Review Desk
            </button>
          )}
        </nav>

        {/* User Profile & Role Badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
          
          <span className={`badge ${getRoleBadgeClass(primaryRole)}`} style={{ padding: '6px 12px', fontSize: '0.75rem' }}>
            {getRoleIcon(primaryRole)} {primaryRole}
          </span>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.85rem', fontWeight: 700 }}>{user?.name || 'User'}</div>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{user?.email}</div>
          </div>

          <button 
            onClick={logout} 
            className="btn btn-secondary btn-sm" 
            title="Logout"
            style={{ padding: '8px' }}
          >
            <LogOut size={16} />
          </button>

        </div>

      </div>
    </header>
  );
}
