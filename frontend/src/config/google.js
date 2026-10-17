// Google sign-in is shown only when a real OAuth client ID is configured (placeholders like
// "your-google-client-id" don't count). The backend must have the same ID in GOOGLE_CLIENT_ID.
export const GOOGLE_CLIENT_ID = (import.meta.env.VITE_GOOGLE_CLIENT_ID || "").trim();
export const GOOGLE_ENABLED = /\.apps\.googleusercontent\.com$/.test(GOOGLE_CLIENT_ID);
