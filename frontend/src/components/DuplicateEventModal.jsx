import { useState } from "react";
import api, { apiErrorMessage } from "../api/apiClient";

/** Organizer: copy an event (details, schedule, speakers, FAQs...) to a new date. It needs admin approval again. */
export default function DuplicateEventModal({ event, onClose, onDuplicated }) {
  const [name, setName] = useState(event.title || "");
  const [date, setDate] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(null);

  const submit = async (e) => {
    e.preventDefault();
    if (!date) { setError("Choose a date and time for the new event."); return; }
    const when = new Date(date);
    if (Number.isNaN(when.getTime()) || when <= new Date()) { setError("The new event's date must be in the future."); return; }
    setBusy(true);
    setError("");
    try {
      const { data } = await api.post(`/organizer/events/${event.id}/duplicate`, { eventDate: when.toISOString(), name });
      setDone(data);
      onDuplicated?.(data);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="fb-overlay" onClick={onClose}>
      <div className="fb-modal fb-modal--sm" role="dialog" aria-modal="true" aria-labelledby="dup-title" onClick={(e) => e.stopPropagation()}>
        <div className="fb-modal__header">
          <h3 className="fb-modal__title" id="dup-title">Duplicate event</h3>
          <button type="button" className="fb-modal__close" aria-label="Close" onClick={onClose}>&times;</button>
        </div>
        {done ? (
          <div className="fb-modal__body">
            <div className="fb-alert fb-alert--success">{done.message}</div>
            <button type="button" className="fb-btn fb-btn--primary" onClick={onClose}>Done</button>
          </div>
        ) : (
          <form onSubmit={submit} className="fb-modal__body">
            <p className="fb-hint">
              Copies the details, schedule, speakers, FAQs, highlights and what to bring. Registrations, attendance,
              announcements and images aren't copied. The new event needs admin approval.
            </p>
            {error && <div className="fb-alert fb-alert--error" role="alert">{error}</div>}
            <div className="fb-field">
              <label className="fb-field__label" htmlFor="dup-name">Name</label>
              <input id="dup-name" className="fb-field__input" value={name} maxLength={255} required
                onChange={(e) => setName(e.target.value)} />
            </div>
            <div className="fb-field">
              <label className="fb-field__label" htmlFor="dup-date">New date and time *</label>
              <input id="dup-date" type="datetime-local" className="fb-field__input" value={date} required
                onChange={(e) => setDate(e.target.value)} />
            </div>
            <div className="fb-modal__actions" style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
              <button type="button" className="fb-btn fb-btn--ghost" onClick={onClose}>Cancel</button>
              <button type="submit" className="fb-btn fb-btn--primary" disabled={busy}>{busy ? "Duplicating…" : "Duplicate"}</button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
