import { useEffect, useState } from "react";
import { apiErrorMessage } from "../../api/apiClient";
import { getInviteStatus, invitePastAttendees } from "../../api/socialService";
import "../../styles/Social.css";

/** Manage event: invite students who checked in at the organizer's earlier events (once per event). */
export default function InvitePastAttendees({ eventId }) {
  const [status, setStatus] = useState(null);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    getInviteStatus(eventId).then(setStatus).catch(() => setStatus(null));
  }, [eventId]);

  const send = async () => {
    setSending(true);
    setError("");
    try { setStatus(await invitePastAttendees(eventId)); } catch (e) { setError(apiErrorMessage(e)); } finally { setSending(false); }
  };

  if (!status) return null;
  return (
    <section className="invite-bar" aria-label="Invite past attendees">
      {status.sentAt ? (
        <p role="status"><b>Invites sent</b> to {status.recipients} past {status.recipients === 1 ? "attendee" : "attendees"} on{" "}
          {new Date(status.sentAt).toLocaleDateString(undefined, { day: "numeric", month: "short" })}.</p>
      ) : status.canInvite ? (
        <>
          <p><b>{status.eligible} {status.eligible === 1 ? "student" : "students"}</b> checked in at your earlier events and haven't registered yet.</p>
          <button type="button" className="pc-btn pc-btn--primary" onClick={send} disabled={sending}>
            {sending ? "Sending…" : "Invite past attendees"}
          </button>
        </>
      ) : (
        <p className="social-muted">Invite past attendees: {status.reason}.</p>
      )}
      {error && <p className="social-error" role="alert">{error}</p>}
    </section>
  );
}
