import axios from "axios";
import { jwtDecode } from "jwt-decode";

// All requests go directly to the API Gateway — no port 5173 involved
const BASE_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/, "");
export const API_BASE_URL = BASE_URL;
// Login, signup, refresh and logout carry the HttpOnly refresh cookie. In production VITE_AUTH_URL=/api sends
// them through the frontend's own domain (proxied by vercel.json), so the cookie is first-party: Safari blocks
// third-party cookies, and its users would otherwise be logged out whenever the access token expires.
export const AUTH_BASE_URL = (import.meta.env.VITE_AUTH_URL || BASE_URL).replace(/\/+$/, "");

/**
 * Absolute URL for an event image. Images are served by the backend, so relative paths and bare file
 * names must point at VITE_API_URL (the dev-server proxy doesn't exist in production). Old rows stored
 * as http://localhost:8080/images/... are rebased too; other absolute URLs are kept.
 */
export const resolveImageUrl = (url) => {
  if (!url) return null;
  if (/^https?:\/\//i.test(url)) {
    try {
      const u = new URL(url);
      if (/^(localhost|127\.0\.0\.1)$/.test(u.hostname) && u.pathname.startsWith("/images/")) {
        return BASE_URL + u.pathname;
      }
    } catch {
      /* fall through */
    }
    return url;
  }
  if (url.startsWith("/")) return BASE_URL + url;
  return `${BASE_URL}/images/${url}`;
};

/**
 * Image URL at a given size. For Cloudinary images, Cloudinary resizes and converts on the fly: the
 * best format the browser supports (WebP/AVIF), automatic quality, `width` pixels wide, and with `ratio`
 * (e.g. "16:9") a smart crop that keeps the important part in frame — like a YouTube thumbnail.
 * Other images (stored on the server's disk) are returned as they are.
 */
const CLOUDINARY_UPLOAD = /^(https:\/\/res\.cloudinary\.com\/[^/]+\/image\/upload\/)(.+)$/;
export const imageVariant = (url, { width, ratio } = {}) => {
  const resolved = resolveImageUrl(url);
  const match = resolved && resolved.match(CLOUDINARY_UPLOAD);
  if (!match || /^[a-z]{1,3}_[^/]*\//.test(match[2]) && !/^v\d+\//.test(match[2])) return resolved;
  const parts = ["f_auto", "q_auto"];
  if (ratio) parts.push("c_fill", "g_auto", `ar_${ratio}`);
  else parts.push("c_limit");
  if (width) parts.push(`w_${width}`);
  return `${match[1]}${parts.join(",")}/${match[2]}`;
};

/** srcSet for a Cloudinary image at several widths (undefined for other images, so the browser uses src). */
export const imageSrcSet = (url, widths, ratio) => {
  const resolved = resolveImageUrl(url);
  if (!resolved || !CLOUDINARY_UPLOAD.test(resolved)) return undefined;
  return widths.map((w) => `${imageVariant(url, { width: w, ratio })} ${w}w`).join(", ");
};

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

/** Save only the access token (+ update roles and exp/iat on the stored user object). */
export const saveAccessToken = (jwt) => {
  if (!jwt) return;
  localStorage.setItem("jwt_token", jwt);

  // Update the stored user from the new token without clobbering other fields. Roles come from the token
  // too: a refreshed token carries roles granted since login (e.g. a student accepted as organizer/admin),
  // and keeping the old ones showed the wrong dashboard.
  try {
    const decoded = jwtDecode(jwt);
    const stored = getUserData();
    if (stored) {
      localStorage.setItem(
        "user",
        JSON.stringify({
          ...stored,
          ...(Array.isArray(decoded.roles) ? { roles: decoded.roles } : {}),
          exp: decoded.exp,
          iat: decoded.iat,
        }),
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

// ─── User-facing error messages ───────────────────────────────────────────────

const STATUS_MESSAGES = {
  400: "Some of the information is invalid. Please check it and try again.",
  401: "Your session has expired. Please log in again.",
  403: "You don't have permission to do that.",
  404: "That item could not be found. It may have been removed.",
  409: "This conflicts with existing data.",
  413: "The file is too large.",
};

/**
 * The message to show the user for a failed request: the backend's own explanation (ApiError's `error`)
 * when there is one, otherwise a plain description of what went wrong — never axios's
 * "Request failed with status code 409".
 */
export const apiErrorMessage = (err, fallback = "Something went wrong. Please try again.") => {
  const data = err?.response?.data;
  if (typeof data === "string" && data.trim() && !data.trim().startsWith("<")) return data.trim();
  const fromBody = data?.error || data?.message || data?.detail;
  if (typeof fromBody === "string" && fromBody.trim()) return fromBody.trim();
  const status = err?.response?.status;
  if (status >= 500) return "The server ran into a problem. Please try again in a moment.";
  if (status) return STATUS_MESSAGES[status] || fallback;
  if (err?.code === "ECONNABORTED" || /timeout/i.test(err?.message || ""))
    return "The server took too long to respond. Please try again.";
  if (err?.code === "ERR_NETWORK" || err?.message === "Network Error")
    return "Can't reach the server. Check your internet connection and try again.";
  return err?.message || fallback;
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

// ─── Refresh logic — one call at a time, shared by every caller ────────────────

const doRefresh = async () => {
  // The refresh token is in an HttpOnly cookie — the browser sends it
  // automatically when credentials: 'include' is set.  No body needed.
  const res = await fetch(`${AUTH_BASE_URL}/auth/refresh`, {
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

// Each refresh rotates the refresh cookie, so a second concurrent refresh would present an already-used
// token and be rejected (logging the user out). Every caller — the 401 interceptor and AuthContext's
// startup/scheduled refresh — shares this one in-flight request.
let refreshPromise = null;

export const refreshAccessToken = () => {
  if (!refreshPromise) {
    refreshPromise = doRefresh()
      .catch((err) => {
        if (err.message === "REFRESH_FAILED") {
          window.dispatchEvent(new CustomEvent("jwt:forceLogout"));
        }
        throw err;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
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

      // Sent with a token that has been replaced since (a refresh finished meanwhile): just retry
      const current = getAccessToken();
      if (current && originalRequest.headers?.["Authorization"] !== `Bearer ${current}`) {
        originalRequest.headers["Authorization"] = `Bearer ${current}`;
        return api(originalRequest);
      }

      try {
        const newToken = await refreshAccessToken();
        originalRequest.headers["Authorization"] = `Bearer ${newToken}`;
        return api(originalRequest);
      } catch (refreshErr) {
        return Promise.reject(refreshErr);
      }
    }

    // Every caller that shows err.message now shows the backend's explanation (or a plain one)
    error.message = apiErrorMessage(error, error.message);

    // Log non-401 errors with endpoint for easier debugging
    if (error.response) {
      console.error(
        `[API] ${error.config?.method?.toUpperCase()} ${error.config?.url}` +
          ` → ${error.response.status}: ${error.message}`,
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
