/**
 * Backward-compatibility shim — all existing imports from utils/apiClient
 * continue to work unchanged while everything is now Axios-backed.
 */
import api, {
  getAccessToken,
  saveAccessToken,
  saveTokens,      // deprecated alias
  clearTokens,
  isTokenExpired,
  AUTH_BASE_URL,
} from '../api/apiClient';
import { jwtDecode } from 'jwt-decode';

export { getAccessToken, saveAccessToken, saveTokens, clearTokens, isTokenExpired };

// ─── isTokenExpiredOrExpiring (used by AuthContext) ───────────────────────────
export const isTokenExpiredOrExpiring = (token, bufferSeconds = 60) => {
  if (!token) return true;
  try {
    const { exp } = jwtDecode(token);
    if (!exp) return true;
    return exp - Math.floor(Date.now() / 1000) < bufferSeconds;
  } catch { return true; }
};

// ─── silentRefresh — used by the AuthContext scheduled timer ──────────────────
let _isRefreshing = false;
let _queue = [];

const processQueue = (err, token = null) => {
  _queue.forEach(({ resolve, reject }) => err ? reject(err) : resolve(token));
  _queue = [];
};

export const silentRefresh = async () => {
  if (_isRefreshing) {
    return new Promise((resolve, reject) => _queue.push({ resolve, reject }));
  }
  _isRefreshing = true;

  const BASE_URL = AUTH_BASE_URL;

  try {
    // Refresh token is in an HttpOnly cookie — browser sends it automatically.
    // No body needed.
    const res = await fetch(`${BASE_URL}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json' },
    });

    if (res.status === 401 || res.status === 403) throw new Error('REFRESH_FAILED');
    if (!res.ok) throw new Error('NETWORK_ERROR');

    const data   = await res.json();
    const newJwt = data.accessToken || data.jwt || data.token || data.access_token;
    if (!newJwt)  throw new Error('REFRESH_FAILED');

    saveAccessToken(newJwt);
    window.dispatchEvent(new CustomEvent('jwt:refreshed', { detail: { token: newJwt } }));
    processQueue(null, newJwt);
    return newJwt;
  } catch (err) {
    processQueue(err);
    if (err.message === 'REFRESH_FAILED') {
      window.dispatchEvent(new CustomEvent('jwt:forceLogout'));
    }
    throw err;
  } finally {
    _isRefreshing = false;
  }
};

// ─── Fetch-like response wrapper ─────────────────────────────────────────────
const toFetchLike = (axiosRes) => ({
  ok:     axiosRes.status >= 200 && axiosRes.status < 300,
  status: axiosRes.status,
  json:   () => Promise.resolve(axiosRes.data),
  data:   axiosRes.data,
});

// ─── apiGet / apiPost — drop-in fetch replacements ───────────────────────────
export const apiGet = async (url, options = {}) => {
  try {
    const res = await api.get(url, { headers: options.headers });
    return toFetchLike(res);
  } catch (err) {
    if (err.message === 'REFRESH_FAILED') throw new Error('SESSION_EXPIRED');
    if (err.response) return toFetchLike(err.response);
    throw err;
  }
};

export const apiPost = async (url, body, options = {}) => {
  try {
    const res = await api.post(url, body, { headers: options.headers });
    return toFetchLike(res);
  } catch (err) {
    if (err.message === 'REFRESH_FAILED') throw new Error('SESSION_EXPIRED');
    if (err.response) return toFetchLike(err.response);
    throw err;
  }
};

export const apiRequest = apiGet;
export default api;
