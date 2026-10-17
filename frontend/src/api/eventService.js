import api from "./apiClient";

const normalizeValue = (val) => (typeof val === "string" ? val.trim() : "");

const toTitleCase = (val) => {
  const v = normalizeValue(val);
  if (!v) return "";
  return v
    .toLowerCase()
    .split(/\s+/)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
};

/**
 * Build URLSearchParams from page, size, and filter object.
 */
const buildParams = (page, size, filters = {}) => {
  const params = new URLSearchParams();
  params.set("page", page);
  params.set("size", size);

  if (filters.search?.trim()) params.set("name", filters.search.trim());
  if (filters.categories?.length)
    filters.categories.forEach((c) => params.append("category", c));
  if (filters.venues?.length)
    filters.venues.forEach((v) => params.append("venue", v));
  if (filters.department && filters.department !== "all")
    params.set("department", filters.department);
  if (filters.fee === "free") params.set("costOfRegistration", "Free");
  if (filters.fee === "paid") params.set("paid", "true");
  if (filters.date) params.set("dateFilter", filters.date);

  return params.toString();
};

/**
 * Fetch paginated event summaries.
 * Returns { content, totalPages, totalElements, currentPage }
 */
export const fetchEvents = async (page = 0, size = 6, filters = {}) => {
  const qs = buildParams(page, size, filters);
  const res = await api.get(`/events/summary/all?${qs}`);
  const data = res.data;

  // Backend wraps pagination in data.page
  const pageInfo = data.page || {};
  const content = data.content || [];
  const totalElements =
    pageInfo.totalElements ?? data.totalElements ?? content.length;
  const totalPages =
    pageInfo.totalPages ??
    data.totalPages ??
    (Math.ceil(totalElements / size) || 1);
  const currentPage = pageInfo.number ?? data.number ?? page;

  return { content, totalPages, totalElements, currentPage };
};

/**
 * Fetch events using backend filter API.
 * Backend contract: POST /events/filter with EventFilterRequest payload.
 */
export const fetchFilteredEvents = async ({
  page = 0,
  size = 6,
  search = "",
  category = "",
  venue = "",
  categories = [],
  venues = [],
  department = "",
  dateRange = "",
  costType = "",
} = {}) => {
  // Use local normalizer that returns null for empty/all values (backend ignores nulls)
  const normalizeLocal = (val) => (val === "all" || !val || String(val).trim() === "" ? null : String(val).trim());

  const payload = {
    page,
    size,
    search: normalizeLocal(search),
    category: normalizeLocal(category),
    venue: normalizeLocal(venue),
    // Several categories/venues are matched by the backend (any of them), so every filter is server-side
    categories: categories.map(normalizeLocal).filter(Boolean),
    venues: venues.map(normalizeLocal).filter(Boolean),
    eventField: normalizeLocal(department),
    dateRange: normalizeLocal(dateRange),
    costType: normalizeLocal(costType),
  };
   
  const res = await api.post("/events/filter", payload);
  const data = res.data || {};

  // Spring Boot may nest pagination in data.page (Page<> serialization)
  const pageInfo = data.page || {};
  return {
    content: data.content || [],
    totalPages: pageInfo.totalPages ?? data.totalPages ?? 0,
    totalElements: pageInfo.totalElements ?? data.totalElements ?? 0,
    currentPage: pageInfo.number ?? data.number ?? page,
  };
};

/**
 * Build dynamic filter options from real events.
 * This removes hardcoded dummy categories/venues/departments in the sidebar.
 */
export const fetchFilterOptions = async () => {
  // Counted by the backend over approved events (this used to download up to 300 events to count them here)
  const { data } = await api.get("/events/filter-options");

  const categories = (data?.categories || [])
    .map((c) => ({ id: String(c.value).toLowerCase(), label: toTitleCase(c.value), count: Number(c.count) || 0 }))
    .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label));

  const unique = (values, label) => {
    const map = new Map();
    for (const raw of values || []) {
      const value = normalizeValue(raw);
      if (value && !map.has(value.toLowerCase())) map.set(value.toLowerCase(), { id: value.toLowerCase(), label: label(value) });
    }
    return Array.from(map.values()).sort((a, b) => a.label.localeCompare(b.label));
  };
  const venues = unique(data?.venues, (v) => v);
  const departments = [{ id: "all", label: "All Departments" }, ...unique(data?.departments, toTitleCase)];

  return { categories, venues, departments };
};

/**
 * Fetch a single event by ID.
 */
export const fetchEventById = async (id) => {
  const res = await api.get(`/events/${id}`);
  return res.data;
};

/**
 * POST /events/{eventId}/register
 * Registers a user for an event.
 * @param {number|string} eventId
 * @param {{ name, email, phone, department, yearOfStudy }} body
 * @param {number|string} userId  – sent as request header
 * @deprecated Use registrationService.js instead
 */
export const registerForEvent = async (eventId, body) => {
  const res = await api.post(`/events/${eventId}/register`, body);
  return res.data;
};
