import { useCallback, useEffect, useState } from "react";
import { apiErrorMessage, getAccessToken } from "../../api/apiClient";
import { getSeatStatus, leaveWaitlist, releaseSeat } from "../../services/registrationService";
import "../../styles/Social.css";
import "../../styles/Engagement.css";

/**
 * Event sidebar, for a logged-in student: "Release your seat" when registered (before the event, not yet
 * checked in), or the waitlist when the event is full.
 */
export default function SeatControls({ eventId, onJoinWaitlist, onChanged, refreshKey }) {
  const [status, setStatus] = useState(null);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    if (!getAccessToken()) return;
    getSeatStatus(eventId).then(setStatus).catch(() => setStatus(null));
  }, [eventId]);
  useEffect(load, [load, refreshKey]);
  useEffect(() => {
    window.addEventListener("ce-seat-changed", load);
    return () => window.removeEventListener("ce-seat-changed", load);
  }, [load]);

  if (!status) return null;

  const run = async (fn) => {
    setBusy(true);
    setError("");
    try {
      setStatus(await fn());
      setConfirming(false);
      onChanged?.();
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (status.registered && status.canRelease) {
    return (
      <div className="seat">
        {!confirming ? (
          <button type="button" className="seat__link" onClick={() => setConfirming(true)}>
            Can&apos;t make it? Release your seat
          </button>
        ) : (
          <div className="seat__confirm" role="group" aria-label="Release your seat">
            <p>Release your seat?{status.waitlistSize > 0 ? " The next student on the waitlist gets it." : " Someone else can register."} Your QR ticket stops working.</p>
            <div className="seat__actions">
              <button type="button" className="pc-btn pc-btn--danger" disabled={busy} onClick={() => run(() => releaseSeat(eventId))}>
                Release my seat
              </button>
              <button type="button" className="pc-btn pc-btn--ghost" onClick={() => setConfirming(false)}>Keep it</button>
            </div>
          </div>
        )}
        {error && <p className="pc__error" role="alert">{error}</p>}
      </div>
    );
  }

  if (!status.registered && status.full) {
    return (
      <div className="seat">
        {status.onWaitlist ? (
          <>
            <p className="seat__waiting" role="status">
              You&apos;re number {status.position} on the waitlist. If a seat opens, we&apos;ll register you and email your ticket.
            </p>
            <button type="button" className="seat__link" disabled={busy} onClick={() => run(() => leaveWaitlist(eventId))}>
              Leave the waitlist
            </button>
          </>
        ) : (
          <button type="button" className="save-btn save-btn--full" onClick={onJoinWaitlist}>
            Join the waitlist{status.waitlistSize > 0 ? ` (${status.waitlistSize} waiting)` : ""}
          </button>
        )}
        {error && <p className="pc__error" role="alert">{error}</p>}
      </div>
    );
  }
  return null;
}
