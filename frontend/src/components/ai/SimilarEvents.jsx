import { useEffect, useState } from "react";
import { fetchSimilarEvents } from "../../api/aiService";
import AiEventTile from "./AiEventTile";
import SparkleIcon from "./SparkleIcon";
import "../../styles/Ai.css";

/** "You might also like" on the event page. Public; renders nothing when there's nothing similar. */
export default function SimilarEvents({ eventId, limit = 4 }) {
  // Results are tagged with their event so a previous event's suggestions never show on the next page
  const [result, setResult] = useState({ eventId: null, items: [] });
  const items = result.eventId === eventId ? result.items : [];

  useEffect(() => {
    let cancelled = false;
    fetchSimilarEvents(eventId, limit)
      .then((data) => {
        if (!cancelled) setResult({ eventId, items: data });
      })
      .catch(() => {
        /* optional section — stay hidden on errors */
      });
    return () => {
      cancelled = true;
    };
  }, [eventId, limit]);

  if (items.length === 0) return null;

  return (
    <section className="ai-panel ai-panel--flush">
      <div className="ai-panel__header">
        <h2 className="ai-panel__title">
          <SparkleIcon /> You might also like
        </h2>
      </div>
      <div className="ai-tiles">
        {items.map((r) => (
          <AiEventTile key={r.event.id} event={r.event} reasons={r.reasons} />
        ))}
      </div>
    </section>
  );
}
