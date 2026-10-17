/**
 * imageService.js - event images (served and uploaded via the backend at VITE_API_URL)
 */
import api, { getAccessToken, API_BASE_URL, imageVariant } from "./apiClient";

/** URL of a gallery image ({ url, fileName } from the API, or a file name), optionally `width` pixels wide. */
export const getImageUrl = (image, width) => {
  const url = typeof image === "string" ? image : image?.url || image?.fileName;
  return imageVariant(url, width ? { width } : {});
};

/** PUT /event/images/{imageId}/cover — show this image on the event's cards and page. */
export const setCoverImage = async (imageId) => {
  const { data } = await api.put(`/event/images/${imageId}/cover`);
  return data;
};

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
