import { useState } from "react";
import { addCollaborator } from "../services/collaboratorService";

/**
 * AddCollaboratorModal
 * Props:
 *   eventId   – the event to add collaborator to
 *   onClose   – close the modal
 *   onSuccess – callback after successful add
 */
export default function AddCollaboratorModal({ eventId, onClose, onSuccess }) {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");

    if (!email.trim()) {
      setError("Please enter a collaborator email.");
      return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) {
      setError("Please enter a valid email address.");
      return;
    }

    setLoading(true);
    try {
      await addCollaborator(eventId, email.trim());
      setSuccess("Collaborator added successfully!");
      setEmail("");
      onSuccess?.();
      setTimeout(() => onClose?.(), 1200);
    } catch (err) {
      const status = err?.response?.status;
      const msg =
        typeof err?.response?.data === "string"
          ? err.response.data
          : err?.response?.data?.message || err?.response?.data?.error;

      if (status === 403) {
        setError("Only the event creator can add collaborators.");
      } else if (
        status === 409 ||
        (msg && msg.toLowerCase().includes("duplicate"))
      ) {
        setError("This collaborator has already been added to the event.");
      } else {
        setError(msg || "Failed to add collaborator. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fb-overlay" onClick={onClose}>
      <div
        className="fb-modal fb-modal--sm"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="fb-modal__header">
          <h3 className="fb-modal__title">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              style={{ width: 20, height: 20 }}
            >
              <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
              <circle cx="8.5" cy="7" r="4" />
              <line x1="20" y1="8" x2="20" y2="14" />
              <line x1="23" y1="11" x2="17" y2="11" />
            </svg>
            Add Collaborator
          </h3>
          <button className="fb-modal__close" onClick={onClose}>
            &times;
          </button>
        </div>

        <form onSubmit={handleSubmit} className="fb-modal__body">
          <p className="fb-hint">
            Enter the email address of the person you want to add as a
            collaborator for this event.
          </p>

          {error && <div className="fb-alert fb-alert--error">{error}</div>}
          {success && (
            <div className="fb-alert fb-alert--success">{success}</div>
          )}

          <div className="fb-field">
            <label className="fb-field__label">Collaborator Email *</label>
            <input
              className="fb-field__input"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="e.g. collaborator@example.com"
              disabled={loading}
              required
            />
          </div>

          <div className="fb-actions">
            <button
              type="button"
              className="fb-btn fb-btn--ghost"
              onClick={onClose}
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="fb-btn fb-btn--primary"
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="fb-spinner" /> Adding...
                </>
              ) : (
                "Add Collaborator"
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
