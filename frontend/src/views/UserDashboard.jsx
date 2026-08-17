import React, { useState, useEffect } from 'react';
import { api, generateUUID } from '../api/client';
import { Send, ArrowDownLeft, ArrowUpRight, RefreshCw, CheckCircle2, AlertOctagon, Clock, AlertTriangle, PlusCircle } from 'lucide-react';

export function UserDashboard() {
  const [wallet, setWallet] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);

  // Payment form state
  const [amount, setAmount] = useState('');
  const [destinationAccount, setDestinationAccount] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState(generateUUID());
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);

  // Top-up form state
  const [topUpAmount, setTopUpAmount] = useState('');
  const [topUpLoading, setTopUpLoading] = useState(false);
  const [topUpResult, setTopUpResult] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    setFetchError(null);
    try {
      const [walletRes, txRes, paymentsRes] = await Promise.all([
        api.wallet.getWallet(),
        api.wallet.getTransactions(),
        api.payments.getPayments()
      ]);

      setWallet(walletRes);
      setTransactions(Array.isArray(txRes) ? txRes : []);
      setPayments(Array.isArray(paymentsRes) ? paymentsRes : []);
    } catch (e) {
      setFetchError(e.message || 'Unable to communicate with Spring Boot backend API');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!amount || parseFloat(amount) <= 0) return;
    setSubmitting(true);
    setResult(null);

    try {
      const res = await api.payments.createPayment({
        amount: parseFloat(amount),
        currency: 'INR',
        description: destinationAccount ? `Payment to ${destinationAccount}` : 'Merchant Payment'
      }, idempotencyKey);

      setResult(res);
      setAmount('');
      setDestinationAccount('');
      setIdempotencyKey(generateUUID());
      fetchData();
    } catch (err) {
      setResult({ error: err.message || 'Payment processing failed' });
    } finally {
      setSubmitting(false);
    }
  };

  const handleTopUp = async (e) => {
    e.preventDefault();
    const amt = parseFloat(topUpAmount);
    if (!amt || amt <= 0) return;
    setTopUpLoading(true);
    setTopUpResult(null);
    try {
      const res = await api.wallet.topUp(amt);
      setTopUpResult({ success: true, balance: res.balance });
      setTopUpAmount('');
      fetchData();
    } catch (err) {
      setTopUpResult({ success: false, error: err.message || 'Top-up failed' });
    } finally {
      setTopUpLoading(false);
    }
  };

  /**
   * Maps backend enum values to readable badges.
   *
   * PaymentStatus:   APPROVED | REVIEW | REJECTED
   * TransactionStatus: COMPLETED | PENDING | BLOCKED
   * WalletTransactionType: CREDIT | DEBIT | REFUND | REVERSAL
   */
  const getStatusBadge = (status) => {
    switch (status) {
      case 'APPROVED':
      case 'COMPLETED':
        return <span className="badge badge-status-completed"><CheckCircle2 size={12} /> APPROVED</span>;

      case 'REJECTED':
      case 'BLOCKED':
        return <span className="badge badge-status-blocked"><AlertOctagon size={12} /> REJECTED</span>;

      case 'REVIEW':
      case 'PENDING':
        return <span className="badge badge-status-pending"><Clock size={12} /> UNDER REVIEW</span>;

      default:
        return <span className="badge badge-status-pending">{status}</span>;
    }
  };

  // Calculate Held Funds & Available Balance
  const totalBalance = wallet && wallet.balance !== undefined ? parseFloat(wallet.balance) : 0;
  const heldInReview = payments.filter(p => p.status === 'REVIEW').reduce((acc, p) => acc + (parseFloat(p.amount) || 0), 0);
  const availableBalance = Math.max(0, totalBalance - heldInReview);

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', padding: '0 24px 48px' }} className="animate-fade-in">

      {/* Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>User Wallet &amp; Payments Hub</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem' }}>
            Execute payments with JWT Authorization &amp; Idempotency Key protection (<code>/api/v1/payments</code>).
          </p>
        </div>
        <button onClick={fetchData} className="btn btn-secondary btn-sm">
          <RefreshCw size={14} className={loading ? 'pulse-glow' : ''} /> Refresh API Data
        </button>
      </div>

      {fetchError && (
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
            <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>Backend Offline or JWT Authentication Error</div>
            <div style={{ fontSize: '0.85rem', color: '#fecaca' }}>{fetchError}</div>
          </div>
        </div>
      )}

      {/* Row 1: Wallet Card + Payment Form */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: '24px', marginBottom: '24px' }}>

        {/* Wallet Overview Card */}
        <div className="glass-card" style={{ padding: '28px', background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.12), rgba(18, 24, 40, 0.85))', borderColor: 'rgba(16, 185, 129, 0.3)', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--role-user)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                Database Wallet Account
              </span>
              <span className="badge badge-status-completed">{wallet ? wallet.status : (loading ? 'LOADING' : 'OFFLINE')}</span>
            </div>

            <div style={{ marginBottom: '20px' }}>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Available to Spend</div>
              <div style={{ fontSize: '2.5rem', fontWeight: 800, fontFamily: 'var(--font-mono)', color: availableBalance > 0 ? '#10b981' : '#f59e0b' }}>
                {wallet ? `₹ ${availableBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}` : (loading ? 'Fetching...' : '₹ 0.00')}
              </div>
            </div>

            <div style={{ background: 'rgba(0,0,0,0.25)', padding: '10px 14px', borderRadius: '8px', marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Total Ledger Balance</div>
                <div style={{ fontSize: '0.95rem', fontWeight: 700, fontFamily: 'var(--font-mono)' }}>
                  ₹ {totalBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                </div>
              </div>
              {heldInReview > 0 && (
                <div style={{ textAlign: 'right' }}>
                  <div style={{ fontSize: '0.7rem', color: '#f59e0b', fontWeight: 600 }}>Held in Review</div>
                  <div style={{ fontSize: '0.95rem', fontWeight: 700, fontFamily: 'var(--font-mono)', color: '#f59e0b' }}>
                    - ₹ {heldInReview.toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                  </div>
                </div>
              )}
            </div>
          </div>

          <div style={{ background: 'rgba(0,0,0,0.3)', padding: '12px 14px', borderRadius: '10px', border: '1px solid var(--border-subtle)' }}>
            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>Wallet Reference ID</div>
            <div style={{ fontSize: '0.85rem', fontFamily: 'var(--font-mono)', color: 'var(--text-primary)', wordBreak: 'break-all' }}>
              {wallet ? wallet.walletId : (loading ? 'Loading...' : 'None')}
            </div>
          </div>
        </div>

        {/* Payment Action Card */}
        <div className="glass-card" style={{ padding: '28px' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Send size={18} color="var(--accent-cyan)" /> Make Payment / Transfer Funds
            </h3>
            <span className="badge badge-role-user">JWT Secured</span>
          </div>

          {result && (
            <div style={{
              padding: '14px',
              borderRadius: '10px',
              fontSize: '0.85rem',
              marginBottom: '20px',
              background: result.error
                ? 'rgba(239, 68, 68, 0.15)'
                : result.status === 'APPROVED'
                  ? 'rgba(16, 185, 129, 0.15)'
                  : result.status === 'REJECTED'
                    ? 'rgba(239, 68, 68, 0.15)'
                    : 'rgba(245, 158, 11, 0.15)',  // REVIEW → amber
              border: '1px solid var(--border-subtle)'
            }}>
              {result.error ? (
                <div style={{ color: '#fca5a5' }}>{result.error}</div>
              ) : (
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '6px' }}>
                    <span style={{ fontWeight: 700 }}>Fraud Assessment Decision:</span>
                    {getStatusBadge(result.status)}
                  </div>
                  {result.status === 'REVIEW' && (
                    <div style={{ fontSize: '0.78rem', color: '#fcd34d', marginBottom: '4px' }}>
                      ⚠️ Payment flagged for analyst review. Funds are held — an analyst must approve or reject.
                    </div>
                  )}
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                    Payment ID: <span className="font-mono">{result.paymentId}</span> | Tx Ref: <span className="font-mono">{result.transactionId}</span>
                  </div>
                </div>
              )}
            </div>
          )}

          <form onSubmit={handleSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label">Amount (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  required
                  placeholder="0.00"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  className="form-input font-mono"
                />
              </div>
              <div className="form-group">
                <label className="form-label">Destination Account / Merchant ID <span style={{ color: 'var(--text-muted)', fontWeight: 400 }}>(optional)</span></label>
                <input
                  type="text"
                  placeholder="e.g. M182910 or C99182"
                  value={destinationAccount}
                  onChange={(e) => setDestinationAccount(e.target.value)}
                  className="form-input font-mono"
                />
              </div>
            </div>

            <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', marginBottom: '20px', background: 'rgba(0,0,0,0.2)', padding: '8px 12px', borderRadius: '8px' }}>
              <span style={{ fontWeight: 600, color: 'var(--accent-cyan)' }}>Idempotency Header Protection:</span>{' '}
              <span className="font-mono">{idempotencyKey.slice(0, 24)}...</span>
            </div>

            <button type="submit" disabled={submitting} className="btn btn-primary" style={{ width: '100%', padding: '12px' }}>
              {submitting ? 'Evaluating ML Fraud Models &amp; Processing...' : 'Submit Idempotent Payment'}
            </button>
          </form>
        </div>
      </div>

      {/* Row 2: Top-Up Wallet */}
      <div className="glass-card" style={{ padding: '24px', marginBottom: '24px', borderColor: 'rgba(99, 102, 241, 0.3)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
          <PlusCircle size={20} color="#6366f1" />
          <h3 style={{ fontSize: '1.05rem', fontWeight: 700 }}>Top Up Wallet</h3>
          <span className="badge" style={{ background: 'rgba(99,102,241,0.15)', color: '#a5b4fc', border: '1px solid rgba(99,102,241,0.3)' }}>
            POST /api/v1/wallet/topup
          </span>
        </div>

        {topUpResult && (
          <div style={{
            padding: '12px 14px',
            borderRadius: '8px',
            fontSize: '0.85rem',
            marginBottom: '16px',
            background: topUpResult.success ? 'rgba(16, 185, 129, 0.12)' : 'rgba(239, 68, 68, 0.12)',
            border: '1px solid var(--border-subtle)',
            color: topUpResult.success ? '#6ee7b7' : '#fca5a5',
            fontWeight: 600
          }}>
            {topUpResult.success
              ? `✅ Wallet credited! New balance: ₹ ${parseFloat(topUpResult.balance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
              : `❌ ${topUpResult.error}`}
          </div>
        )}

        <form onSubmit={handleTopUp} style={{ display: 'flex', alignItems: 'flex-end', gap: '16px' }}>
          <div className="form-group" style={{ flex: '0 0 240px', marginBottom: 0 }}>
            <label className="form-label">Amount to Add (₹)</label>
            <input
              type="number"
              step="0.01"
              min="1"
              required
              placeholder="e.g. 5000"
              value={topUpAmount}
              onChange={(e) => setTopUpAmount(e.target.value)}
              className="form-input font-mono"
            />
          </div>
          <button
            type="submit"
            disabled={topUpLoading}
            className="btn btn-primary"
            style={{ background: 'linear-gradient(135deg, #6366f1, #8b5cf6)', padding: '10px 24px' }}
          >
            <PlusCircle size={15} />
            {topUpLoading ? 'Crediting...' : 'Add Funds'}
          </button>
        </form>
      </div>

      {/* Row 3: Payments + Wallet Ledger Tables */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>

        {/* Payments History */}
        <div className="glass-card" style={{ padding: '24px' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '16px' }}>
            Payment Gateway Records (<code>/api/v1/payments</code>)
          </h3>

          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Created At</th>
                </tr>
              </thead>
              <tbody>
                {payments.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>
                      {loading ? 'Fetching records...' : 'No payments found in backend database.'}
                    </td>
                  </tr>
                ) : (
                  payments.map((p) => (
                    <tr key={p.paymentId}>
                      <td className="font-mono" style={{ fontSize: '0.75rem' }}>{p.paymentId}</td>
                      <td className="font-mono" style={{ fontWeight: 700 }}>
                        ₹ {parseFloat(p.amount).toFixed(2)}
                      </td>
                      <td>{getStatusBadge(p.status)}</td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                        {new Date(p.createdAt).toLocaleString()}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Wallet Ledger */}
        <div className="glass-card" style={{ padding: '24px' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '16px' }}>
            Wallet Ledger Transactions (<code>/api/v1/wallet/transactions</code>)
          </h3>

          <div className="table-container">
            <table className="custom-table">
              <thead>
                <tr>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>After Balance</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {transactions.length === 0 ? (
                  <tr>
                    <td colSpan={4} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '24px' }}>
                      {loading ? 'Fetching ledger...' : 'No wallet transactions recorded yet.'}
                    </td>
                  </tr>
                ) : (
                  transactions.map((tx) => (
                    <tr key={tx.id}>
                      <td>
                        <span style={{
                          color: tx.type === 'CREDIT' ? 'var(--status-completed)' : 'var(--status-failed)',
                          fontWeight: 700,
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px'
                        }}>
                          {tx.type === 'CREDIT' ? <ArrowDownLeft size={14} /> : <ArrowUpRight size={14} />}
                          {tx.type}
                        </span>
                      </td>
                      <td className="font-mono" style={{ fontWeight: 700 }}>
                        ₹ {parseFloat(tx.amount).toFixed(2)}
                      </td>
                      <td className="font-mono" style={{ fontWeight: 600 }}>
                        ₹ {parseFloat(tx.balanceAfter || 0).toFixed(2)}
                      </td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                        {new Date(tx.createdAt).toLocaleString()}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

      </div>

    </div>
  );
}
