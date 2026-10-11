import api from "../api/apiClient";

/**
 * Certificate service — matches CertificateTemplateController endpoints:
 *
 *   POST /certificate/organizer/upload-certificate-template — upload template
 *   GET  /certificate/eligible                              — eligible certs for student
 *   GET  /certificate/download/{eventId}                    — download PDF
 */

// ─── Organizer ──────────────────────────────────────────────────────────────

/**
 * POST /certificate/organizer/upload-certificate-template
 * FormData: file (MultipartFile), eventId (Long)
 */
export const uploadCertificateTemplate = async (file, eventId) => {
  const fd = new FormData();
  fd.append("file", file);
  fd.append("eventId", eventId);
  const { data } = await api.post(
    "/certificate/organizer/upload-certificate-template",
    fd,
    {
      headers: { "Content-Type": "multipart/form-data" },
    },
  );
  return data;
};

// ─── Student ────────────────────────────────────────────────────────────────

/**
 * GET /certificate/eligible
 * Returns: [{ eventId, eventTitle, eventDate }]
 */
export const fetchEligibleCertificates = async () => {
  const { data } = await api.get("/certificate/eligible");
  return Array.isArray(data) ? data : [];
};

/**
 * GET /certificate/download/{eventId}
 * Returns: PDF blob
 */
export const downloadCertificate = async (eventId) => {
  try {
    const response = await api.get(`/certificate/download/${eventId}`, {
      responseType: "blob",
    });
    // Trigger download in browser
    const blob = new Blob([response.data], { type: "application/pdf" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `certificate-${eventId}.pdf`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    return true;
  } catch (err) {
    // When responseType is 'blob', error response data is also a Blob.
    // Parse it to extract the server's error message.
    if (err?.response?.data instanceof Blob) {
      try {
        const text = await err.response.data.text();
        const json = JSON.parse(text);
        const msg = json.message || json.error || text;
        throw new Error(msg);
      } catch (parseErr) {
        if (parseErr.message && parseErr.message !== "[object Object]") {
          throw parseErr;
        }
      }
    }
    throw err;
  }
};
