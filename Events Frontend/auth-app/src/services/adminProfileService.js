import api from "../api/apiClient";

const toArray = (data) => {
  if (Array.isArray(data)) return data;
  if (data && typeof data === "object") {
    for (const key of ["data", "content", "items", "list", "result"]) {
      if (Array.isArray(data[key])) return data[key];
    }
    return [data];
  }
  return [];
};

// ─── Badges ─────────────────────────────────────────────────────────────────────

export const createBadge = async (badge) => {
  const { data } = await api.post("/management/create/badge", badge);
  return data;
};

export const getBadge = async () => {
  const { data } = await api.get("/management/badge");
  return data;
};

export const getBadges = async () => {
  const { data } = await api.get("/management/all-badges");
  return toArray(data);
};

export const updateBadge = async (badge) => {
  const { data } = await api.post("/management/update/badge", badge, {
    params: { id: badge?.id },
  });
  return data;
};

export const deleteBadge = async (id) => {
  await api.get("/management/delete/badge", { params: { id } });
};

// ─── Interests ──────────────────────────────────────────────────────────────────

export const createInterest = async (interest) => {
  const { data } = await api.post("/management/create/interest", interest);
  return data;
};

export const getInterest = async () => {
  const { data } = await api.get("/management/interest");
  return data;
};

export const getInterests = async () => {
  const { data } = await api.get("/management/all-interests");
  return toArray(data);
};

export const updateInterest = async (interest) => {
  const { data } = await api.post("/management/update/interest", interest, {
    params: { id: interest?.interestId },
  });
  return data;
};

export const deleteInterest = async (id) => {
  await api.get("/management/delete/interest", { params: { id } });
};

// ─── Notifications ──────────────────────────────────────────────────────────────

export const createNotification = async (notification) => {
  const { data } = await api.post(
    "/management/create/notification",
    notification,
  );
  return data;
};

export const getNotification = async () => {
  const { data } = await api.get("/management/notification");
  return data;
};

export const getNotifications = async () => {
  const { data } = await api.get("/management/all-notifications");
  return toArray(data);
};

export const updateNotification = async (notification) => {
  const { data } = await api.post(
    "/management/update/notification",
    notification,
    {
      params: { id: notification?.id },
    },
  );
  return data;
};

export const deleteNotification = async (id) => {
  await api.get("/management/delete/notification", { params: { id } });
};

// ─── Social Profiles ────────────────────────────────────────────────────────────

export const addSocialProfile = async (socialProfile) => {
  const { data } = await api.post("/management/add-social", socialProfile);
  return data;
};

export const getSocialProfiles = async () => {
  const { data } = await api.get("/management/social");
  return Array.isArray(data) ? data : [];
};

export const updateSocialProfile = async (socialProfile) => {
  const { data } = await api.post(
    "/management/update/social-profile",
    socialProfile,
    {
      params: { id: socialProfile?.id },
    },
  );
  return data;
};

export const deleteSocialProfile = async (id) => {
  await api.delete("/management/delete/social-profile", { params: { id } });
};

// ─── Preferences ────────────────────────────────────────────────────────────────

export const createPreference = async (preference) => {
  const { data } = await api.post("/management/create/preference", preference);
  return data;
};

export const getPreference = async () => {
  const { data } = await api.get("/management/preference");
  return data;
};

export const getPreferences = async () => {
  const { data } = await api.get("/management/all-preferences");
  return toArray(data);
};

export const updatePreference = async (preference) => {
  const { data } = await api.post("/management/update/preference", preference, {
    params: { id: preference?.preferenceId },
  });
  return data;
};

export const deletePreference = async (id) => {
  await api.get("/management/delete/preference", { params: { id } });
};
