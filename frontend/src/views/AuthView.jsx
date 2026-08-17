import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { Shield, Lock, Mail, User, ArrowRight, UserPlus, LogIn } from 'lucide-react';

export function AuthView() {
  const [mode, setMode] = useState('login'); // 'login' or 'register'
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const { login, register } = useAuth();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);

    try {
      if (mode === 'login') {
        await login(email, password);
      } else {
        await register(name, email, password);
      }
    } catch (err) {
      setError(err.message || 'Authentication failed. Please check your credentials.');
    } finally {
      setSubmitting(false);
    }
  };

  const toggleMode = () => {
    setError(null);
    setMode(mode === 'login' ? 'register' : 'login');
  };

  return (
    <div style={{ 
      minHeight: '100vh', 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center', 
      padding: '24px' 
    }}>
      <div style={{ width: '100%', maxWidth: '440px' }} className="glass-card animate-fade-in">
        
        {/* Header */}
        <div style={{ padding: '36px 32px 24px', textAlign: 'center', borderBottom: '1px solid var(--border-subtle)' }}>
          <div style={{ 
            width: '56px', 
            height: '56px', 
            margin: '0 auto 16px', 
            background: 'linear-gradient(135deg, #6366f1, #06b6d4)', 
            borderRadius: '16px', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center',
            boxShadow: '0 0 25px rgba(99, 102, 241, 0.4)'
          }}>
            <Shield size={30} color="#fff" />
          </div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, marginBottom: '6px' }}>PayShield AI</h2>
          <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            {mode === 'login' ? 'Sign in to access your dashboard' : 'Create a new user account'}
          </p>
        </div>

        <div style={{ padding: '28px 32px 36px' }}>
          
          {error && (
            <div style={{ 
              background: 'rgba(239, 68, 68, 0.15)', 
              border: '1px solid rgba(239, 68, 68, 0.3)', 
              color: '#fca5a5', 
              padding: '10px 14px', 
              borderRadius: '8px', 
              fontSize: '0.8rem', 
              marginBottom: '20px' 
            }}>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            {mode === 'register' && (
              <div className="form-group">
                <label className="form-label">Full Name</label>
                <div style={{ position: 'relative' }}>
                  <User size={16} style={{ position: 'absolute', left: '12px', top: '12px', color: 'var(--text-muted)' }} />
                  <input 
                    type="text" 
                    required 
                    value={name} 
                    onChange={(e) => setName(e.target.value)} 
                    className="form-input" 
                    style={{ paddingLeft: '38px' }} 
                    placeholder="John Doe" 
                  />
                </div>
              </div>
            )}

            <div className="form-group">
              <label className="form-label">Email Address</label>
              <div style={{ position: 'relative' }}>
                <Mail size={16} style={{ position: 'absolute', left: '12px', top: '12px', color: 'var(--text-muted)' }} />
                <input 
                  type="email" 
                  required 
                  value={email} 
                  onChange={(e) => setEmail(e.target.value)} 
                  className="form-input" 
                  style={{ paddingLeft: '38px' }} 
                  placeholder="name@payshield.ai" 
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '24px' }}>
              <label className="form-label">Password</label>
              <div style={{ position: 'relative' }}>
                <Lock size={16} style={{ position: 'absolute', left: '12px', top: '12px', color: 'var(--text-muted)' }} />
                <input 
                  type="password" 
                  required 
                  minLength={6} 
                  value={password} 
                  onChange={(e) => setPassword(e.target.value)} 
                  className="form-input" 
                  style={{ paddingLeft: '38px' }} 
                  placeholder="••••••••" 
                />
              </div>
            </div>

            <button type="submit" disabled={submitting} className="btn btn-primary" style={{ width: '100%', padding: '12px', marginBottom: '20px' }}>
              {submitting ? 'Authenticating...' : (mode === 'login' ? 'Sign In' : 'Create Account')} <ArrowRight size={16} />
            </button>
          </form>

          {/* Hyperlink to switch between Login and Register */}
          <div style={{ textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            {mode === 'login' ? (
              <span>
                Don't have an account?{' '}
                <button 
                  type="button" 
                  onClick={toggleMode} 
                  style={{ background: 'none', border: 'none', color: 'var(--accent-cyan)', fontWeight: 700, cursor: 'pointer', textDecoration: 'underline' }}
                >
                  Register here
                </button>
              </span>
            ) : (
              <span>
                Already have an account?{' '}
                <button 
                  type="button" 
                  onClick={toggleMode} 
                  style={{ background: 'none', border: 'none', color: 'var(--accent-cyan)', fontWeight: 700, cursor: 'pointer', textDecoration: 'underline' }}
                >
                  Log in here
                </button>
              </span>
            )}
          </div>

        </div>
      </div>
    </div>
  );
}
