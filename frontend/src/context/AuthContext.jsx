import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('payshield_token') || null);
  const [primaryRole, setPrimaryRole] = useState('USER');
  const [loading, setLoading] = useState(true);

  // Helper to extract primary role from backend roles list (e.g. ['ROLE_ADMIN', 'ADMIN'])
  const extractPrimaryRole = (rolesList = []) => {
    const cleanRoles = Array.from(rolesList).map(r => r.replace('ROLE_', '').toUpperCase());
    if (cleanRoles.includes('ADMIN')) return 'ADMIN';
    if (cleanRoles.includes('ANALYST')) return 'ANALYST';
    return 'USER';
  };

  useEffect(() => {
    // Restore authenticated session from localStorage if present
    const savedUser = localStorage.getItem('payshield_user');
    const savedToken = localStorage.getItem('payshield_token');

    if (savedUser && savedToken) {
      try {
        const parsedUser = JSON.parse(savedUser);
        setUser(parsedUser);
        setToken(savedToken);
        setPrimaryRole(extractPrimaryRole(parsedUser.roles));
      } catch (e) {
        logout();
      }
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const res = await api.auth.login({ email, password });
    const authData = res.data || res;

    if (authData && authData.accessToken) {
      const rolesArray = Array.from(authData.roles || ['USER']);
      const role = extractPrimaryRole(rolesArray);

      const userProfile = {
        userId: authData.userId,
        email: authData.email,
        name: authData.name || email.split('@')[0],
        roles: rolesArray
      };

      setToken(authData.accessToken);
      setUser(userProfile);
      setPrimaryRole(role);

      localStorage.setItem('payshield_token', authData.accessToken);
      localStorage.setItem('payshield_user', JSON.stringify(userProfile));
      return userProfile;
    } else {
      throw new Error(res.message || 'Authentication failed');
    }
  };

  const register = async (name, email, password) => {
    const res = await api.auth.register({ name, email, password });
    const authData = res.data || res;

    if (authData && authData.accessToken) {
      const rolesArray = Array.from(authData.roles || ['USER']);
      const role = extractPrimaryRole(rolesArray);

      const userProfile = {
        userId: authData.userId,
        email: authData.email,
        name: name,
        roles: rolesArray
      };

      setToken(authData.accessToken);
      setUser(userProfile);
      setPrimaryRole(role);

      localStorage.setItem('payshield_token', authData.accessToken);
      localStorage.setItem('payshield_user', JSON.stringify(userProfile));
      return userProfile;
    } else {
      throw new Error(res.message || 'Registration failed');
    }
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    setPrimaryRole('USER');
    localStorage.removeItem('payshield_token');
    localStorage.removeItem('payshield_user');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        primaryRole,
        loading,
        login,
        register,
        logout
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
