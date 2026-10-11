import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchRecommendations, aiErrorMessage } from "../../api/aiService";
import AiEventTile from "./AiEventTile";
import SparkleIcon from "./SparkleIcon";
import "../../styles/Ai.css";

/**
 * "Recommended for you": ranked by the student's interests, department, past registrations,
 * popularity and date. Each suggestion shows why it was picked.
 */
export default function RecommendedEvents({ limit = 6, className = "" }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await fetchRecommendations(limit));
    } catch (err) {
      setError(aiErrorMessage(err, "Couldn't load recommendations."));
    } finally {
      setLoading(false);
    }
  }, [limit]);

  useEffect(() => {
    load();
  }, [load]);

  // Only generic reasons means we know little about the student yet
  const personalised = items.some((r) =>
    r.reasons?.some((reason) => /interest|registered for|department/i.test(reason)),
  );

  return (
    <section className={`ai-panel ${className}`}>
      <div className="ai-panel__header">
        <div>
          <h2 className="ai-panel__title">
            <SparkleIcon /> Recommended for you
          </h2>
          <p className="ai-panel__subtitle">Picked from your interests, department and the events you've joined.</p>
        </div>
        <button type="button" className="ai-btn ai-btn--text" onClick={load} disabled={loading}>
          {loading ? <span className="ai-spinner" /> : "Refresh"}
        </button>
      </div>

      {error ? (
        <div className="ai-error">{error}</div>
      ) : loading ? (
        <div className="ai-tiles">
          {[0, 1, 2].map((i) => (
            <div key={i} className="ai-skeleton" />
          ))}
        </div>
      ) : items.length === 0 ? (
        <p className="ai-note">
          No upcoming events to suggest right now. <Link to="/events">Browse all events</Link>.
        </p>
      ) : (
        <div className="ai-stack">
          <div className="ai-tiles">
            {items.map((r) => (
              <AiEventTile key={r.event.id} event={r.event} reasons={r.reasons} />
            ))}
          </div>
          {!personalised && (
            <p className="ai-note">
              Tip: <Link to="/profile">add your interests and department</Link> to get more personal suggestions.
            </p>
          )}
        </div>
      )}
    </section>
  );
}
