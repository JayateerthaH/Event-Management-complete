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
  refreshAccessToken,
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

// ─── silentRefresh — used by AuthContext (startup and scheduled refresh) ─────
// Shares the single in-flight refresh with the 401 interceptor; two concurrent refreshes would
// reuse a rotated refresh token and log the user out.
export const silentRefresh = () => refreshAccessToken();

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
