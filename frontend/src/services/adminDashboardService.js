import api from "../api/apiClient";

/**
 * Admin dashboard service — fetches data from three endpoints:
 *   GET /dashboard       — basic admin info / profile
 *   GET /admin           — admin-specific data (users, events, reports)
 *   GET /analytics       — system-wide analytics, trends, role distribution
 *
 * Each function returns the raw response data (unwrapped from common envelopes).
 */

const unwrap = (data) => {
  if (data && typeof data === "object" && !Array.isArray(data)) {
    for (const k of ["data", "result", "response", "payload", "body"]) {
      if (data[k] && typeof data[k] === "object") return data[k];
    }
  }
  return data ?? {};
};

/** GET /dashboard — basic info shared across roles */
/**
 * GET /admin/overview — { totalUsers, totalEvents, activeOrganizers, platformRevenue }.
 * (Replaces calls to /dashboard, /admin and /analytics, which never existed on the backend.)
 */
export const fetchAllAdminData = async () => {
  const { data } = await api.get("/admin/overview");
  return unwrap(data);
};

/** GET /events/summary/all?page=X&size=Y — paginated event summaries */
export const fetchAllEvents = async (page = 0, size = 6) => {
  const { data } = await api.get("/events/summary/all", {
    params: { page, size },
  });
  return data; // expects Spring Page<EventSummaryDto>
};

/** GET /events/admin/pending-count — { count } of events waiting for approval */
export const fetchPendingEventCount = async () => {
  const { data } = await api.get("/events/admin/pending-count");
  return data?.count ?? 0;
};

// ─── Students & Organizers ──────────────────────────────────────────────────

/** GET /admin/students — list all students (StudentDto: id, firstName, lastName) */
export const fetchStudents = async () => {
  const { data } = await api.get("/admin/students");
  if (Array.isArray(data)) return data;
  if (data && typeof data === "object") {
    for (const k of [
      "data",
      "content",
      "result",
      "items",
      "list",
      "students",
    ]) {
      if (Array.isArray(data[k])) return data[k];
    }
  }
  return [];
};

/** GET /admin/organizer — list all organizers */
export const fetchOrganizers = async () => {
  const { data } = await api.get("/admin/organizer");
  if (Array.isArray(data)) return data;
  if (data && typeof data === "object") {
    for (const k of [
      "data",
      "content",
      "result",
      "items",
      "list",
      "organizers",
    ]) {
      if (Array.isArray(data[k])) return data[k];
    }
  }
  return [];
};

// ─── Onboarding ─────────────────────────────────────────────────────────────

/** GET /admin/onboard — list pending onboard requests */
export const fetchOnboardRequests = async () => {
  const { data } = await api.get("/admin/onboard");
  // Handles both plain array and wrapped responses
  if (Array.isArray(data)) return data;
  if (data && typeof data === "object") {
    for (const k of [
      "data",
      "content",
      "result",
      "items",
      "list",
      "requests",
    ]) {
      if (Array.isArray(data[k])) return data[k];
    }
  }
  return [];
};

/** POST /admin/onBoardNewOrganizer — accept organizer request */
export const acceptOrganizer = async (email) => {
  const { data } = await api.post("/admin/onBoardNewOrganizer", { email });
  return data;
};

/** POST /admin/onBoardNewAdmin — accept admin request */
export const acceptAdmin = async (email) => {
  const { data } = await api.post("/admin/onBoardNewAdmin", { email });
  return data;
};

// ─── Event Approval ─────────────────────────────────────────────────────────

/** PUT /events/admin/approve/{eventId} — approve an event */
export const approveEvent = async (eventId) => {
  const { data } = await api.put(`/events/admin/approve/${eventId}`);
  return data;
};

/** PUT /events/admin/reject/{eventId} — reject an event */
export const rejectEvent = async (eventId) => {
  const { data } = await api.put(`/events/admin/reject/${eventId}`);
  return data;
};

/** GET /admin/users?q= — accounts whose email or profile name contains q: [{ id, email, name, roles[] }] */
export const searchUsers = async (q) => {
  const { data } = await api.get("/admin/users", { params: { q } });
  return Array.isArray(data) ? data : [];
};
