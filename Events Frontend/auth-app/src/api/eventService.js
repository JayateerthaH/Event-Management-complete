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
  const sampleSize = 300;
  // Public, approved-only endpoint (GET /events/summary/all is admin-only and includes unapproved events)
  const data = await fetchFilteredEvents({ page: 0, size: sampleSize });
  const list = Array.isArray(data.content) ? data.content : [];

  const categoryMap = new Map();
  const venueMap = new Map();
  const departmentMap = new Map();

  list.forEach((ev) => {
    const catRaw = normalizeValue(ev?.category);
    const venueRaw = normalizeValue(ev?.venue);
    const deptRaw = normalizeValue(ev?.eventField || ev?.field || ev?.department);

    if (catRaw) {
      const key = catRaw.toLowerCase();
      const prev = categoryMap.get(key) || {
        id: key,
        label: toTitleCase(catRaw),
        count: 0,
      };
      prev.count += 1;
      categoryMap.set(key, prev);
    }

    if (venueRaw) {
      const key = venueRaw.toLowerCase();
      const prev = venueMap.get(key) || { id: key, label: venueRaw };
      venueMap.set(key, prev);
    }

    if (deptRaw) {
      const key = deptRaw.toLowerCase();
      const prev = departmentMap.get(key) || {
        id: key,
        label: toTitleCase(deptRaw),
      };
      departmentMap.set(key, prev);
    }
  });

  const categories = Array.from(categoryMap.values()).sort(
    (a, b) => b.count - a.count || a.label.localeCompare(b.label),
  );
  const venues = Array.from(venueMap.values()).sort((a, b) =>
    a.label.localeCompare(b.label),
  );
  const departments = [
    { id: "all", label: "All Departments" },
    ...Array.from(departmentMap.values()).sort((a, b) =>
      a.label.localeCompare(b.label),
    ),
  ];

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
