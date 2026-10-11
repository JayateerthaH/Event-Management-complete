import api from "../api/apiClient";

/**
 * Collaborator service — matches OrganizerController endpoints:
 *
 *   POST /organizer/addCollaborator — add a collaborator to an event
 *   GET  /organizer/complete/{eventId} — mark event as completed
 */

/**
 * POST /organizer/addCollaborator
 * Body: { eventId: Long, email: String }
 */
export const addCollaborator = async (eventId, collaboratorEmail) => {
  const { data } = await api.post("/organizer/addCollaborator", {
    eventId: Number(eventId),
    email: collaboratorEmail,
  });
  return data;
};

/**
 * GET /organizer/complete/{eventId}
 * Marks the event as completed and triggers badge/update distribution
 */
export const completeEvent = async (eventId) => {
  const { data } = await api.get(`/organizer/complete/${eventId}`);
  return data;
};
