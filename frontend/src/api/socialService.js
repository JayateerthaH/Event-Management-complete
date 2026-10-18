import api from "./apiClient";

/** Event buddies, friends, people search, safety and friend notifications (/social). */
const data = (p) => p.then((r) => r.data);

export const getSocialSettings = () => data(api.get("/social/settings"));
export const updateSocialSettings = (body) => data(api.put("/social/settings", body));
export const getAllInterests = () => data(api.get("/social/interests"));

export const getEventBuddies = (eventId) => data(api.get(`/social/events/${eventId}/buddies`));
export const optInToEvent = (eventId) => data(api.post(`/social/events/${eventId}/opt-in`));
export const optOutOfEvent = (eventId) => data(api.delete(`/social/events/${eventId}/opt-in`));

export const searchPeople = (q, interests = []) => {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  interests.forEach((i) => params.append("interest", i));
  return data(api.get(`/social/search?${params}`));
};

export const sayHi = (userId, eventId) => data(api.post("/social/requests", { userId, eventId: eventId ?? null }));
export const getRequests = () => data(api.get("/social/requests"));
export const acceptRequest = (id) => data(api.post(`/social/requests/${id}/accept`));
export const ignoreRequest = (id) => api.post(`/social/requests/${id}/ignore`);
export const cancelRequest = (id) => api.delete(`/social/requests/${id}`);

export const getFriends = () => data(api.get("/social/friends"));
export const removeFriend = (userId) => api.delete(`/social/friends/${userId}`);

export const getBlocked = () => data(api.get("/social/blocks"));
export const blockUser = (userId) => api.post(`/social/blocks/${userId}`);
export const unblockUser = (userId) => api.delete(`/social/blocks/${userId}`);
export const reportUser = (userId, reason) => data(api.post("/social/reports", { userId, reason }));

export const getBuddyNotifications = () => data(api.get("/social/notifications"));
export const markBuddyNotificationsRead = () => api.post("/social/notifications/read");

export const getStudentReports = () => data(api.get("/social/admin/reports"));
export const resolveStudentReport = (id) => api.post(`/social/admin/reports/${id}/resolve`);

export const getAchievements = () => data(api.get("/social/achievements"));

export const getOrganizerForEvent = (eventId) => data(api.get(`/social/organizers/by-event/${eventId}`));
export const followOrganizer = (organizerId) => data(api.post(`/social/follow/${organizerId}`));
export const unfollowOrganizer = (organizerId) => data(api.delete(`/social/follow/${organizerId}`));
export const getFollowing = () => data(api.get("/social/following"));

export const getNotificationSettings = () => data(api.get("/social/notification-settings"));
export const updateNotificationSettings = (body) => data(api.put("/social/notification-settings", body));

export const getMyPortfolio = () => data(api.get("/social/portfolio"));
export const setPortfolioPublished = (published) => data(api.put("/social/portfolio", { published }));
export const resetPortfolioLink = () => data(api.post("/social/portfolio/reset"));
export const getPublicPortfolio = (slug) => data(api.get(`/public/portfolio/${encodeURIComponent(slug)}`));

export const getSemesterGoal = () => data(api.get("/social/goal"));
export const setSemesterGoal = (target) => data(api.put("/social/goal", { target }));
export const clearSemesterGoal = () => data(api.delete("/social/goal"));
export const getDepartmentGoing = (eventId) => data(api.get(`/social/events/${eventId}/department-going`));
export const getInviteStatus = (eventId) => data(api.get(`/social/events/${eventId}/invite-past-attendees`));
export const invitePastAttendees = (eventId) => data(api.post(`/social/events/${eventId}/invite-past-attendees`));
export const getWrapped = (semester) => data(api.get("/social/wrapped", { params: semester ? { semester } : {} }));
export const wrappedShared = () => api.post("/social/wrapped/shared");
export const getEventRecap = (eventId) => data(api.get(`/social/events/${eventId}/recap`));
export const getDepartmentLeaderboard = () => data(api.get("/social/leaderboard/departments"));
export const getSocialProof = (eventId) => data(api.get(`/events/${eventId}/social-proof`));
