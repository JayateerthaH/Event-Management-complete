import api from './apiClient';

// ─── Read endpoints ───────────────────────────────────────────────────────────

/** GET /events/{eventId} — full event summary for hero section */
export const fetchEventById = async (eventId) => {
  const res = await api.get(`/events/${eventId}`);
  return res.data;
};

/** GET /events/{eventId}/about — AboutEvent { id, about } */
export const fetchAboutEvent = async (eventId) => {
  try { return (await api.get(`/events/${eventId}/about`)).data; }
  catch { return null; }
};

/** GET /events/{eventId}/details — EventDetails { organizedBy, departments, durationInHours, language, level, contactEmail, contactPhone } */
export const fetchEventDetails = async (eventId) => {
  try { return (await api.get(`/events/${eventId}/details`)).data; }
  catch { return null; }
};

/** GET /events/{eventId}/schedule — EventSchedule { time[], schedule[], smallDescription[] } */
export const fetchSchedule = async (eventId) => {
  try { return (await api.get(`/events/${eventId}/schedule`)).data ?? null; }
  catch { return null; }
};

/** GET /events/{eventId}/faqs — FAQs[] { id, questions, answers } */
export const fetchFaqs = async (eventId) => {
  try {
    const d = (await api.get(`/events/${eventId}/faqs`)).data;
    return Array.isArray(d) ? d : [];
  } catch { return []; }
};

/** GET /events/{eventId}/highlights — KeyHighlights[] { id, highlights, name, smallDescription } */
export const fetchHighlights = async (eventId) => {
  try {
    const d = (await api.get(`/events/${eventId}/highlights`)).data;
    if (Array.isArray(d)) return d;
    // Handle wrapped responses { data: [...] } or single object
    if (d && typeof d === 'object') {
      for (const k of ['data', 'content', 'result', 'items', 'highlights', 'keyHighlights', 'list']) {
        if (Array.isArray(d[k])) return d[k];
      }
      // Single highlight object — wrap in array
      if (d.id || d.name || d.highlights) return [d];
    }
    return [];
  } catch { return []; }
};

/** GET /events/{eventId}/speakers — EventSpeakers[] { id, name, profession, shortExperience } */
export const fetchSpeakers = async (eventId) => {
  try {
    const d = (await api.get(`/events/${eventId}/speakers`)).data;
    return Array.isArray(d) ? d : [];
  } catch { return []; }
};

/** GET /events/{eventId}/whatToBring — WhatToBring[] { id, item } */
export const fetchWhatToBring = async (eventId) => {
  try {
    const d = (await api.get(`/events/${eventId}/whatToBring`)).data;
    return Array.isArray(d) ? d : [];
  } catch { return []; }
};

/** GET /events/{eventId}/points — legacy */
export const fetchPoints = async (eventId) => {
  try {
    const d = (await api.get(`/events/${eventId}/points`)).data;
    return Array.isArray(d) ? d : [];
  } catch { return []; }
};

// ─── Organizer write endpoints ────────────────────────────────────────────────

/** POST /organizer/events/{eventId}/about — { about } */
export const updateAboutEvent = async (eventId, data) => {
  return (await api.post(`/organizer/events/${eventId}/about`, data)).data;
};

/** POST /organizer/events/{eventId}/details — EventDetails body */
export const addEventDetails = async (eventId, data) => {
  return (await api.post(`/organizer/events/${eventId}/details`, data)).data;
};

/** POST /organizer/events/{eventId}/schedule — { time[], schedule[], smallDescription[] } */
export const addSchedule = async (eventId, data) => {
  return (await api.post(`/organizer/events/${eventId}/schedule`, data)).data;
};

/** POST /organizer/events/{eventId}/faqs — { questions, answers } */
export const addFaq = async (eventId, data) => {
  return (await api.post(`/organizer/events/${eventId}/faqs`, data)).data;
};

/** POST /organizer/events/{eventId}/highlights — { name, smallDescription, highlights? } */
export const addHighlight = async (eventId, data) => {
  return (await api.post(`/organizer/events/${eventId}/highlights`, data)).data;
};

/** POST /organizer/events/{eventId}/speakers — { name, profession, shortExperience } */
export const addSpeaker = async (eventId, data) => {
  return (await api.post(`/organizer/events/${eventId}/speakers`, data)).data;
};

/** POST /organizer/events/{eventId}/whatToBring — { item } */
export const addWhatToBring = async (eventId, data) => {
  return (await api.post(`/organizer/events/${eventId}/whatToBring`, data)).data;
};

// Keep old name as alias for backward compat
export const addScheduleSession = addSchedule;
