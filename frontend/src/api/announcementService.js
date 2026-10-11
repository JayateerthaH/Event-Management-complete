import api from "./apiClient";

/** GET /events/{eventId}/announcements — event-specific announcements for current user */
export const fetchEventAnnouncements = async (eventId) => {
  const { data } = await api.get(`/events/${eventId}/announcements`);
  return Array.isArray(data) ? data : [];
};

/** GET /user/announcement — all announcements for current user (across events) */
export const fetchUserAnnouncements = async () => {
  const { data } = await api.get("/user/announcement");
  return Array.isArray(data) ? data : [];
};
