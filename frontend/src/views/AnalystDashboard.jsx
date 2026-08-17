import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import { ShieldAlert, Play, AlertOctagon, CheckCircle2, RefreshCw, Filter, Search, AlertTriangle, ShieldCheck, XCircle } from 'lucide-react';

function isValidUUID(str) {
  if (!str || typeof str !== 'string') return false;
  const looseUuidRegex = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;
  return looseUuidRegex.test(str.trim());
}

function formatRuleName(ruleType) {
  switch (ruleType) {
    case 'LARGE_TRANSACTION': return 'Large Transaction Rule';
    case 'HIGH_VELOCITY': return 'High Velocity Rule';
    case 'UNUSUAL_AMOUNT': return 'Unusual Amount Rule';
    case 'ACCOUNT_ACTIVITY_ANOMALY': return 'Account Activity Anomaly Rule';
    case 'DESTINATION_RISK': return 'Destination Risk Rule';
    default: return ruleType ? String(ruleType).replace(/_/g, ' ') : 'Fraud Rule';
  }
}

export function AnalystDashboard() {
  const [transactions, setTransactions] = useState([]);
  const [loadingTx, setLoadingTx] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [inspecting, setInspecting] = useState(false);
  const [inspectedStatus, setInspectedStatus] = useState(null);
  const [userWalletBalance, setUserWalletBalance] = useState(null);
  const [backendOffline, setBackendOffline] = useState(false);
  const [statusMessage, setStatusMessage] = useState(null);

  // Fraud Rule Context parameters
  const [userId, setUserId] = useState('');
  const [txId, setTxId] = useState('');
  const [amount, setAmount] = useState('');
  const [txLast5Min, setTxLast5Min] = useState(0);
  const [txLast1Hr, setTxLast1Hr] = useState(0);
  const [avgAmount, setAvgAmount] = useState('');
  const [failedAttempts, setFailedAttempts] = useState(0);
  const [newDevice, setNewDevice] = useState(false);
  const [locationChanged, setLocationChanged] = useState(false);
  const [destinationHighRisk, setDestinationHighRisk] = useState(false);

  const [evaluating, setEvaluating] = useState(false);
  const [evaluationResult, setEvaluationResult] = useState(null);
  const [evalError, setEvalError] = useState(null);

  const fetchTransactions = async () => {
    setLoadingTx(true);
    setBackendOffline(false);
    try {
      const res = await api.transactions.getTransactions({ status: statusFilter, size: 100 });
      const txData = res.data?.content || res.data || res || [];
      const txList = Array.isArray(txData) ? txData : [];
      setTransactions(txList);

      // Auto inspect first transaction if available
      if (txList.length > 0 && !txId) {
        inspectTransaction(txList[0], txList);
      }
    } catch (e) {
      console.error('Failed to fetch transactions for analyst:', e);
      setBackendOffline(true);
    } finally {
      setLoadingTx(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [statusFilter]);

  // Inspect transaction by fetching details from GET /api/v1/transactions/{id}
  const inspectTransaction = async (txSummary, currentTxList = transactions) => {
    if (!txSummary || !txSummary.id) return;
    setInspecting(true);
    setEvalError(null);
    setEvaluationResult(null);
    setStatusMessage(null);

    try {
      let fullTx = txSummary;
      try {
        const fullTxRes = await api.transactions.getTransaction(txSummary.id);
        fullTx = fullTxRes.data || fullTxRes || txSummary;
      } catch (err) {
        // Use summary if endpoint fails
      }

      const rawUserId = (fullTx.userId && isValidUUID(fullTx.userId)) 
        ? fullTx.userId 
        : (txSummary.userId && isValidUUID(txSummary.userId) ? txSummary.userId : 'c7a3b4e1-2f5a-4b6c-8d9e-1f2a3b4c5d6e');

      const rawTxId = (fullTx.id && isValidUUID(fullTx.id))
        ? fullTx.id
        : (isValidUUID(txSummary.id) ? txSummary.id : 'a619e679-1e60-4d92-8bab-1ac284eb3d00');

      const targetAmount = fullTx.amount !== undefined ? fullTx.amount : (txSummary.amount || 0);
      const targetTime = fullTx.transactionTime ? new Date(fullTx.transactionTime).getTime() : Date.now();

      setTxId(rawTxId);
      setUserId(rawUserId);
      setAmount(targetAmount.toString());
      setInspectedStatus(fullTx.status || txSummary.status || null);
      setUserWalletBalance(fullTx.userWalletBalance !== undefined && fullTx.userWalletBalance !== null ? parseFloat(fullTx.userWalletBalance) : null);

      // Calculate velocity & historical parameters strictly prior to or at the inspected transaction time
      const txSourceList = currentTxList.length > 0 ? currentTxList : transactions;
      const userTxList = txSourceList.filter(t => (t.userId && t.userId === rawUserId) || t.id === rawTxId);
      
      const fiveMinAgo = targetTime - (5 * 60 * 1000);
      const oneHourAgo = targetTime - (60 * 60 * 1000);
      const twentyFourHoursAgo = targetTime - (24 * 60 * 60 * 1000);

      // Velocity in 5m (at moment of transaction): between (T - 5m) and T
      const count5m = userTxList.filter(t => {
        const tTime = t.transactionTime ? new Date(t.transactionTime).getTime() : 0;
        return tTime >= fiveMinAgo && tTime <= targetTime;
      }).length;

      // Velocity in 1h (at moment of transaction): between (T - 1h) and T
      const count1h = userTxList.filter(t => {
        const tTime = t.transactionTime ? new Date(t.transactionTime).getTime() : 0;
        return tTime >= oneHourAgo && tTime <= targetTime;
      }).length;

      // Historical average of completed transactions prior to this transaction
      const priorCompleted = userTxList.filter(t => {
        const tTime = t.transactionTime ? new Date(t.transactionTime).getTime() : 0;
        return (t.status === 'COMPLETED' || t.status === 'APPROVED') && tTime < targetTime;
      });

      const avg = priorCompleted.length > 0
        ? (priorCompleted.reduce((acc, t) => acc + (parseFloat(t.amount) || 0), 0) / priorCompleted.length).toFixed(2)
        : targetAmount.toString();

      // Failed attempts in last 24h prior to this transaction
      const failedCount = userTxList.filter(t => {
        const tTime = t.transactionTime ? new Date(t.transactionTime).getTime() : 0;
        return (t.status === 'BLOCKED' || t.status === 'FAILED' || t.status === 'REJECTED') && tTime >= twentyFourHoursAgo && tTime <= targetTime;
      }).length;

      setTxLast5Min(Math.max(1, count5m));
      setTxLast1Hr(Math.max(1, count1h));
      setAvgAmount(avg);
      setFailedAttempts(failedCount);
    } catch (err) {
      console.error('Inspection error:', err);
    } finally {
      setInspecting(false);
    }
  };

  // Analyst Decision Action: Approve or Block Transaction
  const handleUpdateStatus = async (targetTxId, targetStatus) => {
    if (!targetTxId) return;
    setStatusMessage(null);
    try {
      await api.transactions.updateTransactionStatus(targetTxId, targetStatus);
      setStatusMessage({ 
        type: targetStatus === 'COMPLETED' ? 'success' : 'danger', 
        text: `Transaction ${targetStatus === 'COMPLETED' ? 'APPROVED & COMPLETED' : 'BLOCKED & REJECTED'} in database!` 
      });
      fetchTransactions(); // Refresh table stream
    } catch (err) {
      setStatusMessage({ type: 'danger', text: err.message || 'Failed to update transaction status' });
    }
  };

  const handleEvaluate = async (e) => {
    e.preventDefault();
    setEvalError(null);
    setEvaluationResult(null);

    let cleanUserId = userId.trim();
    if (!cleanUserId || !isValidUUID(cleanUserId)) {
      cleanUserId = 'c7a3b4e1-2f5a-4b6c-8d9e-1f2a3b4c5d6e';
      setUserId(cleanUserId);
    }

    let cleanTxId = txId.trim();
    let finalTxId = isValidUUID(cleanTxId) ? cleanTxId : null;

    setEvaluating(true);

    try {
      const payload = {
        userId: cleanUserId,
        transactionId: finalTxId,
        amount: parseFloat(amount) || 100.0,
        transactionsLast5Minutes: parseInt(txLast5Min) || 0,
        transactionsLast1Hour: parseInt(txLast1Hr) || 0,
        averageTransactionAmount: parseFloat(avgAmount) || parseFloat(amount) || 0,
        recentFailedAttempts: parseInt(failedAttempts) || 0,
        newDevice: Boolean(newDevice),
        locationChanged: Boolean(locationChanged),
        destinationHighRisk: Boolean(destinationHighRisk)
      };

      const res = await api.fraud.evaluateRules(payload);
      setEvaluationResult(res);
    } catch (err) {
      console.error('Fraud rule evaluation error:', err);
      setEvalError(err.message || 'Connection refused by Spring Boot backend. Please verify backend is running on http://127.0.0.1:8080.');
    } finally {
      setEvaluating(false);
    }
  };

  const getRiskScoreCategory = (score) => {
    if (score >= 75) return { label: 'CRITICAL RISK', color: '#ef4444', badge: 'badge-status-blocked' };
    if (score >= 50) return { label: 'HIGH RISK', color: '#f97316', badge: 'badge-status-flagged' };
    if (score >= 30) return { label: 'MEDIUM RISK', color: '#f59e0b', badge: 'badge-status-pending' };
    return { label: 'LOW RISK', color: '#10b981', badge: 'badge-status-completed' };
  };

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '0 24px 48px' }} className="animate-fade-in">
      
      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '10px' }}>
            <ShieldAlert size={26} color="var(--role-analyst)" /> Analyst Fraud & Risk Review Desk
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Inspect database transactions, evaluate Spring Boot rules, and Approve/Block transactions (`PUT /api/v1/transactions/{`id`}/status`).
          </p>
        </div>
        <button onClick={fetchTransactions} className="btn btn-secondary btn-sm">
          <RefreshCw size={14} className={loadingTx ? 'pulse-glow' : ''} /> Refresh Stream
        </button>
      </div>

      {statusMessage && (
        <div style={{ 
          padding: '14px 20px', 
          borderRadius: '10px', 
          fontSize: '0.85rem', 
          marginBottom: '24px',
          background: statusMessage.type === 'success' ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
          border: '1px solid var(--border-subtle)',
          color: statusMessage.type === 'success' ? '#6ee7b7' : '#fca5a5',
          fontWeight: 700
        }}>
          {statusMessage.text}
        </div>
      )}

      {backendOffline && (
        <div style={{ 
          background: 'rgba(239, 68, 68, 0.15)', 
          border: '1px solid rgba(239, 68, 68, 0.4)', 
          color: '#fca5a5', 
          padding: '16px 20px', 
          borderRadius: '12px', 
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          <AlertTriangle size={24} />
          <div>
            <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>Spring Boot Backend Server Offline (ECONNREFUSED)</div>
            <div style={{ fontSize: '0.85rem', color: '#fecaca' }}>
              Vite dev proxy cannot connect to <code>http://127.0.0.1:8080</code>.
            </div>
          </div>
        </div>
      )}

      {/* Live System Transactions Review Stream Table */}
      <div className="glass-card" style={{ padding: '24px', marginBottom: '32px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Transactions Review Queue (`GET /api/v1/transactions`)</h3>
          
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Filter size={14} color="var(--text-muted)" />
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="form-select" style={{ padding: '4px 10px', fontSize: '0.8rem' }}>
              <option value="">All Statuses</option>
              <option value="PENDING">PENDING (Awaiting Review)</option>
              <option value="BLOCKED">BLOCKED</option>
              <option value="COMPLETED">COMPLETED</option>
            </select>
          </div>
        </div>

        <div className="table-container">
          <table className="custom-table">
            <thead>
              <tr>
                <th>Tx Reference</th>
                <th>Type</th>
                <th>Amount</th>
                <th>Status</th>
                <th>Action & Analyst Decision</th>
              </tr>
            </thead>
            <tbody>
              {transactions.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>
                    {loadingTx ? 'Loading transactions from backend database...' : 'No transactions recorded in database.'}
                  </td>
                </tr>
              ) : (
                transactions.map((tx) => (
                  <tr key={tx.id} style={{ background: tx.id === txId ? 'rgba(139, 92, 246, 0.12)' : tx.status === 'PENDING' || tx.status === 'FLAGGED' ? 'rgba(245, 158, 11, 0.05)' : 'transparent' }}>
                    <td className="font-mono" style={{ fontWeight: 700 }}>{tx.transactionReference || tx.id}</td>
                    <td>{tx.transactionType}</td>
                    <td className="font-mono" style={{ fontWeight: 700 }}>₹ {parseFloat(tx.amount).toFixed(2)}</td>
                    <td>
                      <span className={`badge badge-status-${(tx.status || 'COMPLETED').toLowerCase()}`}>
                        {tx.status}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <button 
                          onClick={() => inspectTransaction(tx)} 
                          className="btn btn-secondary btn-sm"
                          style={{ fontSize: '0.75rem', padding: '4px 8px', borderColor: 'var(--role-analyst)' }}
                        >
                          <Search size={12} /> Inspect
                        </button>
                        
                        {/* Only PENDING transactions (mapped from PaymentStatus.REVIEW) can be acted on */}
                        {tx.status === 'PENDING' && (
                          <>
                            <button 
                              onClick={() => handleUpdateStatus(tx.id, 'COMPLETED')}
                              className="btn btn-primary btn-sm"
                              style={{ fontSize: '0.75rem', padding: '4px 8px', background: '#10b981' }}
                              title="Approve — deducts funds from user wallet"
                            >
                              <ShieldCheck size={12} /> Approve
                            </button>
                            <button 
                              onClick={() => handleUpdateStatus(tx.id, 'BLOCKED')}
                              className="btn btn-danger btn-sm"
                              style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                              title="Block — payment marked REJECTED, funds not deducted"
                            >
                              <XCircle size={12} /> Reject
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Fraud Rule Evaluator Sandbox & Risk Output */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '32px' }}>
        
        {/* Fraud Rule Context Input Form */}
        <div className="glass-card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Fraud Rule Context Sandbox</h3>
            <span className="badge badge-role-analyst">{inspecting ? 'Fetching Details...' : 'Rule Evaluator'}</span>
          </div>

          {evalError && (
            <div style={{ 
              background: 'rgba(239, 68, 68, 0.15)', 
              border: '1px solid rgba(239, 68, 68, 0.4)', 
              color: '#fca5a5', 
              padding: '12px 14px', 
              borderRadius: '8px', 
              fontSize: '0.8rem', 
              marginBottom: '16px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px'
            }}>
              <AlertTriangle size={18} />
              <div>{evalError}</div>
            </div>
          )}

          <form onSubmit={handleEvaluate}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div className="form-group">
                <label className="form-label">User ID (Valid UUID Required)</label>
                <input 
                  type="text" 
                  placeholder="e.g. c7a3b4e1-2f5a-4b6c-8d9e-1f2a3b4c5d6e" 
                  value={userId} 
                  onChange={(e) => setUserId(e.target.value)} 
                  className="form-input font-mono" 
                />
              </div>
              <div className="form-group">
                <label className="form-label">Transaction ID (UUID)</label>
                <input 
                  type="text" 
                  placeholder="Transaction UUID" 
                  value={txId} 
                  onChange={(e) => setTxId(e.target.value)} 
                  className="form-input font-mono" 
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div className="form-group">
                <label className="form-label">Amount (₹)</label>
                <input type="number" step="0.01" placeholder="0.00" value={amount} onChange={(e) => setAmount(e.target.value)} className="form-input font-mono" />
              </div>
              <div className="form-group">
                <label className="form-label">Calculated User Avg Amount (₹)</label>
                <input type="number" step="0.01" placeholder="0.00" value={avgAmount} onChange={(e) => setAvgAmount(e.target.value)} className="form-input font-mono" />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px' }}>
              <div className="form-group">
                <label className="form-label">Tx Velocity 5m</label>
                <input type="number" value={txLast5Min} onChange={(e) => setTxLast5Min(e.target.value)} className="form-input font-mono" />
              </div>
              <div className="form-group">
                <label className="form-label">Tx Velocity 1h</label>
                <input type="number" value={txLast1Hr} onChange={(e) => setTxLast1Hr(e.target.value)} className="form-input font-mono" />
              </div>
              <div className="form-group">
                <label className="form-label">Failed Attempts</label>
                <input type="number" value={failedAttempts} onChange={(e) => setFailedAttempts(e.target.value)} className="form-input font-mono" />
              </div>
            </div>

            <div style={{ marginTop: '12px', padding: '16px', background: 'rgba(0,0,0,0.2)', borderRadius: '10px', border: '1px solid var(--border-subtle)', marginBottom: '20px' }}>
              <div style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: '12px' }}>
                Anomaly Flags & Context Triggers
              </div>
              
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.8rem' }}>
                  <label className="toggle-switch">
                    <input type="checkbox" checked={newDevice} onChange={(e) => setNewDevice(e.target.checked)} />
                    <span className="slider"></span>
                  </label>
                  <span>New Device</span>
                </label>

                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.8rem' }}>
                  <label className="toggle-switch">
                    <input type="checkbox" checked={locationChanged} onChange={(e) => setLocationChanged(e.target.checked)} />
                    <span className="slider"></span>
                  </label>
                  <span>Location Anomaly</span>
                </label>

                <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.8rem' }}>
                  <label className="toggle-switch">
                    <input type="checkbox" checked={destinationHighRisk} onChange={(e) => setDestinationHighRisk(e.target.checked)} />
                    <span className="slider"></span>
                  </label>
                  <span>High-Risk Dest</span>
                </label>
              </div>
            </div>

            <button type="submit" disabled={evaluating} className="btn btn-primary" style={{ width: '100%', background: 'linear-gradient(135deg, #8b5cf6, #ec4899)' }}>
              <Play size={16} /> {evaluating ? 'Evaluating Spring Boot Engine...' : 'Run Fraud Rule Evaluation'}
            </button>
          </form>
        </div>

        {/* Real-time Risk Assessment Output */}
        <div className="glass-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Risk Assessment Result</h3>
              {evaluationResult && (
                <span className={`badge ${getRiskScoreCategory(evaluationResult.totalRiskPoints || 0).badge}`}>
                  {getRiskScoreCategory(evaluationResult.totalRiskPoints || 0).label}
                </span>
              )}
            </div>

            {!evaluationResult ? (
              <div style={{ textAlign: 'center', padding: '60px 20px', color: 'var(--text-muted)' }}>
                <ShieldAlert size={40} style={{ margin: '0 auto 12px', opacity: 0.4 }} />
                <p>Click "Inspect" on any transaction row or fill the sandbox form and click "Run Fraud Rule Evaluation".</p>
              </div>
            ) : (
              <div>
                <div style={{ 
                  textAlign: 'center', 
                  padding: '24px', 
                  background: 'rgba(0,0,0,0.3)', 
                  borderRadius: '16px', 
                  border: `2px solid ${getRiskScoreCategory(evaluationResult.totalRiskPoints || 0).color}`,
                  marginBottom: '24px'
                }}>
                  <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: '0.05em' }}>
                    Total Calculated Risk Points
                  </div>
                  <div style={{ fontSize: '3.5rem', fontWeight: 900, color: getRiskScoreCategory(evaluationResult.totalRiskPoints || 0).color, fontFamily: 'var(--font-mono)' }}>
                    {evaluationResult.totalRiskPoints || 0}
                  </div>
                  <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                    {evaluationResult.triggeredRules || 0} of {evaluationResult.results ? evaluationResult.results.length : 0} Fraud Rules Triggered
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '20px' }}>
                  <div style={{ background: 'rgba(255,255,255,0.03)', padding: '12px', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Decision Recommendation</div>
                    <div style={{ fontSize: '0.9rem', fontWeight: 700, color: (evaluationResult.totalRiskPoints || 0) >= 50 ? '#ef4444' : '#10b981' }}>
                      {(evaluationResult.totalRiskPoints || 0) >= 75 ? 'BLOCK & QUARANTINE' : (evaluationResult.totalRiskPoints || 0) >= 50 ? 'FLAG FOR MANUAL REVIEW' : 'APPROVE'}
                    </div>
                  </div>
                  <div style={{ background: 'rgba(255,255,255,0.03)', padding: '12px', borderRadius: '8px', border: '1px solid var(--border-subtle)' }}>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Target User UUID</div>
                    <div style={{ fontSize: '0.8rem', fontWeight: 700, wordBreak: 'break-all' }} className="font-mono">{userId}</div>
                  </div>
                </div>

                {/* User Wallet Balance & Solvency Verification */}
                <div style={{
                  background: userWalletBalance !== null && userWalletBalance < parseFloat(amount || 0)
                    ? 'rgba(239, 68, 68, 0.15)'
                    : 'rgba(16, 185, 129, 0.1)',
                  border: `1px solid ${userWalletBalance !== null && userWalletBalance < parseFloat(amount || 0) ? 'rgba(239, 68, 68, 0.4)' : 'rgba(16, 185, 129, 0.3)'}`,
                  borderRadius: '10px',
                  padding: '14px 16px',
                  marginBottom: '20px'
                }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                    <span style={{ fontSize: '0.75rem', fontWeight: 700, textTransform: 'uppercase', color: 'var(--text-muted)' }}>
                      User Wallet Solvency Check
                    </span>
                    <span style={{
                      fontSize: '0.8rem',
                      fontWeight: 800,
                      color: userWalletBalance !== null && userWalletBalance < parseFloat(amount || 0) ? '#ef4444' : '#10b981'
                    }}>
                      {userWalletBalance !== null
                        ? (userWalletBalance < parseFloat(amount || 0) ? '❌ INSUFFICIENT BALANCE' : '✅ SUFFICIENT FUNDS')
                        : 'BALANCE UNKNOWN'}
                    </span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.85rem' }}>
                    <span>User Current Balance: <strong className="font-mono">₹{userWalletBalance !== null ? userWalletBalance.toFixed(2) : '--'}</strong></span>
                    <span>Required Amount: <strong className="font-mono">₹{parseFloat(amount || 0).toFixed(2)}</strong></span>
                  </div>
                  {userWalletBalance !== null && userWalletBalance < parseFloat(amount || 0) && (
                    <div style={{ fontSize: '0.75rem', color: '#fca5a5', marginTop: '8px', lineHeight: 1.4 }}>
                      ⚠️ <strong>Overdraft Protection:</strong> User's wallet balance (₹{userWalletBalance.toFixed(2)}) is less than required ₹{parseFloat(amount || 0).toFixed(2)}. Approving this payment is blocked. Please Reject or request user top-up.
                    </div>
                  )}
                </div>

                {/* Direct Analyst Decision Override Action Buttons */}
                {txId && (
                  <div style={{ background: 'rgba(0,0,0,0.3)', padding: '14px', borderRadius: '10px', border: '1px solid var(--border-subtle)' }}>
                    {inspectedStatus && inspectedStatus !== 'PENDING' ? (
                      <div style={{ textAlign: 'center', padding: '8px 0' }}>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '8px' }}>Transaction is not eligible for analyst action</div>
                        <span className={`badge badge-status-${(inspectedStatus || '').toLowerCase()}`} style={{ fontSize: '0.85rem', padding: '6px 14px' }}>
                          {inspectedStatus === 'COMPLETED' ? '✅ Already APPROVED' : inspectedStatus === 'BLOCKED' ? '🚫 Already BLOCKED' : inspectedStatus}
                        </span>
                      </div>
                    ) : (
                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                        <button
                          onClick={() => handleUpdateStatus(txId, 'COMPLETED')}
                          disabled={userWalletBalance !== null && userWalletBalance < parseFloat(amount || 0)}
                          className="btn btn-primary"
                          style={{
                            background: userWalletBalance !== null && userWalletBalance < parseFloat(amount || 0) ? 'rgba(255,255,255,0.08)' : '#10b981',
                            cursor: userWalletBalance !== null && userWalletBalance < parseFloat(amount || 0) ? 'not-allowed' : 'pointer',
                            opacity: userWalletBalance !== null && userWalletBalance < parseFloat(amount || 0) ? 0.5 : 1
                          }}
                          title={userWalletBalance !== null && userWalletBalance < parseFloat(amount || 0) ? `Cannot approve: User wallet balance (₹${userWalletBalance?.toFixed(2)}) is insufficient for ₹${parseFloat(amount || 0).toFixed(2)}` : 'Approve — deducts funds from user wallet (only valid for PENDING transactions)'}
                        >
                          <ShieldCheck size={16} /> Approve &amp; Debit Wallet
                        </button>
                        <button
                          onClick={() => handleUpdateStatus(txId, 'BLOCKED')}
                          className="btn btn-danger"
                          title="Block — payment marked REJECTED, funds not deducted"
                        >
                          <XCircle size={16} /> Block / Reject Payment
                        </button>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>

      </div>

      {/* Evaluated Rules Breakdown Table */}
      {evaluationResult && evaluationResult.results && (
        <div className="glass-card" style={{ padding: '24px' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '16px' }}>
            Evaluated Fraud Rules Breakdown ({evaluationResult.results.length})
          </h3>

          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Status</th>
                  <th>Rule Name</th>
                  <th>Risk Points</th>
                  <th>Evaluation Reason</th>
                  <th>Observed Value</th>
                  <th>Threshold</th>
                </tr>
              </thead>
              <tbody>
                {evaluationResult.results.map((rule, idx) => (
                  <tr key={idx} style={{ background: rule.triggered ? 'rgba(239, 68, 68, 0.05)' : 'transparent' }}>
                    <td>
                      {rule.triggered ? (
                        <span className="badge badge-status-failed">
                          <AlertOctagon size={12} /> TRIGGERED
                        </span>
                      ) : (
                        <span className="badge badge-status-completed">
                          <CheckCircle2 size={12} /> PASSED
                        </span>
                      )}
                    </td>
                    <td style={{ fontWeight: 700 }}>
                      {formatRuleName(rule.ruleType)}
                    </td>
                    <td className="font-mono" style={{ fontWeight: 800, color: rule.triggered ? '#ef4444' : 'var(--text-muted)' }}>
                      +{rule.riskPoints || 0}
                    </td>
                    <td style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                      {rule.reason || 'Rule evaluated successfully'}
                    </td>
                    <td className="font-mono" style={{ fontSize: '0.8rem' }}>
                      {rule.observedValue !== undefined && rule.observedValue !== null ? parseFloat(rule.observedValue).toFixed(2) : '-'}
                    </td>
                    <td className="font-mono" style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      {rule.threshold !== undefined && rule.threshold !== null ? parseFloat(rule.threshold).toFixed(2) : '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

    </div>
  );
}
