import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getSavedEvents, loadSavedIds, useSavedIds } from "../../api/savedService";
import SaveButton from "./SaveButton";
import "../../styles/Engagement.css";

const fmt = (d) => (d ? new Date(d).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" }) : "Date to be announced");

/** Student dashboard: events they saved, soonest first; past ones drop off. */
export default function SavedEvents() {
  const [events, setEvents] = useState(null);
  const savedIds = useSavedIds();

  useEffect(() => {
    // The ids drive which rows stay listed (unsaving removes a row), so load them with the list
    Promise.all([getSavedEvents(), loadSavedIds()])
      .then(([list]) => {
        const now = Date.now();
        setEvents(list.filter((e) => !e.eventDate || new Date(e.eventDate).getTime() > now));
      })
      .catch(() => setEvents([]));
  }, []);

  if (!events) return null;
  const upcoming = events
    .filter((e) => savedIds.has(Number(e.id)))
    .sort((a, b) => new Date(a.eventDate || 8.64e15) - new Date(b.eventDate || 8.64e15));

  return (
    <section className="sd-card saved-events" aria-labelledby="saved-title">
      <div className="sd-card__header">
        <h2 className="sd-card__title" id="saved-title">Saved events</h2>
      </div>
      {upcoming.length === 0 ? (
        <p className="saved-events__empty">
          Tap the bookmark on any event to save it here. <Link to="/events">Browse events</Link>
        </p>
      ) : (
        <ul className="saved-events__list">
          {upcoming.map((e) => (
            <li key={e.id}>
              <Link to={`/events/${e.id}`} className="saved-events__name">{e.name}</Link>
              <span className="saved-events__meta">{fmt(e.eventDate)}{e.venue ? ` · ${e.venue}` : ""}{e.full ? " · Full" : ""}</span>
              <SaveButton eventId={e.id} variant="inline" />
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
