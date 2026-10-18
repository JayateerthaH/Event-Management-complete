import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/apiClient";
import "../../styles/Engagement.css";

const when = (d) => {
  const t = new Date(d);
  const today = new Date().toDateString() === t.toDateString();
  return `${today ? "Today" : t.toLocaleDateString(undefined, { weekday: "long" })}, ${t.toLocaleTimeString(undefined, { hour: "numeric", minute: "2-digit" })}`;
};

/** Student dashboard: an event in the next 24 hours, with its QR ticket one tap away. */
export default function TicketSoon() {
  const [soon, setSoon] = useState([]);

  useEffect(() => {
    api.get("/attendance/my-tickets").then(({ data }) => {
      const now = Date.now();
      setSoon((Array.isArray(data) ? data : [])
        .filter((t) => !t.attended && t.eventDate && new Date(t.eventDate).getTime() > now - 3 * 3600000 && new Date(t.eventDate).getTime() < now + 24 * 3600000)
        .sort((a, b) => new Date(a.eventDate) - new Date(b.eventDate)));
    }).catch(() => setSoon([]));
  }, []);

  if (soon.length === 0) return null;
  return (
    <section className="ticket-soon" aria-label="Coming up">
      {soon.map((t) => (
        <div key={`${t.eventId}-${t.code}`} className="ticket-soon__row">
          <span className="ticket-soon__icon" aria-hidden="true">🎟️</span>
          <p><b>{t.eventName}</b><br /><span>{when(t.eventDate)}{t.venue ? ` · ${t.venue}` : ""}</span></p>
          <Link className="pc-btn pc-btn--primary" to={`/tickets#ticket-${t.eventId}`}>Open ticket</Link>
        </div>
      ))}
    </section>
  );
}
