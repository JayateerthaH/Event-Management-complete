import api from "../api/apiClient";
import { getUserData } from "../api/apiClient";

/**
 * POST /event/{eventId}/register
 *
 * Registers a user for an event.
 * Sends the full registration body including userId and isAttended.
 *
 * @param {number|string} eventId
 * @param {{ name: string, email: string, phone: string, department: string, yearOfStudy: string }} formData
 * @returns {Promise<import('axios').AxiosResponse>}
 */
export const registerForEvent = async (eventId, formData) => {
  const user = getUserData();
  const userId = user?.userId ?? user?.id ?? "";

  if (!userId) {
    throw new Error("User is not logged in. Please log in and try again.");
  }

  if (!eventId) {
    throw new Error("Event ID is missing.");
  }

  const response = await api.post(`/event/${eventId}/register`, {
    name: formData.name,
    email: formData.email,
    phone: formData.phone,
    department: formData.department,
    yearOfStudy: formData.yearOfStudy,
    isAttended: false,
  });

  return response;
};
