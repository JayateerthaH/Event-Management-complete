import { useSyncExternalStore } from "react";
import api, { getAccessToken } from "./apiClient";

/**
 * Saved ("Interested") events. The ids are loaded once per session and shared by every save button,
 * so a card and the event page always agree.
 */
let ids = new Set();
let loadedFor = null;
const listeners = new Set();
const emit = () => listeners.forEach((l) => l());

export const loadSavedIds = async (force = false) => {
  const token = getAccessToken();
  if (!token) { ids = new Set(); loadedFor = null; emit(); return ids; }
  if (!force && loadedFor === token) return ids;
  loadedFor = token;
  try {
    const { data } = await api.get("/events/saved/ids");
    ids = new Set((Array.isArray(data) ? data : []).map(Number));
  } catch {
    loadedFor = null;
  }
  emit();
  return ids;
};

export const isSaved = (eventId) => ids.has(Number(eventId));

export const setSaved = async (eventId, saved) => {
  const id = Number(eventId);
  const before = new Set(ids);
  ids = new Set(ids);
  if (saved) ids.add(id); else ids.delete(id);
  emit();
  try {
    const { data } = saved ? await api.post(`/events/${id}/save`) : await api.delete(`/events/${id}/save`);
    return data;
  } catch (err) {
    ids = before;   // undo the optimistic change
    emit();
    throw err;
  }
};

export const getSavedEvents = () => api.get("/events/saved").then((r) => (Array.isArray(r.data) ? r.data : []));

const subscribe = (l) => { listeners.add(l); return () => listeners.delete(l); };
let snapshot = ids;
const getSnapshot = () => { if (snapshot !== ids) snapshot = ids; return snapshot; };

/** The current set of saved ids (re-renders when it changes). */
export const useSavedIds = () => useSyncExternalStore(subscribe, getSnapshot);
