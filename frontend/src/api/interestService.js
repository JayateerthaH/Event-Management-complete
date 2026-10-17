import api from "./apiClient";

/** Interests a student can pick: the admin's list plus event categories. */
export const getInterestOptions = () => api.get("/management/interest-options").then((r) => r.data);

/** The student's current interest names. */
export const getMyInterestNames = () =>
  api.get("/management/interests").then((r) => (Array.isArray(r.data) ? r.data.map((i) => i.interestName) : []));

/** Replaces the student's interests with these names. */
export const saveInterestNames = (names) => api.put("/management/interests/by-name", { names }).then((r) => r.data);
