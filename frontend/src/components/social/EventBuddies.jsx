import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiErrorMessage } from "../../api/apiClient";
import { getEventBuddies, optInToEvent, optOutOfEvent } from "../../api/socialService";
import PersonCard from "./PersonCard";
import "../../styles/Social.css";

/**
 * Event page panel: friends who are going, and (for registered students who opt in) other attendees who are
 * also open to meeting people. Nobody appears here unless they opted in for this event.
 */
export default function EventBuddies({ eventId, isRegistered }) {
  const [state, setState] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  const load = useCallback(() => {
    getEventBuddies(eventId).then(setState).catch(() => setState(null));
  }, [eventId]);

  useEffect(load, [load, isRegistered]);

  const toggle = async () => {
    setBusy(true);
    setError("");
    try {
      setState(await (state.optedIn ? optOutOfEvent(eventId) : optInToEvent(eventId)));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (!state) return null;
  const friends = state.friendsGoing || [];
  if (!state.registered && friends.length === 0) return null;

  return (
    <section className="ed-section eb" aria-labelledby="eb-title">
      <h2 className="ed-section__title" id="eb-title">Event buddies</h2>

      {friends.length > 0 && (
        <div className="eb__friends">
          <span className="eb__label">Friends going</span>
          <ul className="eb__chips">
            {friends.map((f) => (
              <li key={f.userId}><span aria-hidden="true">{f.name.charAt(0).toUpperCase()}</span>{f.name}</li>
            ))}
          </ul>
        </div>
      )}

      {state.registered && (
        <div className="eb__optin">
          <div>
            <p className="eb__optin-title">
              {state.optedIn ? "You're open to meeting others here" : "Going alone? Meet other attendees"}
            </p>
            <p className="eb__optin-text">
              {state.optedIn
                ? "Other attendees who opted in can see your name, department, year and interests. Never your email or phone."
                : `Only attendees who also opt in see each other.${state.optedInCount > 0 ? ` ${state.optedInCount} already ${state.optedInCount === 1 ? "has" : "have"}.` : ""}`}
            </p>
            {!state.profileReady && !state.optedIn && (
              <p className="eb__optin-text">
                First <Link to="/friends?tab=settings">set the name other students will see</Link>.
              </p>
            )}
          </div>
          <button
            type="button"
            className={`pc-btn ${state.optedIn ? "pc-btn--ghost" : "pc-btn--primary"}`}
            disabled={busy || (!state.profileReady && !state.optedIn)}
            onClick={toggle}
          >
            {state.optedIn ? "Stop showing me" : "I'm open to meeting people"}
          </button>
        </div>
      )}
      {error && <p className="pc__error" role="alert">{error}</p>}

      {state.optedIn && (
        state.people.length > 0 ? (
          <div className="pc-grid">
            {state.people.map((p) => (
              <PersonCard key={p.userId} person={p} eventId={Number(eventId)} onChange={load} />
            ))}
          </div>
        ) : (
          <p className="eb__empty">Nobody else has opted in yet. We'll show them here when they do.</p>
        )
      )}
    </section>
  );
}
