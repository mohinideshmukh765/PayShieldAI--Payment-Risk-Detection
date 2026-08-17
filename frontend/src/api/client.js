/**
 * PayShieldAI Real REST API Client
 * Connects 100% directly to Spring Boot Backend (/api/v1)
 */

const API_BASE = '/api/v1';

// Helper to construct Authorization header with JWT token
function getAuthHeaders(extraHeaders = {}) {
  const token = localStorage.getItem('payshield_token');
  const headers = {
    'Content-Type': 'application/json',
    ...extraHeaders,
  };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return headers;
}

// Client-side UUID generator for Idempotency-Key header
export function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

// Process HTTP response and throw exact backend error message if not ok
async function handleResponse(response) {
  if (!response.ok) {
    let errorMsg = `Server Error (${response.status})`;
    try {
      const errorData = await response.json();
      errorMsg = errorData.message || errorData.error || JSON.stringify(errorData);
    } catch (e) {
      // Could not parse error JSON
    }
    throw new Error(errorMsg);
  }
  return response.json();
}

export const api = {
  // Authentication APIs
  auth: {
    async register(data) {
      const res = await fetch(`${API_BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
      return await handleResponse(res);
    },

    async login(data) {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
      });
      return await handleResponse(res);
    }
  },

  // Wallet APIs (USER role)
  wallet: {
    async getWallet() {
      const res = await fetch(`${API_BASE}/wallet`, {
        headers: getAuthHeaders()
      });
      return await handleResponse(res);
    },

    async getTransactions() {
      const res = await fetch(`${API_BASE}/wallet/transactions`, {
        headers: getAuthHeaders()
      });
      return await handleResponse(res);
    },

    /**
     * Top-up the authenticated user's wallet.
     * POST /api/v1/wallet/topup  { amount: Number }
     */
    async topUp(amount) {
      const res = await fetch(`${API_BASE}/wallet/topup`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({ amount })
      });
      return await handleResponse(res);
    }
  },

  // Payment APIs (USER role)
  payments: {
    async createPayment(data, idempotencyKey = generateUUID()) {
      const res = await fetch(`${API_BASE}/payments`, {
        method: 'POST',
        headers: getAuthHeaders({
          'Idempotency-Key': idempotencyKey
        }),
        body: JSON.stringify(data)
      });
      return await handleResponse(res);
    },

    async getPayment(paymentId) {
      const res = await fetch(`${API_BASE}/payments/${paymentId}`, {
        headers: getAuthHeaders()
      });
      return await handleResponse(res);
    },

    async getPayments() {
      const res = await fetch(`${API_BASE}/payments`, {
        headers: getAuthHeaders()
      });
      return await handleResponse(res);
    }
  },

  // Transaction APIs (USER, ANALYST, ADMIN)
  transactions: {
    async createTransaction(data) {
      const res = await fetch(`${API_BASE}/transactions`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data)
      });
      return await handleResponse(res);
    },

    async getTransaction(transactionId) {
      const res = await fetch(`${API_BASE}/transactions/${transactionId}`, {
        headers: getAuthHeaders()
      });
      return await handleResponse(res);
    },

    async getTransactions(params = {}) {
      const query = new URLSearchParams();
      if (params.status) query.append('status', params.status);
      if (params.type) query.append('type', params.type);
      if (params.page !== undefined) query.append('page', params.page);
      if (params.size !== undefined) query.append('size', params.size);

      const queryString = query.toString();
      const url = `${API_BASE}/transactions${queryString ? `?${queryString}` : ''}`;

      const res = await fetch(url, {
        headers: getAuthHeaders()
      });
      return await handleResponse(res);
    },

    async updateTransactionStatus(transactionId, status) {
      const res = await fetch(`${API_BASE}/transactions/${transactionId}/status?status=${status}`, {
        method: 'PUT',
        headers: getAuthHeaders()
      });
      return await handleResponse(res);
    }
  },

  // Fraud Rule Evaluation APIs (ANALYST & ADMIN)
  fraud: {
    async evaluateRules(data) {
      const res = await fetch(`${API_BASE}/fraud/rules/evaluate`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(data)
      });
      return await handleResponse(res);
    }
  },

  // Health API
  health: {
    async checkHealth() {
      const res = await fetch(`/api/v1/health`);
      return await handleResponse(res);
    }
  }
};
