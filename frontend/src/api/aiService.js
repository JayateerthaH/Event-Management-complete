import api, { getAccessToken } from "./apiClient";

/**
 * AI module — matches AiController (/ai/**):
 *
 *   GET  /ai/status                              — { llmEnabled, model }
 *   GET  /ai/recommendations?limit=              — [{ event, score, reasons[] }]
 *   GET  /ai/search?q=                           — { query, aiUsed, filters, note, results[] }
 *   GET  /ai/events/{id}/similar?limit=          — [{ event, score, reasons[] }]   (public)
 *   POST /ai/events/{id}/ask                     — { answer, answeredFromDetails }
 *   POST /ai/events/generate-description         — { description, faqs[{question, answer}] }   (organizer/admin)
 *   POST /ai/events/{id}/draft-announcement      — { title, message }                          (event managers)
 *   GET  /ai/events/{id}/feedback-summary        — { responses, averageStars, aiAvailable, insights, note }
 *
 * LLM calls can take several seconds, so they get a longer timeout than the default.
 */
const LLM_TIMEOUT = 45000;

/** Backend errors are { error, statusCode }; 429 carries "try again in N seconds". */
export const aiErrorMessage = (err, fallback = "Something went wrong. Please try again.") => {
  const data = err?.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  return data?.error || data?.message || (err?.code === "ECONNABORTED" ? "The AI took too long to respond. Please try again." : null) || fallback;
};

// /ai/status needs a login; cache it per access token so pages don't refetch it
let statusCache = { token: null, promise: null };

export const fetchAiStatus = () => {
  const token = getAccessToken();
  if (!token) return Promise.resolve({ llmEnabled: false, model: "none" });
  if (statusCache.token !== token || !statusCache.promise) {
    statusCache = {
      token,
      promise: api
        .get("/ai/status")
        .then((r) => r.data)
        .catch(() => {
          statusCache = { token: null, promise: null };   // retry next time
          return { llmEnabled: false, model: "none" };
        }),
    };
  }
  return statusCache.promise;
};

export const fetchRecommendations = async (limit = 6) => {
  const { data } = await api.get("/ai/recommendations", { params: { limit } });
  return Array.isArray(data) ? data : [];
};

export const aiSearch = async (query) => {
  const { data } = await api.get("/ai/search", { params: { q: query }, timeout: LLM_TIMEOUT });
  return data;
};

export const fetchSimilarEvents = async (eventId, limit = 4) => {
  const { data } = await api.get(`/ai/events/${eventId}/similar`, { params: { limit } });
  return Array.isArray(data) ? data : [];
};

export const askAboutEvent = async (eventId, question) => {
  const { data } = await api.post(`/ai/events/${eventId}/ask`, { question }, { timeout: LLM_TIMEOUT });
  return data;
};

/** @param {{ title, category, eventDate, venue, cost, keyPoints: string[] }} request */
export const generateEventDescription = async (request) => {
  const { data } = await api.post("/ai/events/generate-description", request, { timeout: LLM_TIMEOUT });
  return data;
};

/** @param {{ notes: string, tone: 'FRIENDLY'|'FORMAL'|'URGENT' }} request */
export const draftAnnouncement = async (eventId, request) => {
  const { data } = await api.post(`/ai/events/${eventId}/draft-announcement`, request, { timeout: LLM_TIMEOUT });
  return data;
};

export const fetchFeedbackSummary = async (eventId) => {
  const { data } = await api.get(`/ai/events/${eventId}/feedback-summary`, { timeout: LLM_TIMEOUT });
  return data;
};
