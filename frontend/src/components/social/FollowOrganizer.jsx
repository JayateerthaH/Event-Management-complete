import { useEffect, useState } from "react";
import { apiErrorMessage } from "../../api/apiClient";
import { followOrganizer, getOrganizerForEvent, unfollowOrganizer } from "../../api/socialService";
import "../../styles/Social.css";

/** Event page: who runs this event, and a Follow button to hear about their next ones. */
export default function FollowOrganizer({ eventId }) {
  const [org, setOrg] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => { getOrganizerForEvent(eventId).then(setOrg).catch(() => setOrg(null)); }, [eventId]);

  if (!org) return null;
  const toggle = async () => {
    setBusy(true);
    setError("");
    try {
      setOrg(await (org.following ? unfollowOrganizer(org.organizerId) : followOrganizer(org.organizerId)));
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <section className="follow-org" aria-label="Organizer">
      <div className="follow-org__avatar" aria-hidden="true">{org.name.charAt(0).toUpperCase()}</div>
      <div className="follow-org__body">
        <span className="eb__label">Organized by</span>
        <p className="follow-org__name">{org.name}</p>
        <p className="follow-org__meta">
          {org.followers} follower{org.followers === 1 ? "" : "s"} · {org.upcomingCount} upcoming event{org.upcomingCount === 1 ? "" : "s"}
        </p>
        {error && <p className="pc__error" role="alert">{error}</p>}
      </div>
      <button type="button" className={`pc-btn ${org.following ? "pc-btn--ghost" : "pc-btn--primary"}`} disabled={busy}
        aria-pressed={org.following} onClick={toggle}>
        {org.following ? "Following" : "Follow"}
      </button>
    </section>
  );
}
