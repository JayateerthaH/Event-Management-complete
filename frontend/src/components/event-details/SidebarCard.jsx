import { useEffect, useRef } from "react";
import { downloadIcs } from "../../utils/calendar";
import ResendQrButton from "../ResendQrButton";
import SaveButton from "../engagement/SaveButton";
import ShareButton from "../engagement/ShareButton";
import SeatControls from "./SeatControls";
import "../../styles/EventDetails.css";

const formatDate = (dateStr) => {
  if (!dateStr) return "";
  try {
    return new Date(dateStr).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
};

export default function SidebarCard({ event, details, onRegister, onJoinWaitlist, refetchEvent }) {
  // Poll for real-time registration count updates every 15 seconds
  const intervalRef = useRef(null);
  useEffect(() => {
    if (!refetchEvent) return;
    intervalRef.current = setInterval(() => {
      refetchEvent();
    }, 15000);
    return () => clearInterval(intervalRef.current);
  }, [refetchEvent]);

  if (!event) return null;

  const cost = event.costOfRegistration;
  const isFree =
    !cost ||
    cost === "0" ||
    cost === 0 ||
    String(cost).toLowerCase() === "free";
  const originalPrice = event.originalPrice || event.originalCost;

  const maxPeople = event.maxPeople ?? event.maxParticipants ?? event.capacity;
  const registered =
    event.registeredCount ??
    event.registrations ??
    event.currentRegistrations ??
    0;
  const spotsLeft = maxPeople ? Math.max(0, maxPeople - registered) : null;
  const isFull = spotsLeft !== null && spotsLeft <= 0;

  const deadline =
    event.registrationDeadline ||
    event.deadlineDate ||
    event.lastDateToRegister;
  const hasCertificate = event.certificate !== false;

  // Determine event status
  const now = new Date();
  const eventDate = event.eventDate || event.date;
  const isStarted = eventDate && new Date(eventDate) <= now;
  const isRegistered = event.isRegistered === true;

  return (
    <aside className="ed-sidebar">
      <div className="ed-sidebar__card">
        {/* ── Price Header ── */}
        <div className="ed-sidebar__price-section">
          <span className="ed-sidebar__price">
            {isFree ? "FREE" : `₹${cost}`}
          </span>
          {isFree && originalPrice && (
            <span className="ed-sidebar__original-price">₹{originalPrice}</span>
          )}
          {!isFree &&
            originalPrice &&
            String(originalPrice) !== String(cost) && (
              <span className="ed-sidebar__original-price">
                ₹{originalPrice}
              </span>
            )}
        </div>

        {/* ── Action Buttons ── */}
        <div className="ed-sidebar__actions">
          {isStarted ? (
            <button
              className="ed-sidebar__btn ed-sidebar__btn--started"
              disabled
            >
              Event Started!
            </button>
          ) : isRegistered ? (
            <button
              className="ed-sidebar__btn ed-sidebar__btn--primary"
              disabled
              style={{ opacity: 0.7, cursor: "not-allowed" }}
            >
              Already Registered ✓
            </button>
          ) : isFull ? (
            <button
              className="ed-sidebar__btn ed-sidebar__btn--closed"
              disabled
            >
              Registration Closed
            </button>
          ) : (
            <button
              className="ed-sidebar__btn ed-sidebar__btn--primary"
              onClick={onRegister}
            >
              Register Now
            </button>
          )}

          {!isStarted && (
            <SeatControls
              eventId={event.id ?? event.eventId}
              onJoinWaitlist={onJoinWaitlist}
              onChanged={refetchEvent}
              refreshKey={`${isRegistered}-${registered}`}
            />
          )}

          {isRegistered && !isStarted && (
            <ResendQrButton
              eventId={event.id ?? event.eventId}
              className="ed-sidebar__btn ed-sidebar__btn--outline"
            />
          )}

          <button
            className="ed-sidebar__btn ed-sidebar__btn--outline"
            onClick={() =>
              downloadIcs(event, {
                durationInHours: details?.durationInHours,
                pageUrl: window.location.href,
              })
            }
            disabled={isStarted}
          >
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
            Add to Calendar
          </button>

          {!isRegistered && !isStarted && <SaveButton eventId={event.id ?? event.eventId} variant="full" />}
          {!isStarted && <ShareButton event={event} />}
        </div>

        {/* ── Spots Left ── */}
        {spotsLeft !== null && (
          <p
            className={`ed-sidebar__spots ${isFull ? "ed-sidebar__spots--full" : ""}`}
          >
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
            {isFull
              ? "All spots filled!"
              : `Only ${spotsLeft.toLocaleString()} spots left!`}
          </p>
        )}

        {/* ── Divider ── */}
        <div className="ed-sidebar__divider" />

        {/* ── Info Rows ── */}
        <div className="ed-sidebar__info-list">
          {/* Registered */}
          {registered > 0 && (
            <div className="ed-sidebar__info-row">
              <div className="ed-sidebar__info-icon">
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
              </div>
              <div className="ed-sidebar__info-text">
                <span className="ed-sidebar__info-value">
                  {registered.toLocaleString()}
                </span>
                <span className="ed-sidebar__info-label">
                  Already Registered
                </span>
              </div>
            </div>
          )}

          {/* Registration Deadline */}
          {deadline && (
            <div className="ed-sidebar__info-row">
              <div className="ed-sidebar__info-icon">
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
              </div>
              <div className="ed-sidebar__info-text">
                <span className="ed-sidebar__info-value">
                  {formatDate(deadline)}
                </span>
                <span className="ed-sidebar__info-label">
                  Registration Deadline
                </span>
              </div>
            </div>
          )}

          {/* Certificate */}
          {hasCertificate && (
            <div className="ed-sidebar__info-row">
              <div className="ed-sidebar__info-icon">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                >
                  <circle cx="12" cy="8" r="6" />
                  <path d="M9 14l-2 8 5-3 5 3-2-8" />
                </svg>
              </div>
              <div className="ed-sidebar__info-text">
                <span className="ed-sidebar__info-value">Certificate</span>
                <span className="ed-sidebar__info-label">Upon Completion</span>
              </div>
            </div>
          )}
        </div>

        {/* ── Divider ── */}
        <div className="ed-sidebar__divider" />

        {/* ── Share This Event ── */}
        <div className="ed-sidebar__share">
          <h4 className="ed-sidebar__share-title">Share This Event</h4>
          <div className="ed-sidebar__share-icons">
            {/* Copy link */}
            <button
              className="ed-sidebar__share-btn"
              title="Copy link"
              onClick={() => {
                navigator.clipboard?.writeText(window.location.href);
              }}
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71" />
                <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71" />
              </svg>
            </button>
            {/* Twitter / X */}
            <a
              className="ed-sidebar__share-btn"
              title="Share on X"
              href={`https://twitter.com/intent/tweet?url=${encodeURIComponent(window.location.href)}&text=${encodeURIComponent(event.name || "")}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" />
              </svg>
            </a>
            {/* LinkedIn */}
            <a
              className="ed-sidebar__share-btn"
              title="Share on LinkedIn"
              href={`https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(window.location.href)}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433a2.062 2.062 0 01-2.063-2.065 2.064 2.064 0 112.063 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z" />
              </svg>
            </a>
            {/* WhatsApp */}
            <a
              className="ed-sidebar__share-btn"
              title="Share on WhatsApp"
              href={`https://wa.me/?text=${encodeURIComponent((event.name || "") + " " + window.location.href)}`}
              target="_blank"
              rel="noopener noreferrer"
            >
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z" />
              </svg>
            </a>
          </div>
        </div>
      </div>
    </aside>
  );
}
