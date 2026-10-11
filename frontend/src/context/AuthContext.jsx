/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useRef, useCallback } from 'react';
import { googleLogout } from '@react-oauth/google';
import { jwtDecode } from 'jwt-decode';
import { silentRefresh, isTokenExpiredOrExpiring } from '../utils/apiClient';
import { loginRequest, signupRequest } from '../api/authService';

const AuthContext = createContext(null);

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// Revoke the refresh token server-side and expire its HttpOnly cookie (JS cannot clear it itself).
// Callers that navigate away wait for this (max 3s) so the redirect can't cut the request off;
// keepalive is a backstop if the page unloads anyway.
const revokeSession = () =>
  fetch(`${API_URL}/auth/logout`, {
    method: 'POST',
    credentials: 'include',
    keepalive: true,
    signal: AbortSignal.timeout(3000),
  }).catch(() => {});

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  const [token, setToken] = useState(() => {
    return localStorage.getItem('jwt_token');
  });

  const refreshTimerRef = useRef(null);

  // Force logout — clears all state and redirects to login
  const forceLogout = useCallback(() => {
    if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
    // Revoke on the server first; only then drop stored credentials and reload into /login.
    revokeSession().finally(() => {
      try { googleLogout(); } catch (e /* eslint-disable-line no-unused-vars */) { console.error("Error parsing user data"); }
      // No setUser(null) here: the full reload below resets React state, and clearing it first made the
      // route guard navigate to /login a second time (client-side) before this redirect.
      localStorage.removeItem('user');
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('google_token');
      window.location.href = '/login';
    });
  }, []);

  // Schedule the next proactive refresh based on token expiry
  const scheduleRefresh = useCallback((currentToken) => {
    if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
    if (!currentToken) return;

    try {
      const decoded = jwtDecode(currentToken);
      if (!decoded.exp) return;

      const now = Math.floor(Date.now() / 1000);
      const timeUntilExpiry = decoded.exp - now;

      // Refresh 2 minutes before expiry (minimum 5 seconds from now)
      const refreshIn = Math.max((timeUntilExpiry - 120) * 1000, 5000);

      refreshTimerRef.current = setTimeout(async () => {
        try {
          await silentRefresh();
          // jwt:refreshed event updates state via listener
        } catch (err) {
          if (err.message !== 'NETWORK_ERROR') forceLogout();
          // On network error: do nothing, let next API call retry
        }
      }, refreshIn);
    } catch { /* ignore */ }
  }, [forceLogout]);

  // Listen for token refresh events from apiClient (updates React state)
  useEffect(() => {
    const onRefreshed = (e) => {
      const newToken = e.detail?.token;
      if (newToken) {
        setToken(newToken);
        const stored = localStorage.getItem('user');
        if (stored) {
          try {
            const parsed = JSON.parse(stored);
            setUser(parsed);
          } catch (e /* eslint-disable-line no-unused-vars */) { console.error("Error parsing user data"); }
        }
        scheduleRefresh(newToken);
      }
    };

    const onForceLogout = () => forceLogout();

    window.addEventListener('jwt:refreshed', onRefreshed);
    window.addEventListener('jwt:forceLogout', onForceLogout);

    return () => {
      window.removeEventListener('jwt:refreshed', onRefreshed);
      window.removeEventListener('jwt:forceLogout', onForceLogout);
    };
  }, [forceLogout, scheduleRefresh]);

  // On mount: schedule proactive refresh; if already expired try immediately
  useEffect(() => {
    const storedToken = localStorage.getItem('jwt_token');
    if (!storedToken) return;

    if (isTokenExpiredOrExpiring(storedToken, 0)) {
      // Token is fully expired — try refresh immediately
      silentRefresh().catch((err) => {
        if (err.message !== 'NETWORK_ERROR') forceLogout();
      });
    } else {
      // Token still valid — schedule proactive refresh 2 min before expiry
      scheduleRefresh(storedToken);
      // Also refresh immediately if it expires within the next 5 minutes
      if (isTokenExpiredOrExpiring(storedToken, 300)) {
        silentRefresh().catch(() => { /* ignore — schedule covers it */ });
      }
    }

    return () => {
      if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
    };
  }, [forceLogout, scheduleRefresh]);

  // Login with email/password
  const login = async (credentials) => {
    try {
      const { userData, jwt } = await loginRequest(credentials);
      setToken(jwt);
      setUser(userData);
      scheduleRefresh(jwt);
      return userData;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  };

  // Signup with email/password
  const signup = async (signupData) => {
    try {
      const { userData, jwt } = await signupRequest(signupData);
      setToken(jwt);
      setUser(userData);
      scheduleRefresh(jwt);
      return userData;
    } catch (error) {
      console.error('Signup error:', error);
      throw error;
    }
  };

  // Login with Google OAuth
  const loginWithGoogle = async (credentialResponse) => {
    try {
      const decoded = jwtDecode(credentialResponse.credential);
      
      // You can send this to your backend to verify and get your own JWT
      // For now, storing Google auth data
      const userData = {
        name: decoded.name,
        email: decoded.email,
        picture: decoded.picture,
        sub: decoded.sub,
        authProvider: 'google'
      };
      
      setUser(userData);
      localStorage.setItem('user', JSON.stringify(userData));
      localStorage.setItem('google_token', credentialResponse.credential);

      // Optionally, send to backend to create/login user and get your own JWT
      // const response = await fetch(`${API_URL}/auth/google`, {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify({ token: credentialResponse.credential })
      // });
      
    } catch (error) {
      console.error('Google login error:', error);
      throw error;
    }
  };

  const logout = () => {
    revokeSession();
    try { googleLogout(); } catch (e /* eslint-disable-line no-unused-vars */) { console.error("Error parsing user data"); }
    setUser(null);
    setToken(null);
    localStorage.removeItem('user');
    localStorage.removeItem('jwt_token');
    localStorage.removeItem('google_token');
    if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
  };

  // Get authorization header for API calls
  const getAuthHeader = () => {
    return token ? { Authorization: `Bearer ${token}` } : {};
  };

  return (
    <AuthContext.Provider value={{ 
      user, 
      token,
      login,
      signup,
      loginWithGoogle, 
      logout,
      forceLogout,
      getAuthHeader
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
