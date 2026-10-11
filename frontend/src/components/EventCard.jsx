import { useState, memo } from "react";
import { useNavigate } from "react-router-dom";
import { resolveImageUrl } from "../api/apiClient";
import "../styles/EventCard.css";

const CATEGORY_COLORS = {
  Technical: { bg: "rgba(139, 92, 246, 0.2)", text: "#a78bfa", icon: "⚙️" },
  Cultural: { bg: "rgba(236, 72, 153, 0.2)", text: "#f472b6", icon: "🎭" },
  Sports: { bg: "rgba(34, 197, 94, 0.2)", text: "#4ade80", icon: "🏃" },
  Workshop: { bg: "rgba(251, 146, 60, 0.2)", text: "#fb923c", icon: "🔧" },
  Hackathon: { bg: "rgba(56, 189, 248, 0.2)", text: "#38bdf8", icon: "💻" },
  Seminar: { bg: "rgba(167, 139, 250, 0.2)", text: "#a78bfa", icon: "🎓" },
  Default: { bg: "rgba(139, 92, 246, 0.2)", text: "#a78bfa", icon: "📅" },
};

const FALLBACK_IMAGE =
  "https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=400&h=250&fit=crop";

const buildImageUrl = (url) => resolveImageUrl(url) || FALLBACK_IMAGE;

const formatDate = (dateString) => {
  if (!dateString) return "TBA";
  const date = new Date(dateString);
  return date.toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
};

const formatTime = (dateString) => {
  if (!dateString) return "";
  const date = new Date(dateString);
  return date.toLocaleTimeString("en-US", {
    hour: "numeric",
    minute: "2-digit",
    hour12: true,
  });
};

const formatPrice = (cost) => {
  if (!cost || cost === "0" || cost.toLowerCase() === "free") {
    return { text: "Free", isFree: true };
  }
  return { text: `₹${cost}`, isFree: false };
};

const EventCard = memo(
  ({
    id,
    name,
    description,
    category,
    field,
    eventDate,
    venue,
    maxPeople,
    costOfRegistration,
    imageUrl,
    isHot = false,
    isTrending = false,
    
  }) => {
    const [imageLoaded, setImageLoaded] = useState(false);
    const [imageError, setImageError] = useState(false);
    const navigate = useNavigate();

    const categoryStyle = CATEGORY_COLORS[category] || CATEGORY_COLORS.Default;
    const price = formatPrice(costOfRegistration);
    const formattedDate = formatDate(eventDate);
    const formattedTime = formatTime(eventDate);

    const handleImageLoad = () => setImageLoaded(true);
    const handleImageError = () => {
      setImageError(true);
      setImageLoaded(true);
    };

    const handleRegister = (e) => {
      e.stopPropagation();
      navigate(`/events/${id}`, {
        state: {
          event: {
            id,
            name,
            description,
            category,
            field,
            eventDate,
            venue,
            maxPeople,
            costOfRegistration,
            imageUrl,
          },
        },
      });
    };

    // Build summary object to pass via navigation state so the details page
    // can render the header immediately without an extra /events/{id} call.
    const handleCardClick = () => {
      navigate(`/events/${id}`, {
        state: {
          event: {
            id,
            name,
            description,
            category,
            field,
            eventDate,
            venue,
            maxPeople,
            costOfRegistration,
            imageUrl,
          },
        },
      });
    };

    return (
      <article
        className="event-card"
        onClick={handleCardClick}
        style={{ cursor: "pointer" }}
      >
        <div className="event-card-image-container">
          {!imageLoaded && (
            <div className="event-card-image-skeleton">
              <div className="skeleton-shimmer"></div>
            </div>
          )}
          <img
            src={imageError ? FALLBACK_IMAGE : buildImageUrl(imageUrl)}
            alt={name}
            className={`event-card-image ${imageLoaded ? "loaded" : ""}`}
            onLoad={handleImageLoad}
            onError={handleImageError}
            loading="lazy"
          />
          {(isHot || isTrending) && (
            <span className={`event-badge ${isHot ? "hot" : "trending"}`}>
              {isHot ? "HOT" : "Trending"}
            </span>
          )}
        </div>

        <div className="event-card-content">
          <div className="event-card-header">
            <span
              className="event-category-tag"
              style={{
                backgroundColor: categoryStyle.bg,
                color: categoryStyle.text,
              }}
            >
              <span className="category-icon">{categoryStyle.icon}</span>
              {category || "Event"}
            </span>
            <span className="event-date">
              <svg
                className="date-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
              >
                <rect
                  x="3"
                  y="4"
                  width="18"
                  height="18"
                  rx="2"
                  strokeWidth="2"
                />
                <line x1="16" y1="2" x2="16" y2="6" strokeWidth="2" />
                <line x1="8" y1="2" x2="8" y2="6" strokeWidth="2" />
                <line x1="3" y1="10" x2="21" y2="10" strokeWidth="2" />
              </svg>
              {formattedDate}
            </span>
          </div>

          <h3 className="event-title">{name || "Untitled Event"}</h3>

          {description && <p className="event-description">{description}</p>}

          <div className="event-info-row">
            <div className="event-info-item">
              <svg
                className="info-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
              >
                <path
                  d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"
                  strokeWidth="2"
                />
                <circle cx="12" cy="10" r="3" strokeWidth="2" />
              </svg>
              <span>{venue || "TBA"}</span>
            </div>
            {formattedTime && (
              <div className="event-info-item">
                <svg
                  className="info-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                >
                  <circle cx="12" cy="12" r="10" strokeWidth="2" />
                  <polyline points="12,6 12,12 16,14" strokeWidth="2" />
                </svg>
                <span>{formattedTime}</span>
              </div>
            )}
          </div>

          <div className="event-meta-row">
            <div className="event-info-item">
              <svg
                className="info-icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
              >
                <path
                  d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"
                  strokeWidth="2"
                />
                <circle cx="9" cy="7" r="4" strokeWidth="2" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" strokeWidth="2" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" strokeWidth="2" />
              </svg>
              <span>{maxPeople ? `${maxPeople} Registered` : "Open"}</span>
            </div>
            {isTrending && (
              <div className="event-info-item trending-indicator">
                <svg
                  className="info-icon"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                >
                  <polyline
                    points="23,6 13.5,15.5 8.5,10.5 1,18"
                    strokeWidth="2"
                  />
                  <polyline points="17,6 23,6 23,12" strokeWidth="2" />
                </svg>
                <span>Trending</span>
              </div>
            )}
          </div>

          <div className="event-card-footer">
            <span className={`event-price ${price.isFree ? "free" : ""}`}>
              {price.text}
            </span>
            <button
              className="register-btn"
              onClick={handleRegister}
              aria-label={`Register for ${name}`}
            >
              Register Now
            </button>
          </div>
        </div>
      </article>
    );
  },
);

EventCard.displayName = "EventCard";

export default EventCard;
