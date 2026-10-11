import api from "./apiClient";

// ─── Personal Info  →  PersonalInfoCon  GET|POST /management/students ───────────────
// Backend: @RequestHeader("userId") is auto-added by apiClient interceptor.
// GET  → ResponseEntity<StudentDto>
// POST → ResponseEntity<StudentDto>  body = Student fields

export const fetchProfile = async () => {
  const { data } = await api.get("/management/students");
  return data;
};

export const updateProfile = async (profileData) => {
  const { data } = await api.post("/management/students", profileData);
  return data;
};

// ─── Volunteer Preferences  →  VolunteerCon  GET|POST /management/preferences
// GET  → Set<Preferences>  [{ preferenceId, preferenceType }]
// POST body = Set<Long>  (array of preferenceId numbers)

export const fetchVolunteerPreferences = async () => {
  const { data } = await api.get("/management/preferences");
  return Array.isArray(data) ? data : [];
};

export const updateVolunteerPreferences = async (preferenceIds) => {
  const { data } = await api.post("/management/preferences", preferenceIds);
  return Array.isArray(data) ? data : [];
};

/** GET /management/all-preferences — full master list of available preferences */
export const fetchAllPreferences = async () => {
  const { data } = await api.get("/management/all-preferences");
  return Array.isArray(data) ? data : [];
};

// ─── Achievement Badges  →  BadgesCon  GET|POST /management/badges ───────────────────
// GET  → Set<Badges>  [{ id, badgeName, badgeDescription }]
// POST body = Set<Long>  (array of badgeId numbers for student badge updates)

export const fetchBadges = async () => {
  const { data } = await api.get("/management/badges");
  return Array.isArray(data) ? data : [];
};

export const updateStudentBadges = async (badgeIds) => {
  const { data } = await api.post("/management/badges", badgeIds);
  return Array.isArray(data) ? data : [];
};

/** GET /management/all-badges — full master list of available badge definitions */
export const fetchAllBadges = async () => {
  const { data } = await api.get("/management/all-badges");
  return Array.isArray(data) ? data : [];
};

// ─── Social Profiles  →  SocialProfileCon  GET|POST /management/profile ─────────────
// GET  → Set<SocialProfileUpdate>  [{ id: { platformId, urlLink }, userId }]
// POST body = Map<Long,String>  e.g. { "1": "https://github.com/user" }

export const fetchSocialLinks = async () => {
  const { data } = await api.get("/management/profile");
  return Array.isArray(data) ? data : [];
};

export const updateSocialLinks = async (platformMap) => {
  const { data } = await api.post("/management/profile", platformMap);
  return Array.isArray(data) ? data : [];
};

// ─── Platform Master List (optional — tries admin endpoint) ───────────────────
// GET /management/social  → SocialProfiles[]  [{ id, platform }]
// May fail for non-admin users — caller should handle gracefully.

export const fetchPlatforms = async () => {
  const { data } = await api.get("/management/social");
  return Array.isArray(data) ? data : [];
};

// ─── Interests  →  InterestsCon  GET|POST /management/interests ──────────────
// GET  → Set<Interests>  [{ interestId, interestName }]
// POST body = Set<Long>  (array of interestId numbers)

export const fetchStudentInterests = async () => {
  const { data } = await api.get("/management/interests");
  return Array.isArray(data) ? data : [];
};

export const updateStudentInterests = async (interestIds) => {
  const { data } = await api.post("/management/interests", interestIds);
  return Array.isArray(data) ? data : [];
};

/** GET /management/all-interests — full master list of available interests */
export const fetchAllInterests = async () => {
  const { data } = await api.get("/management/all-interests");
  return Array.isArray(data) ? data : [];
};

// ─── Notification Preferences  →  NotificationCon  GET|POST /management/notifications

export const fetchNotificationPrefs = async () => {
  const { data } = await api.get("/management/notifications");
  return Array.isArray(data) ? data : [];
};

export const updateNotificationPrefs = async (notifIds) => {
  const { data } = await api.post("/management/notifications", notifIds);
  return Array.isArray(data) ? data : [];
};

/** GET /management/all-notifications — master list of notification types */
export const fetchAllNotifications = async () => {
  const { data } = await api.get("/management/all-notifications");
  return Array.isArray(data) ? data : [];
};
