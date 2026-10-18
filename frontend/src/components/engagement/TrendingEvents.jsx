import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import api from "../../api/apiClient";
import AiEventTile from "../ai/AiEventTile";
import "../../styles/Ai.css";

/** Home and dashboard: upcoming events students are saving and registering for this week. */
export default function TrendingEvents({ limit = 6 }) {
  const [events, setEvents] = useState([]);

  useEffect(() => {
    api.get("/events/trending", { params: { limit } })
      .then((r) => setEvents(Array.isArray(r.data) ? r.data : []))
      .catch(() => setEvents([]));
  }, [limit]);

  if (events.length === 0) return null;
  return (
    <section className="ai-panel trending" aria-labelledby="trending-title">
      <div className="ai-panel__header">
        <div>
          <h2 className="ai-panel__title" id="trending-title">🔥 Trending this week</h2>
          <p className="ai-panel__subtitle">What students are saving and signing up for right now.</p>
        </div>
        <Link to="/events" className="ai-btn ai-btn--text">See all</Link>
      </div>
      <div className="ai-tiles">
        {events.map((e) => (
          <AiEventTile key={e.id} event={e}
            reasons={[e.registeredCount > 0 ? `${e.registeredCount} going` : "New this week",
              ...(e.maxPeople && e.registeredCount >= e.maxPeople * 0.8 ? [`Only ${e.maxPeople - e.registeredCount} seats left`] : [])]} />
        ))}
      </div>
    </section>
  );
}
