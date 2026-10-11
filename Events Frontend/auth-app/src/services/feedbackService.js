import api from "../api/apiClient";

/**
 * Feedback service — matches FeedbackController endpoints:
 *
 *   GET  /events/feedback/check             — pending feedback for student
 *   POST /events/feedback/submit/{eventId}  — submit feedback
 *   GET  /events/organizer/feedback/{eventId}  — all feedback for event (organizer)
 *   GET  /events/organizer/feedback/request  — request feedback from attendees
 */

// ─── Student ────────────────────────────────────────────────────────────────

/** GET /events/feedback/check → List<EventLink> */
export const fetchPendingFeedback = async () => {
  const { data } = await api.get("/events/feedback/check");
  return Array.isArray(data) ? data : [];
};

/*
 * POST /events/feedback/submit/{eventId}
 * Body: { noOfStars: Long, feedBack: String }
 * Returns: FeedBackDto
 */
export const submitFeedback = async (eventId, feedback) => {
  const { data } = await api.post(`/events/feedback/submit/${eventId}`, {
    noOfStars: feedback.noOfStars,
    feedBack: feedback.feedBack,
  });
  return data;
};

// ─── Organizer ──────────────────────────────────────────────────────────────

/** GET /events/organizer/feedback/{eventId} → all feedback for event */
export const fetchEventFeedback = async (eventId) => {
  const { data } = await api.get(`/events/organizer/feedback/${eventId}`);
  return Array.isArray(data) ? data : [];
};

/**
 * POST /events/organizer/feedback/request
 * Body: { eventId: Long, url: String }
 * Triggers feedback request to all registered attendees
 */
export const requestFeedback = async (eventId, url) => {
  const { data } = await api.post("/events/organizer/feedback/request", {
    eventId,
    url,
  });
  return data;
};
