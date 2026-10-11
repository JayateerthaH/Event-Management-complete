/**
 * imageService.js - event images (served and uploaded via the backend at VITE_API_URL)
 */
import api, { getAccessToken, API_BASE_URL, resolveImageUrl } from "./apiClient";

export const getImageUrl = (fileName) => resolveImageUrl(fileName);

export const uploadEventImage = async (eventId, file) => {
  const formData = new FormData();
  formData.append("file", file);
  const token = getAccessToken();
  const res = await fetch(`${API_BASE_URL}/event/images/upload/${eventId}`, {
    method: "POST",
    body: formData,
    credentials: "include",
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    // Backend errors are JSON ApiError bodies: { error, statusCode, timeStamp }
    const text = await res.text().catch(() => "");
    let message = text;
    try {
      message = JSON.parse(text).error || text;
    } catch {
      /* not JSON — use the raw text */
    }
    throw new Error(message || `Upload failed with status ${res.status}`);
  }
  return res.json();
};

export const getEventImages = async (eventId) => {
  const res = await api.get(`/event/images/${eventId}`);
  return Array.isArray(res.data) ? res.data : [];
};

export const deleteEventImage = async (imageId) => {
  await api.delete(`/event/images/${imageId}`);
};
