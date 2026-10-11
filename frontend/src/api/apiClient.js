import axios from "axios";
import { jwtDecode } from "jwt-decode";

// All requests go directly to the API Gateway — no port 5173 involved
const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

// ─── Token helpers ────────────────────────────────────────────────────────────
// Refresh token lives ONLY in an HttpOnly cookie set by the backend.
// The frontend never reads, stores, or sends it — the browser handles it
// automatically via credentials: 'include'.

export const getAccessToken = () => localStorage.getItem("jwt_token");
export const getUserData = () => {
  try {
    return JSON.parse(localStorage.getItem("user") || "null");
  } catch {
    return null;
  }
};

/** Save only the access token (+ update exp/iat on the stored user object). */
export const saveAccessToken = (jwt) => {
  if (!jwt) return;
  localStorage.setItem("jwt_token", jwt);

  // Update exp/iat in stored user without clobbering other fields
  try {
    const decoded = jwtDecode(jwt);
    const stored = getUserData();
    if (stored) {
      localStorage.setItem(
        "user",
        JSON.stringify({ ...stored, exp: decoded.exp, iat: decoded.iat }),
      );
    }
  } catch {
    /* ignore */
  }
};

/** @deprecated Use saveAccessToken instead */
export const saveTokens = (jwt) => saveAccessToken(jwt);

export const clearTokens = () => {
  localStorage.removeItem("jwt_token");
  localStorage.removeItem("user");
  localStorage.removeItem("google_token");
  // HttpOnly cookie cannot be cleared from JS — backend /auth/logout should handle it
};

// Some third-party auth/analytics scripts may store JSON-style cookie values
// (e.g. g_state) that Netty flags as invalid while parsing Cookie headers.
// Clearing them client-side avoids noisy gateway "invalid char" logs.
const clearProblematicCookies = () => {
  if (typeof document === "undefined") return;
  const names = ["g_state"];
  names.forEach((name) => {
    document.cookie = `${name}=; Max-Age=0; path=/`;
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  });
};

export const isTokenExpired = (token) => {
  if (!token) return true;
  try {
    const { exp } = jwtDecode(token);
    return !exp || exp < Math.floor(Date.now() / 1000);
  } catch {
    return true;
  }
};

// ─── Axios instance ───────────────────────────────────────────────────────────

const api = axios.create({
  baseURL: BASE_URL,
  timeout: 60000, // 60 seconds (increased for file uploads)
  headers: { "Content-Type": "application/json" },
  withCredentials: true, // send httpOnly cookies automatically
});

// ─── Allowed API prefixes — anything else is a frontend route ──────────────────

// Only these path prefixes are real backend endpoints.
// Anything else (e.g. /login, /signup, /dashboard-page) must use React Router.
const ALLOWED_API_PREFIXES = [
  "/auth",
  "/management",
  "/event",
  "/events",
  "/dashboard",
  "/home",
  "/images",
  "/api",
  "/organizer",
  "/admin",
  "/analytics",
  "/certificate",
  "/attendance",
  "/user",
  "/ai",
];

const isFrontendRoute = (url = "") => {
  // Strip the baseURL so we're left with the path
  const path = url.replace(BASE_URL, "").split("?")[0];
  return !ALLOWED_API_PREFIXES.some((prefix) => path.startsWith(prefix));
};

// ─── Request interceptor — guard + attach auth headers ────────────────────────

api.interceptors.request.use(
  (config) => {
    // Keep cookie header clean for gateway/netty parsing.
    clearProblematicCookies();

    // Block accidental calls to frontend-only routes
    const fullUrl =
      config.baseURL && !config.url?.startsWith("http")
        ? config.url
        : config.url?.replace(config.baseURL || "", "");
    if (isFrontendRoute(fullUrl)) {
      console.error(
        `[API Guard] Blocked request to frontend route: ${fullUrl}. ` +
          `Use React Router (navigate / <Link>) for page navigation.`,
      );
      return Promise.reject(
        new Error(
          `BLOCKED: "${fullUrl}" is a frontend route, not an API endpoint.`,
        ),
      );
    }

    // For multipart requests, let browser/axios set boundary automatically.
    if (
      typeof FormData !== "undefined" &&
      config.data instanceof FormData &&
      config.headers
    ) {
      delete config.headers["Content-Type"];
      delete config.headers["content-type"];
    }

    const token = getAccessToken();
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }

    // NOTE: userId / userRoles headers are NOT sent from the frontend.
    // The Gateway's AuthenticationFilter extracts them from the JWT and
    // injects them into the forwarded request automatically.

    return config;
  },
  (error) => Promise.reject(error),
);

// ─── Refresh logic — one call at a time, queue concurrent requests ─────────────

let isRefreshing = false;
let waitQueue = []; // { resolve, reject }

const processQueue = (error, newToken = null) => {
  waitQueue.forEach(({ resolve, reject }) =>
    error ? reject(error) : resolve(newToken),
  );
  waitQueue = [];
};

const doRefresh = async () => {
  // The refresh token is in an HttpOnly cookie — the browser sends it
  // automatically when credentials: 'include' is set.  No body needed.
  const res = await fetch(`${BASE_URL}/auth/refresh`, {
    method: "POST",
    credentials: "include", // sends the HttpOnly cookie
    headers: { "Content-Type": "application/json" },
  });

  if (res.status === 401 || res.status === 403)
    throw new Error("REFRESH_FAILED");
  if (!res.ok) throw new Error("NETWORK_ERROR");

  const data = await res.json();
  const newJwt =
    data.accessToken || data.jwt || data.token || data.access_token;

  if (!newJwt) throw new Error("REFRESH_FAILED");

  saveAccessToken(newJwt);

  // Notify AuthContext so React state updates too
  window.dispatchEvent(
    new CustomEvent("jwt:refreshed", { detail: { token: newJwt } }),
  );

  return newJwt;
};

// ─── Response interceptor — handle HTML detection + 401 → refresh → retry ────

api.interceptors.response.use(
  (response) => {
    // Detect HTML responses (wrong endpoint / proxy returning index.html)
    const ct = response.headers?.["content-type"] || "";
    if (ct.includes("text/html")) {
      console.error(
        `[API] Invalid response from ${response.config?.url} — ` +
          `received HTML instead of JSON. This usually means the URL is wrong ` +
          `or the backend is not running. Content-Type: ${ct}`,
      );
      return Promise.reject(
        new Error(
          `Invalid API response from ${response.config?.url} — received HTML instead of JSON`,
        ),
      );
    }
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // Only attempt refresh on 401 and only once per request
    if (error.response?.status === 401 && !originalRequest._retried) {
      originalRequest._retried = true;

      if (isRefreshing) {
        // Another refresh is in flight — queue and wait
        try {
          const newToken = await new Promise((resolve, reject) => {
            waitQueue.push({ resolve, reject });
          });
          originalRequest.headers["Authorization"] = `Bearer ${newToken}`;
          return api(originalRequest);
        } catch (queueErr) {
          return Promise.reject(queueErr);
        }
      }

      isRefreshing = true;
      try {
        const newToken = await doRefresh();
        processQueue(null, newToken);
        originalRequest.headers["Authorization"] = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshErr) {
        processQueue(refreshErr, null);
        if (refreshErr.message === "REFRESH_FAILED") {
          window.dispatchEvent(new CustomEvent("jwt:forceLogout"));
        }
        return Promise.reject(refreshErr);
      } finally {
        isRefreshing = false;
      }
    }

    // Log non-401 errors with endpoint for easier debugging
    if (error.response) {
      console.error(
        `[API] ${error.config?.method?.toUpperCase()} ${error.config?.url}` +
          ` → ${error.response.status}: ${error.response.data?.message || error.message}`,
      );
    } else {
      console.error(
        `[API] Network error on ${error.config?.url}: ${error.message}`,
      );
    }

    return Promise.reject(error);
  },
);

export default api;
