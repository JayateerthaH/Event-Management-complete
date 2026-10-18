import { useEffect, useState } from "react";
import { getEventRecap } from "../../api/socialService";
import "../../styles/Social.css";

const pct = (v) => (v == null ? "—" : `${Math.round(v * 100)}%`);

/** Manage event, once it has started: show-up, feedback and followers it brought. */
export default function EventRecap({ eventId }) {
  const [recap, setRecap] = useState(null);

  useEffect(() => {
    getEventRecap(eventId).then(setRecap).catch(() => setRecap(null));
  }, [eventId]);

  if (!recap?.available) return null;
  return (
    <section className="recap-bar" aria-labelledby="recap-title">
      <h2 id="recap-title">Event recap</h2>
      <dl>
        <div><dt>Checked in</dt><dd>{recap.checkedIn} of {recap.registered}</dd></div>
        <div><dt>Show-up rate</dt><dd>{pct(recap.showUpRate)}</dd></div>
        <div><dt>Feedback</dt><dd>{recap.feedbackCount}{recap.averageRating != null ? ` · ${recap.averageRating.toFixed(1)}★` : ""}</dd></div>
        <div><dt>Followers</dt><dd>{recap.followers}{recap.newFollowersSinceEvent > 0 ? ` (+${recap.newFollowersSinceEvent} since)` : ""}</dd></div>
      </dl>
      <p className="social-note">When you mark the event completed, everyone who checked in gets a thank-you with links to give feedback and follow you.</p>
    </section>
  );
}
