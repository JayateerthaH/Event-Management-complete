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

/**
 * POST /attendance/resend-qr/{eventId}
 * Re-emails the attendance QR for the logged-in student's registration (same code as before).
 * Errors: 404 not registered, 400 attendance already marked, 429 asked too recently, 502 email failed.
 */
export const resendQr = async (eventId) => {
  const { data } = await api.post(`/attendance/resend-qr/${eventId}`);
  return typeof data === "string" ? data : "QR code sent to your email.";
};

/** Seat status for the logged-in student: registered, full, waitlist position, whether they can release. */
export const getSeatStatus = async (eventId) => (await api.get(`/event/${eventId}/seat`)).data;

/** Join a full event's waitlist; the details are used to register automatically when a seat opens. */
export const joinWaitlist = async (eventId, formData) =>
  (await api.post(`/event/${eventId}/waitlist`, {
    name: formData.name, email: formData.email, phone: formData.phone,
    department: formData.department, yearOfStudy: formData.yearOfStudy,
  })).data;

export const leaveWaitlist = async (eventId) => (await api.delete(`/event/${eventId}/waitlist`)).data;

/** "Can't make it": frees the seat for the next student on the waitlist. */
export const releaseSeat = async (eventId) => (await api.delete(`/event/${eventId}/register`)).data;
