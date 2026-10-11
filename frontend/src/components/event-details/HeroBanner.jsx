import { useState } from "react";
import { resolveImageUrl } from "../../api/apiClient";
import "../../styles/EventDetails.css";

const FALLBACK_IMAGE =
  "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=1200&h=500&fit=crop";

const buildImageUrl = (url) => resolveImageUrl(url) || FALLBACK_IMAGE;

const formatDate = (dateStr) => {
  if (!dateStr) return "";
  try {
    return new Date(dateStr).toLocaleDateString("en-US", {
      month: "long",
      day: "numeric",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
};

const formatTime = (timeStr) => {
  if (!timeStr) return "";
  try {
    // Handle "HH:mm" or "HH:mm:ss" strings
    const [h, m] = timeStr.split(":");
    const d = new Date();
    d.setHours(+h, +m);
    return d.toLocaleTimeString("en-US", {
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  } catch {
    return timeStr;
  }
};

export default function HeroBanner({ event }) {
  const [imgLoaded, setImgLoaded] = useState(false);
  const [imgError, setImgError] = useState(false);

  if (!event) return null;

  const imgSrc = imgError
    ? FALLBACK_IMAGE
    : buildImageUrl(event.imageUrl || event.image);

  return (
    <section className="ed-hero">
      {/* Background image */}
      <div className="ed-hero__bg">
        {!imgLoaded && <div className="ed-hero__skeleton" />}
        <img
          src={imgSrc}
          alt={event.name || "Event"}
          className={`ed-hero__img ${imgLoaded ? "loaded" : ""}`}
          onLoad={() => setImgLoaded(true)}
          onError={() => {
            setImgError(true);
            setImgLoaded(true);
          }}
        />
        <div className="ed-hero__overlay" />
      </div>

      {/* Content */}
      <div className="ed-hero__content">
        {event.category && (
          <span className="ed-hero__badge">{event.category}</span>
        )}
        <h1 className="ed-hero__title">
          {event.name || event.eventName || "Untitled Event"}
        </h1>

        <div className="ed-hero__meta">
          {(event.date || event.eventDate) && (
            <span className="ed-hero__meta-item">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
              {formatDate(event.date || event.eventDate)}
            </span>
          )}
          {(event.startTime || event.time) && (
            <span className="ed-hero__meta-item">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <circle cx="12" cy="12" r="10" />
                <polyline points="12 6 12 12 16 14" />
              </svg>
              {formatTime(event.startTime || event.time)}
              {event.endTime && ` - ${formatTime(event.endTime)}`}
            </span>
          )}
          {(event.venue || event.location) && (
            <span className="ed-hero__meta-item">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
              {event.venue || event.location}
            </span>
          )}
          {(event.registeredCount != null || event.registrations != null) && (
            <span className="ed-hero__meta-item ed-hero__meta-item--highlight">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 00-3-3.87" />
                <path d="M16 3.13a4 4 0 010 7.75" />
              </svg>
              {(
                event.registeredCount ??
                event.registrations ??
                0
              ).toLocaleString()}{" "}
              Registered
            </span>
          )}
        </div>
      </div>
    </section>
  );
}
