import api from '../api/apiClient';

/**
 * Unwrap a response that may be a plain array or an envelope like
 * { data:[...] } | { content:[...] } | { events:[...] } | { result:[...] }
 * Also handles Spring-style paginated responses: { page: { content: [...] } } or
 * { _embedded: { events: [...] } } etc.
 */
const toArray = (val) => {
  if (Array.isArray(val)) return val;
  if (val && typeof val === 'object') {
    // Top-level keys
    for (const key of ['data', 'content', 'events', 'result', 'items', 'records', 'list', 'registrations', 'pendingEvents']) {
      if (Array.isArray(val[key])) return val[key];
    }
    // Nested: { page: { content: [...] } }
    if (val.page && Array.isArray(val.page.content)) return val.page.content;
    // Spring HATEOAS: { _embedded: { someCollectionName: [...] } }
    if (val._embedded && typeof val._embedded === 'object') {
      const first = Object.values(val._embedded).find(Array.isArray);
      if (first) return first;
    }
    // If the response is a single object (not an array), wrap it
    // so the caller always gets an array.
    if (val.id || val.eventId || val.registrationId) return [val];
  }
  return [];
};

// ─── Student Dashboard ─────────────────────────────────────────────────────────

/** GET /dashboard/user — basic profile + summary stats */
export const fetchUserInfo = async () => {
  const { data } = await api.get('/dashboard/user');
  return data;
};

/** GET /dashboard/events/pending — upcoming / pending registered events */
export const fetchPendingEvents = async () => {
  const { data } = await api.get('/dashboard/events/pending');
  return toArray(data);
};

/** GET /dashboard/events/past — participation history (past events) */
export const fetchPastEvents = async () => {
  const { data } = await api.get('/dashboard/events/past');
  return toArray(data);
};

/**
 * GET /dashboard/user/analytics
 * Expected: [ { month: string, attendanceCount: number }, ... ]
 */
export const fetchUserAnalytics = async () => {
  const { data } = await api.get('/dashboard/user/analytics');
  return toArray(data);
};
/**
 * GET /organizer/collaboratorEvents
 * Returns events where the logged-in student is listed as a collaborator.
 * Each item: { id/eventId, title/name, date/eventDate, venue/location, status }
 */
export const fetchCollaboratorEvents = async () => {
  const { data } = await api.get('/organizer/collaboratorEvents');
  return toArray(data);
};
// ─── Organizer Dashboard ────────────────────────────────────────────────────────

/**
 * Expected response shape:
 * {
 *   totalEvents: number,
 *   totalRegistrations: number,
 *   attendanceCount: number,
 *   revenue: number,
 *   events: [ { id, title, date, registrations, attendance, status } ],
 *   registrationTrends: [ { month, registrations } ],
 *   recentAttendees: [ { id, name, email, event, date } ],
 * }
 */
export const fetchOrganizerDashboard = async () => {
  const { data } = await api.get('/dashboard/organizer/analytics');
  // Unwrap common envelope shapes: { data:{...} }, { result:{...} }, { response:{...} }
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    for (const k of ['data', 'result', 'response', 'payload']) {
      if (data[k] && typeof data[k] === 'object' && !Array.isArray(data[k])) return data[k];
    }
  }
  return data ?? {};
};

// ─── Admin Dashboard ────────────────────────────────────────────────────────────

/**
 * Expected response shape:
 * {
 *   totalUsers: number,
 *   totalEvents: number,
 *   activeOrganizers: number,
 *   platformRevenue: number,
 *   recentUsers: [ { id, name, email, role, joinedDate } ],
 *   pendingEvents: [ { id, title, organizer, date, status } ],
 *   reports: [ { id, type, description, date, status } ],
 *   systemAnalytics: [ { month, users, events } ],
 *   roleDistribution: [ { role, count } ]
 * }
 */
export const fetchAdminDashboard = async () => {
  const { data } = await api.get('/dashboard/admin/analytics');
  // Unwrap common envelope shapes
  if (data && typeof data === 'object' && !Array.isArray(data)) {
    for (const k of ['data', 'result', 'response', 'payload']) {
      if (data[k] && typeof data[k] === 'object' && !Array.isArray(data[k])) return data[k];
    }
  }
  return data ?? {};
};
