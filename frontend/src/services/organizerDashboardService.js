import api from "../api/apiClient";

/**
 * Organizer dashboard service — fetches data from multiple endpoints:
 *   GET  /dashboard              — basic organizer info / profile
 *   GET  /dashboard/organizer    — organizer-specific data (events, attendees, etc.)
 *   GET  /dashboard/organizer/analytics — registration trends, charts
 *   POST /event/organizer/create — create a new event
 *   GET  /event/organizer/all   — list all organizer's events (for manage tab)
 *   DELETE /event/organizer/{id} — delete an event
 */

const unwrap = (data) => {
  if (data && typeof data === "object" && !Array.isArray(data)) {
    for (const k of ["data", "result", "response", "payload", "body"]) {
      if (data[k] && typeof data[k] === "object") return data[k];
    }
  }
  return data ?? {};
};

// ─── Read endpoints ─────────────────────────────────────────────────────────

/** GET /dashboard — basic info shared across roles */
/** GET /event/organizer/attendees — registrations across the organizer's events */
export const getOrganizerAttendees = async () => {
  const { data } = await api.get("/event/organizer/attendees");
  return { recentAttendees: Array.isArray(data) ? data : [] };
};

/** GET /dashboard/organizer — organizer-specific overview (events, attendees, stats) */
export const getOrganizerOverview = async () => {
  const { data } = await api.get("/dashboard/organizer");
  return unwrap(data);
};

/** GET /dashboard/organizer/analytics — registration trends, charts data */
export const getOrganizerAnalytics = async () => {
  const { data } = await api.get("/dashboard/organizer/analytics");
  return unwrap(data);
};

/**
 * Fetch all organizer data in parallel.
 * Returns a merged object with keys from all three endpoints.
 * Individual failures are silently swallowed so partial data still renders.
 */
export const fetchAllOrganizerData = async () => {
  const [dashRes, orgRes, analyticsRes] = await Promise.allSettled([
    getOrganizerAttendees(),
    getOrganizerOverview(),
    getOrganizerAnalytics(),
  ]);

  const dashboard = dashRes.status === "fulfilled" ? dashRes.value : {};
  const organizer = orgRes.status === "fulfilled" ? orgRes.value : {};
  const analytics =
    analyticsRes.status === "fulfilled" ? analyticsRes.value : {};

  return { ...dashboard, ...organizer, ...analytics };
};

// ─── Organizer event management ─────────────────────────────────────────────

/** POST /event/organizer/create — plain JSON (no image) */
export const createEvent = async (eventsDto) => {
  const { data } = await api.post("/event/organizer/create", eventsDto, {
    timeout: 30000,
  });
  return data;
};

/** GET /event/organizer/all — list all events created by organizer */
export const fetchOrganizerEvents = async () => {
  try {
    const { data } = await api.get("/event/organizer/all");
    // Handle both plain array and paginated / wrapped responses
    if (Array.isArray(data)) return data;
    if (data && typeof data === "object") {
      for (const k of [
        "data",
        "content",
        "events",
        "result",
        "items",
        "records",
        "list",
      ]) {
        if (Array.isArray(data[k])) return data[k];
      }
    }
    return [];
  } catch {
    return [];
  }
};

/** DELETE /event/organizer/{eventId} — delete an event */
export const deleteEvent = async (eventId) => {
  const { data } = await api.delete(`/event/organizer/${eventId}`);
  return data;
};

// ─── Analytics ──────────────────────────────────────────────────────────────

/** GET /dashboard/organizer/analytics — [{month, count}] */
export const fetchRegistrationAnalytics = async () => {
  const { data } = await api.get("/dashboard/organizer/analytics");
  return Array.isArray(data) ? data : [];
};

// ─── Announcements ──────────────────────────────────────────────────────────

/** POST /organizer/create/{eventId} — { title, message } */
export const createAnnouncement = async (eventId, payload) => {
  const { data } = await api.post(`/organizer/create/${eventId}`, payload);
  return data;
};

// ─── Certificates ───────────────────────────────────────────────────────────

/** POST /certificate/organizer/upload-certificate-template — multipart: file + eventId */
export const uploadCertificateTemplate = async (formData) => {
  const { data } = await api.post(
    "/certificate/organizer/upload-certificate-template",
    formData,
    {
      headers: { "Content-Type": "multipart/form-data" },
    },
  );
  return data;
};
