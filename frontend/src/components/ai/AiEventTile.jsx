import { useState } from "react";
import { useNavigate } from "react-router-dom";

const FALLBACK_IMAGE =
  "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=400&h=250&fit=crop";

// Same rules as EventCard: absolute URLs as-is, file names served from /images
const imageSrc = (url) => {
  if (!url) return FALLBACK_IMAGE;
  if (/^https?:\/\//.test(url) || url.startsWith("/")) return url;
  return `/images/${url}`;
};

const isFree = (cost) => !cost || /^0+(\.0+)?$/.test(String(cost).trim()) || String(cost).trim().toLowerCase() === "free";

const formatDate = (value) => {
  if (!value) return "TBA";
  const d = new Date(value);
  return Number.isNaN(d.getTime())
    ? "TBA"
    : d.toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" });
};

/**
 * Compact event card for AI results. `event` is the backend EventCard
 * ({ id, name, description, category, field, eventDate, venue, maxPeople, costOfRegistration, imageUrl });
 * `reasons` explain why it was suggested.
 */
export default function AiEventTile({ event, reasons = [] }) {
  const navigate = useNavigate();
  const [imgFailed, setImgFailed] = useState(false);
  const free = isFree(event.costOfRegistration);

  // Pass the summary along so the details page renders its header without waiting (like EventCard)
  const open = () =>
    navigate(`/events/${event.id}`, {
      state: {
        event: {
          id: event.id,
          name: event.name,
          description: event.description,
          category: event.category,
          field: event.field,
          eventDate: event.eventDate,
          venue: event.venue,
          maxPeople: event.maxPeople,
          costOfRegistration: event.costOfRegistration,
          imageUrl: event.imageUrl,
        },
      },
    });

  return (
    <button type="button" className="ai-tile" onClick={open} aria-label={`Open ${event.name}`}>
      <img
        className="ai-tile__image"
        src={imgFailed ? FALLBACK_IMAGE : imageSrc(event.imageUrl)}
        alt=""
        loading="lazy"
        onError={() => setImgFailed(true)}
      />
      <div className="ai-tile__body">
        <div className="ai-tile__meta">
          <span className="ai-tile__category">{event.category || "Event"}</span>
          <span>{formatDate(event.eventDate)}</span>
        </div>
        <h3 className="ai-tile__name">{event.name || "Untitled event"}</h3>
        <div className="ai-tile__meta">
          <span className="ai-tile__where">{event.venue || "Venue TBA"}</span>
          <span className={`ai-tile__price ${free ? "ai-tile__price--free" : ""}`}>
            {free ? "Free" : `₹${event.costOfRegistration}`}
          </span>
        </div>
        {reasons.length > 0 && (
          <ul className="ai-tile__reasons">
            {reasons.slice(0, 3).map((r) => (
              <li key={r}>{r}</li>
            ))}
          </ul>
        )}
      </div>
    </button>
  );
}
