import { useState, useEffect, useCallback } from "react";
import useTabScroll from "../../../hooks/useTabScroll";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";
import Navbar from "../../../components/Navbar";
import QRCode from "qrcode";
import {
  fetchUserInfo,
  fetchPendingEvents,
  fetchPastEvents,
  fetchUserAnalytics,
  fetchCollaboratorEvents,
} from "../../../services/dashboardService";
import { fetchPendingFeedback } from "../../../services/feedbackService";
import {
  fetchEligibleCertificates,
  downloadCertificate,
} from "../../../services/certificateService";
import FeedbackForm from "../../../components/FeedbackForm";
import ResendQrButton from "../../../components/ResendQrButton";
import RecommendedEvents from "../../../components/ai/RecommendedEvents";
import InterestNudge from "../../../components/engagement/InterestNudge";
import SavedEvents from "../../../components/engagement/SavedEvents";
import Achievements from "../../../components/engagement/Achievements";
import SemesterGoal from "../../../components/engagement/SemesterGoal";
import PlanMyWeek from "../../../components/ai/PlanMyWeek";
import PushNudge from "../../../components/engagement/PushNudge";
import TicketSoon from "../../../components/engagement/TicketSoon";
import TrendingEvents from "../../../components/engagement/TrendingEvents";
import DepartmentLeaderboard from "../../../components/engagement/DepartmentLeaderboard";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Cell,
} from "recharts";
import "../../../styles/dashboard/StudentDashboard.css";
import "../../../styles/FeedbackCertificate.css";

const SIDEBAR_TABS = [
  { key: "overview", label: "Overview", icon: "grid" },
  { key: "upcoming", label: "Upcoming Events", icon: "calendar" },
  { key: "history", label: "Participation History", icon: "clock" },
  { key: "collaborator-events", label: "Collaborator Events", icon: "users" },
  { key: "feedback", label: "Pending Feedback", icon: "star" },
  { key: "certificates", label: "Certificates", icon: "award" },
  { key: "analytics", label: "Analytics", icon: "chart" },
];

const getInitials = (first, last, name) => {
  if (first && last) return (first[0] + last[0]).toUpperCase();
  if (name) {
    const parts = name.split(" ");
    if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
    return name.substring(0, 2).toUpperCase();
  }
  return "U";
};

const TabIcon = ({ type }) => {
  switch (type) {
    case "grid":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <rect x="3" y="3" width="7" height="7" rx="1" />
          <rect x="14" y="3" width="7" height="7" rx="1" />
          <rect x="3" y="14" width="7" height="7" rx="1" />
          <rect x="14" y="14" width="7" height="7" rx="1" />
        </svg>
      );
    case "calendar":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <rect x="3" y="4" width="18" height="18" rx="2" />
          <line x1="16" y1="2" x2="16" y2="6" />
          <line x1="8" y1="2" x2="8" y2="6" />
          <line x1="3" y1="10" x2="21" y2="10" />
        </svg>
      );
    case "clock":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <circle cx="12" cy="12" r="10" />
          <polyline points="12 6 12 12 16 14" />
        </svg>
      );
    case "award":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <circle cx="12" cy="8" r="6" />
          <path d="M15.477 12.89L17 22l-5-3-5 3 1.523-9.11" />
        </svg>
      );
    case "chart":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <line x1="18" y1="20" x2="18" y2="10" />
          <line x1="12" y1="20" x2="12" y2="4" />
          <line x1="6" y1="20" x2="6" y2="14" />
        </svg>
      );
    case "star":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
        </svg>
      );
    case "users":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
          <circle cx="9" cy="7" r="4" />
          <path d="M23 21v-2a4 4 0 00-3-3.87" />
          <path d="M16 3.13a4 4 0 010 7.75" />
        </svg>
      );
    default:
      return null;
  }
};

/* helper: parse "15 DEC" style date from a date string */
const formatDateBox = (dateStr) => {
  if (!dateStr) return { day: "--", month: "---" };
  try {
    const d = new Date(dateStr);
    if (isNaN(d)) return { day: "--", month: "---" };
    return {
      day: d.getDate(),
      month: d.toLocaleString("en", { month: "short" }).toUpperCase(),
    };
  } catch {
    return { day: "--", month: "---" };
  }
};

/* pick the first defined value from an object across multiple possible keys.
   Treats 0, false, etc. as valid — only skips undefined / null / empty-string. */
const pick = (obj, ...keys) => {
  for (const k of keys) {
    if (obj == null) continue;
    const v = obj[k];
    if (v !== undefined && v !== null && v !== "") return v;
  }
  return undefined; // caller decides fallback
};

/** Convenience: pick() but always return a display-safe value (0 → 0, missing → '--') */
const pickDisplay = (obj, ...keys) => {
  const v = pick(obj, ...keys);
  return v !== undefined ? v : "--";
};

/* normalise one analytics data point from /dashboard/user/analytics */
const normaliseAnalytics = (a) => {
  const month = pick(a, "month", "monthName", "period", "label", "name");
  const count = pick(
    a,
    "totalEvents",
    "attendanceCount",
    "count",
    "attendance",
    "value",
    "total",
    "events",
    "eventCount",
  );
  return {
    month: month ?? "N/A",
    attendanceCount: Number(count) || 0,
  };
};

/**
 * Normalise one event / registration object from /dashboard/events/pending or /past.
 *
 * Backend may return:
 *   A) Pure event objects  → { id, name, eventDate, venue, category }
 *   B) Registration objects with nested event → { id, eventsDto: { ... }, ... }
 *   C) Flattened join      → { registrationId, eventTitle, eventDate, ... }
 *
 * We first resolve the "inner event" (B/C) and then fall through all key variants.
 */
const normaliseEvent = (raw) => {
  if (!raw || typeof raw !== "object")
    return {
      id: "--",
      title: "--",
      date: "--",
      time: "--",
      location: "--",
      category: "--",
      status: "--",
    };

  // If there's a nested event object, merge its fields for easier picking
  const nested =
    raw.eventsDto ?? raw.event ?? raw.eventDetails ?? raw.eventData ?? null;
  const ev = nested ? { ...nested, ...raw } : raw;

  // Resolve date string first — we may need to extract time from it
  const dateStr = pickDisplay(
    ev,
    "date",
    "eventDate",
    "event_date",
    "startDate",
    "start_date",
    "scheduledDate",
    "scheduled_date",
  );

  // Try to get an explicit time field; if none, extract from the ISO date
  let timeVal = pick(
    ev,
    "time",
    "startTime",
    "start_time",
    "eventTime",
    "event_time",
  );
  if (timeVal == null && dateStr !== "--") {
    try {
      const d = new Date(dateStr);
      if (!isNaN(d)) {
        const h = d.getHours();
        const m = d.getMinutes();
        // Only show extracted time if it's not midnight (00:00 usually means date-only)
        if (h !== 0 || m !== 0) {
          const ampm = h >= 12 ? "PM" : "AM";
          const h12 = h % 12 || 12;
          timeVal = `${h12}:${String(m).padStart(2, "0")} ${ampm}`;
        }
      }
    } catch {
      /* keep undefined */
    }
  }
  if (timeVal == null) timeVal = "--";

  return {
    id: pickDisplay(ev, "id", "eventId", "event_id", "registrationId"),
    title: pickDisplay(
      ev,
      "title",
      "eventTitle",
      "event_title",
      "name",
      "eventName",
      "event_name",
    ),
    date: dateStr,
    time: timeVal,
    location: pickDisplay(
      ev,
      "location",
      "venue",
      "venueName",
      "venue_name",
      "place",
      "eventLocation",
      "event_location",
    ),
    category: pickDisplay(
      ev,
      "category",
      "eventCategory",
      "event_category",
      "type",
      "eventType",
      "event_type",
    ),
    status: pickDisplay(
      ev,
      "status",
      "registrationStatus",
      "registration_status",
      "eventStatus",
      "event_status",
    ),
    attendanceStatus: pickDisplay(
      ev,
      "attendanceStatus",
      "attendance_status",
      "attendanceState",
      "attended",
    ),
  };
};

/* Format date string to readable: "Oct 15, 2024" */
const formatDateReadable = (dateStr) => {
  if (!dateStr || dateStr === "--") return "--";
  try {
    const d = new Date(dateStr);
    if (isNaN(d)) return dateStr;
    return d.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  } catch {
    return dateStr;
  }
};

export default function StudentDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  // ?tab=feedback etc. opens a tab directly (links from notifications)
  const [activeTab, setActiveTab] = useState(() => {
    const tab = new URLSearchParams(window.location.search).get("tab");
    return SIDEBAR_TABS.some((t) => t.key === tab) ? tab : "overview";
  });
  useTabScroll(activeTab, ".sd-sidebar__nav");
  const [badgeCount, setBadgeCount] = useState(null);
  const handleBadgeSummary = useCallback((s) => setBadgeCount(s.earned), []);
  const [userInfo, setUserInfo] = useState(null);
  const [pendingEvents, setPendingEvents] = useState([]);
  const [pastEvents, setPastEvents] = useState([]);
  const [analytics, setAnalytics] = useState([]);

  // Feedback & Certificates state
  const [collaboratorEvents, setCollaboratorEvents] = useState([]);

  // Feedback & Certificates state
  const [pendingFeedback, setPendingFeedback] = useState([]);
  const [feedbackModal, setFeedbackModal] = useState(null); // { eventId, eventTitle }
  const [eligibleCerts, setEligibleCerts] = useState([]);
  const [downloadingId, setDownloadingId] = useState(null);
  const [certError, setCertError] = useState({});

  // QR Code modal state
  const [qrModal, setQrModal] = useState(null); // { eventTitle }
  const [qrDataUrl, setQrDataUrl] = useState(null);

  // eslint-disable-next-line no-unused-vars
  const openQrModal = useCallback(
    async (eventTitle) => {
      const rid =
        userInfo?.randomId ?? userInfo?.random_id ?? userInfo?.qrId ?? "";
      if (!rid) {
        setQrModal({ eventTitle });
        setQrDataUrl(null);
        return;
      }
      try {
        const url = await QRCode.toDataURL(String(rid), {
          width: 280,
          margin: 2,
        });
        setQrDataUrl(url);
      } catch {
        setQrDataUrl(null);
      }
      setQrModal({ eventTitle });
    },
    [userInfo],
  );

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const [
        infoRes,
        pendingRes,
        pastRes,
        analyticsRes,
        fbRes,
        certRes,
        collabRes,
      ] = await Promise.allSettled([
        fetchUserInfo(),
        fetchPendingEvents(),
        fetchPastEvents(),
        fetchUserAnalytics(),
        fetchPendingFeedback(),
        fetchEligibleCertificates(),
        fetchCollaboratorEvents(),
      ]);
      if (cancelled) return;
      if (infoRes.status === "fulfilled") setUserInfo(infoRes.value);
      if (pendingRes.status === "fulfilled") setPendingEvents(pendingRes.value);
      if (pastRes.status === "fulfilled") setPastEvents(pastRes.value);
      if (analyticsRes.status === "fulfilled") setAnalytics(analyticsRes.value);
      if (fbRes.status === "fulfilled") setPendingFeedback(fbRes.value);
      if (certRes.status === "fulfilled") setEligibleCerts(certRes.value);
      if (collabRes.status === "fulfilled")
        setCollaboratorEvents(collabRes.value);
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // Profile — prefer API data, fall back to auth context
  const firstName =
    userInfo?.firstName ||
    userInfo?.first_name ||
    userInfo?.fname ||
    user?.firstName ||
    user?.name?.split(" ")?.[0] ||
    "Student";
  const lastName =
    userInfo?.lastName ||
    userInfo?.last_name ||
    userInfo?.lname ||
    user?.lastName ||
    user?.name?.split(" ")?.slice(1).join(" ") ||
    "";
  const fullName =
    userInfo?.name ||
    userInfo?.fullName ||
    userInfo?.full_name ||
    user?.name ||
    `${firstName} ${lastName}`.trim() ||
    "Student";
  const department =
    userInfo?.department ||
    userInfo?.dept ||
    userInfo?.branch ||
    userInfo?.departmentName ||
    user?.department ||
    "--";
  const yearOfStudy =
    userInfo?.yearOfStudy ||
    userInfo?.year ||
    userInfo?.year_of_study ||
    userInfo?.studyYear ||
    user?.yearOfStudy ||
    "--";
  const initials = getInitials(firstName, lastName, fullName);

  // Stats from /dashboard/user — multi-key fallback via pick()
  // Use pick() (not pickDisplay) so we can explicitly default to 0 instead of '--'
  const eventsAttended =
    pick(
      userInfo,
      "eventsAttended",
      "events_attended",
      "totalEvents",
      "eventCount",
      "attended",
    ) ?? 0;
  const certificatesEarned =
    pick(
      userInfo,
      "certificatesEarned",
      "certificates_earned",
      "totalCertificates",
      "certCount",
      "certificates",
    ) ?? eligibleCerts.length;
  const hoursParticipated =
    pick(
      userInfo,
      "hoursParticipated",
      "hours_participated",
      "totalHours",
      "hours",
      "totalHoursParticipated",
    ) ?? 0;
  const achievements =
    pick(
      userInfo,
      "achievements",
      "totalAchievements",
      "achievementCount",
      "points",
      "badges",
    ) ?? 0;

  // Certificates from userInfo (if included) or empty — multi-key fallback
  // certList / recentCertificates replaced by eligibleCerts from API

  // Aliases for clarity
  const upcomingEvents = pendingEvents;
  const history = pastEvents;

  /* ── Tab content renderer ── */
  const renderContent = () => {
    switch (activeTab) {
      /* ─── OVERVIEW ─── */
      case "overview":
        return (
          <>
            {/* Metrics */}
            <section className="sd-metrics">
              <div className="sd-metric-card">
                <span className="sd-metric-card__icon sd-metric-card__icon--blue">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <rect x="3" y="4" width="18" height="18" rx="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </svg>
                </span>
                <div className="sd-metric-card__info">
                  <span className="sd-metric-card__value">
                    {eventsAttended}
                  </span>
                  <span className="sd-metric-card__label">Events Attended</span>
                </div>
              </div>
              <div className="sd-metric-card">
                <span className="sd-metric-card__icon sd-metric-card__icon--red">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <circle cx="12" cy="8" r="6" />
                    <path d="M15.477 12.89L17 22l-5-3-5 3 1.523-9.11" />
                  </svg>
                </span>
                <div className="sd-metric-card__info">
                  <span className="sd-metric-card__value">
                    {certificatesEarned}
                  </span>
                  <span className="sd-metric-card__label">
                    Certificates Earned
                  </span>
                </div>
              </div>
              <div className="sd-metric-card">
                <span className="sd-metric-card__icon sd-metric-card__icon--green">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <circle cx="12" cy="12" r="10" />
                    <polyline points="12 6 12 12 16 14" />
                  </svg>
                </span>
                <div className="sd-metric-card__info">
                  <span className="sd-metric-card__value">
                    {hoursParticipated}
                  </span>
                  <span className="sd-metric-card__label">
                    Hours Participated
                  </span>
                </div>
              </div>
              <div className="sd-metric-card">
                <span className="sd-metric-card__icon sd-metric-card__icon--purple">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                  </svg>
                </span>
                <div className="sd-metric-card__info">
                  <span className="sd-metric-card__value">{badgeCount ?? achievements}</span>
                  <span className="sd-metric-card__label">Badges earned</span>
                </div>
              </div>
            </section>

            <TicketSoon />
            {/* Upcoming Registered Events */}
            <section className="sd-card">
              <div className="sd-card__header">
                <h2 className="sd-card__title">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <rect x="3" y="4" width="18" height="18" rx="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </svg>
                  Your upcoming events
                </h2>
                <Link to="/tickets" className="sd-tickets-link" title="Your QR tickets, also without internet">My tickets</Link>
                <button
                  className="sd-link-btn"
                  onClick={() => setActiveTab("upcoming")}
                >
                  View All
                </button>
              </div>
              {upcomingEvents.length > 0 ? (
                <div className="sd-events-list">
                  {upcomingEvents.slice(0, 5).map((raw, i) => {
                    const ev = normaliseEvent(raw);
                    const { day, month } = formatDateBox(ev.date);
                    return (
                      <div
                        key={ev.id !== "--" ? ev.id : i}
                        className="sd-event-row"
                      >
                        <div className="sd-event-row__date">
                          <span className="sd-event-row__day">{day}</span>
                          <span className="sd-event-row__month">{month}</span>
                        </div>
                        <div className="sd-event-row__info">
                          <span className="sd-event-row__title">
                            {ev.title}
                          </span>
                          <span className="sd-event-row__meta">
                            <svg
                              viewBox="0 0 24 24"
                              fill="none"
                              stroke="currentColor"
                              strokeWidth="2"
                            >
                              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                              <circle cx="12" cy="10" r="3" />
                            </svg>
                            {ev.location}
                            {ev.time !== "--" && (
                              <>
                                &nbsp;&bull;&nbsp;
                                <svg
                                  viewBox="0 0 24 24"
                                  fill="none"
                                  stroke="currentColor"
                                  strokeWidth="2"
                                >
                                  <circle cx="12" cy="12" r="10" />
                                  <polyline points="12 6 12 12 16 14" />
                                </svg>
                                {ev.time}
                              </>
                            )}
                          </span>
                          {ev.category !== "--" && (
                            <span
                              className={`sd-tag sd-tag--${ev.category.toLowerCase().replace(/\s+/g, "-")}`}
                            >
                              {ev.category}
                            </span>
                          )}
                        </div>
                        <div className="sd-event-row__actions">
                          <ResendQrButton eventId={ev.id} className="sd-btn sd-btn--outline" />
                          <button
                            className="sd-btn sd-btn--primary"
                            onClick={() =>
                              navigate(`/events/${ev.id}`, {
                                state: { event: raw },
                              })
                            }
                          >
                            View Details
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <p className="sd-empty">No upcoming registered events.</p>
              )}
            </section>

            <InterestNudge />
            <PushNudge />
            <PlanMyWeek />
            <RecommendedEvents limit={6} />
            <TrendingEvents limit={3} />
            <SavedEvents />
            <SemesterGoal />
            <Achievements onSummary={handleBadgeSummary} />
            <DepartmentLeaderboard />

            {/* Recent Certificates */}
            <section className="sd-card">
              <div className="sd-card__header">
                <h2 className="sd-card__title">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <circle cx="12" cy="8" r="6" />
                    <path d="M15.477 12.89L17 22l-5-3-5 3 1.523-9.11" />
                  </svg>
                  Recent Certificates
                </h2>
                <button
                  className="sd-link-btn"
                  onClick={() => setActiveTab("certificates")}
                >
                  View All
                </button>
              </div>
              {eligibleCerts.length > 0 ? (
                <div className="sd-cert-grid">
                  {eligibleCerts.slice(0, 4).map((c, i) => (
                    <div key={c.eventId ?? i} className="sd-cert-item">
                      <div className="sd-cert-item__icon">🏆</div>
                      <span className="sd-cert-item__title">
                        {c.eventTitle ?? "--"}
                      </span>
                      <span className="sd-cert-item__date">
                        {c.eventDate
                          ? new Date(c.eventDate).toLocaleDateString()
                          : "--"}
                      </span>
                      <button
                        className="sd-btn sd-btn--download"
                        onClick={async () => {
                          try {
                            await downloadCertificate(c.eventId);
                          } catch (e) {
                            console.error("Certificate download error:", e);
                            // Assume failure means feedback is pending or other eligibility issue
                            // Redirect user to feedback form immediately
                            setFeedbackModal({
                              eventId: c.eventId,
                              eventTitle: c.eventTitle ?? `Event #${c.eventId}`,
                            });
                          }
                        }}
                      >
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
                          <polyline points="7 10 12 15 17 10" />
                          <line x1="12" y1="15" x2="12" y2="3" />
                        </svg>
                        Download
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="sd-empty">No certificates earned yet.</p>
              )}
            </section>
          </>
        );

      /* ─── UPCOMING EVENTS ─── */
      case "upcoming":
        return (
          <section className="sd-card">
            <h2 className="sd-card__title">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <rect x="3" y="4" width="18" height="18" rx="2" />
                <line x1="16" y1="2" x2="16" y2="6" />
                <line x1="8" y1="2" x2="8" y2="6" />
                <line x1="3" y1="10" x2="21" y2="10" />
              </svg>
              All Upcoming Events
            </h2>
            {upcomingEvents.length > 0 ? (
              <div className="sd-events-list">
                {upcomingEvents.map((raw, i) => {
                  const ev = normaliseEvent(raw);
                  const { day, month } = formatDateBox(ev.date);
                  return (
                    <div
                      key={ev.id !== "--" ? ev.id : i}
                      className="sd-event-row"
                    >
                      <div className="sd-event-row__date">
                        <span className="sd-event-row__day">{day}</span>
                        <span className="sd-event-row__month">{month}</span>
                      </div>
                      <div className="sd-event-row__info">
                        <span className="sd-event-row__title">{ev.title}</span>
                        <span className="sd-event-row__meta">
                          <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                          >
                            <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                            <circle cx="12" cy="10" r="3" />
                          </svg>
                          {ev.location}
                          {ev.time !== "--" && (
                            <>
                              &nbsp;&bull;&nbsp;
                              <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                              >
                                <circle cx="12" cy="12" r="10" />
                                <polyline points="12 6 12 12 16 14" />
                              </svg>
                              {ev.time}
                            </>
                          )}
                        </span>
                        {ev.category !== "--" && (
                          <span
                            className={`sd-tag sd-tag--${ev.category.toLowerCase().replace(/\s+/g, "-")}`}
                          >
                            {ev.category}
                          </span>
                        )}
                      </div>
                      <div className="sd-event-row__actions">
                        <ResendQrButton eventId={ev.id} className="sd-btn sd-btn--outline" />
                        <button
                          className="sd-btn sd-btn--primary"
                          onClick={() =>
                            navigate(`/events/${ev.id}`, {
                              state: { event: raw },
                            })
                          }
                        >
                          View Details
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="sd-empty">No upcoming events.</p>
            )}
          </section>
        );

      /* ─── PARTICIPATION HISTORY ─── */
      case "history":
        return (
          <section className="sd-card">
            <div className="sd-card__header">
              <h2 className="sd-card__title">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
                Participation History
              </h2>
            </div>
            {history.length > 0 ? (
              <div className="sd-table-wrap">
                <table className="sd-table">
                  <thead>
                    <tr>
                      <th>Event Name</th>
                      <th>Date</th>
                      <th>Category</th>
                      <th>Status</th>
                      <th>Certificate</th>
                    </tr>
                  </thead>
                  <tbody>
                    {history.map((raw, i) => {
                      const ev = normaliseEvent(raw);
                      const displayStatus =
                        ev.attendanceStatus !== "--"
                          ? ev.attendanceStatus
                          : ev.status;
                      const statusClass = (displayStatus || "")
                        .toLowerCase()
                        .replace(/\s+/g, "-");
                      return (
                        <tr key={ev.id !== "--" ? ev.id : i}>
                          <td className="sd-table__name">{ev.title}</td>
                          <td>{formatDateReadable(ev.date)}</td>
                          <td>
                            {ev.category !== "--" && (
                              <span
                                className={`sd-tag sd-tag--${ev.category.toLowerCase().replace(/\s+/g, "-")}`}
                              >
                                {ev.category}
                              </span>
                            )}
                          </td>
                          <td>
                            <span
                              className={`sd-badge sd-badge--${statusClass}`}
                            >
                              {displayStatus !== "--" ? displayStatus : "N/A"}
                            </span>
                          </td>
                          <td>
                            <button
                              className="sd-btn sd-btn--icon-outline"
                              title="Download Certificate"
                            >
                              <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                              >
                                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
                                <polyline points="7 10 12 15 17 10" />
                                <line x1="12" y1="15" x2="12" y2="3" />
                              </svg>
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="sd-empty">No participation history yet.</p>
            )}
          </section>
        );

      /* ─── COLLABORATOR EVENTS ─── */
      case "collaborator-events":
        return (
          <section className="sd-card">
            <div className="sd-card__header">
              <h2 className="sd-card__title">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                  <circle cx="9" cy="7" r="4" />
                  <path d="M23 21v-2a4 4 0 00-3-3.87" />
                  <path d="M16 3.13a4 4 0 010 7.75" />
                </svg>
                Collaborator Events
              </h2>
            </div>
            {collaboratorEvents.length > 0 ? (
              <div className="sd-events-list">
                {collaboratorEvents.map((raw, i) => {
                  const ev = normaliseEvent(raw);
                  // prefer event-level id over registration id for the manage route
                  const eventId =
                    pick(raw, "eventId", "event_id", "id") ??
                    pick(raw.eventsDto ?? raw.event ?? {}, "id", "eventId") ??
                    ev.id;
                  const { day, month } = formatDateBox(ev.date);
                  return (
                    <div key={eventId ?? i} className="sd-event-item">
                      <div className="sd-event-item__date-box">
                        <span className="sd-event-item__day">{day}</span>
                        <span className="sd-event-item__month">{month}</span>
                      </div>
                      <div className="sd-event-item__info">
                        <h3 className="sd-event-item__title">{ev.title}</h3>
                        <div className="sd-event-item__meta">
                          <span>{ev.time}</span>
                          <span>·</span>
                          <span>{ev.location}</span>
                        </div>
                      </div>
                      <div className="sd-event-item__actions">
                        <span
                          className={`sd-badge sd-badge--${(ev.status || "pending").toLowerCase().replace(/\s+/g, "-")}`}
                        >
                          {ev.status}
                        </span>
                        <button
                          className="sd-btn sd-btn--primary"
                          onClick={() =>
                            navigate(`/scan/${eventId}`, {
                              state: { eventName: ev.title },
                            })
                          }
                          title="Scan QR to mark attendance for this event"
                        >
                          <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            style={{
                              width: "14px",
                              height: "14px",
                              marginRight: "4px",
                            }}
                          >
                            <rect x="3" y="3" width="7" height="7" />
                            <rect x="14" y="3" width="7" height="7" />
                            <rect x="3" y="14" width="7" height="7" />
                          </svg>
                          Scan
                        </button>
                        <button
                          className="sd-btn sd-btn--accent"
                          onClick={() =>
                            navigate(`/dashboard/manage-event/${eventId}`)
                          }
                        >
                          Manage Event
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="sd-empty">
                You have not been added as a collaborator to any events yet.
              </p>
            )}
          </section>
        );

      /* ─── PENDING FEEDBACK ─── */
      case "feedback":
        return (
          <section className="sd-card">
            <h2 className="sd-card__title">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
              >
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
              Pending Feedback
            </h2>
            {pendingFeedback.length > 0 ? (
              <div className="fb-cert-grid">
                {pendingFeedback.map((item, idx) => {
                  const evId = item.eventId ?? item.id;
                  return (
                    <div key={evId ?? idx} className="fb-cert-card">
                      <div className="fb-cert-card__icon">📝</div>
                      <h3 className="fb-cert-card__title">
                        {item.eventTitle ?? item.url ?? `Event #${evId}`}
                      </h3>
                      <button
                        className="sd-btn sd-btn--accent"
                        onClick={() =>
                          setFeedbackModal({
                            eventId: evId,
                            eventTitle: item.eventTitle ?? `Event #${evId}`,
                          })
                        }
                      >
                        Give Feedback
                      </button>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="sd-empty">
                No pending feedback requests — you&apos;re all caught up! 🎉
              </p>
            )}

            {/* Feedback Form Modal */}
            {feedbackModal && (
              <FeedbackForm
                eventId={feedbackModal.eventId}
                eventTitle={feedbackModal.eventTitle}
                onClose={(submitted) => {
                  setFeedbackModal(null);
                  if (submitted) {
                    // remove from pending list
                    setPendingFeedback((prev) =>
                      prev.filter(
                        (f) => (f.eventId ?? f.id) !== feedbackModal.eventId,
                      ),
                    );
                  }
                }}
              />
            )}
          </section>
        );

      /* ─── CERTIFICATES ─── */
      case "certificates":
        return (
          <section className="sd-card">
            <h2 className="sd-card__title">Eligible Certificates</h2>
            {eligibleCerts.length > 0 ? (
              <div className="fb-cert-grid">
                {eligibleCerts.map((c, i) => {
                  const evId = c.eventId;
                  const isDownloading = downloadingId === evId;
                  const errMsg = certError[evId];
                  return (
                    <div key={evId ?? i} className="fb-cert-card">
                      <div className="fb-cert-card__icon">🏆</div>
                      <h3 className="fb-cert-card__title">
                        {c.eventTitle ?? "--"}
                      </h3>
                      <span className="fb-cert-card__date">
                        {c.eventDate
                          ? new Date(c.eventDate).toLocaleDateString()
                          : "--"}
                      </span>
                      {errMsg && (
                        <p
                          className="fb-alert fb-alert--error"
                          style={{ fontSize: "0.78rem", margin: "0.4rem 0" }}
                        >
                          {errMsg}
                        </p>
                      )}
                      <button
                        className="sd-btn sd-btn--download"
                        disabled={isDownloading}
                        onClick={async () => {
                          setDownloadingId(evId);
                          setCertError((prev) => ({ ...prev, [evId]: null }));
                          try {
                            await downloadCertificate(evId);
                          } catch (err) {
                            const msg =
                              err?.message ||
                              err?.response?.data?.message ||
                              "Download failed";
                            
                            // Broaden check: If error mentions feedback OR it's a client error (likely 400/403/404), redirect
                            // User request: Direct to feedback when exception is thrown.
                            const isFeedbackError = 
                                msg.toLowerCase().includes("feedback") || 
                                msg.toLowerCase().includes("feed back") ||
                                (err?.response?.status >= 400 && err?.response?.status < 500);

                            if (isFeedbackError) {
                              setCertError((prev) => ({
                                ...prev,
                                [evId]:
                                  "Feedback required or eligibility check failed. Redirecting to feedback...",
                              }));
                              setTimeout(() => {
                                setFeedbackModal({
                                  eventId: evId,
                                  eventTitle: c.eventTitle ?? `Event #${evId}`,
                                });
                              }, 1500);
                            } else {
                              setCertError((prev) => ({
                                ...prev,
                                [evId]: msg,
                              }));
                            }
                          } finally {
                            setDownloadingId(null);
                          }
                        }}
                      >
                        {isDownloading ? (
                          <span className="fb-spinner" />
                        ) : (
                          <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                          >
                            <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
                            <polyline points="7 10 12 15 17 10" />
                            <line x1="12" y1="15" x2="12" y2="3" />
                          </svg>
                        )}
                        {isDownloading ? "Downloading…" : "Download"}
                      </button>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="sd-empty">
                No certificates available yet. Complete events and submit
                feedback to unlock certificates.
              </p>
            )}

            {/* Feedback Form Modal (triggered when cert download fails due to missing feedback) */}
            {feedbackModal && (
              <FeedbackForm
                eventId={feedbackModal.eventId}
                eventTitle={feedbackModal.eventTitle}
                onClose={(submitted) => {
                  setFeedbackModal(null);
                  if (submitted) {
                    setCertError((prev) => ({
                      ...prev,
                      [feedbackModal.eventId]: null,
                    }));
                  }
                }}
              />
            )}
          </section>
        );

      /* ─── ANALYTICS ─── */
      case "analytics":
        return (
          <section className="sd-card">
            <div className="sd-card__header">
              <h2 className="sd-card__title">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                >
                  <line x1="18" y1="20" x2="18" y2="10" />
                  <line x1="12" y1="20" x2="12" y2="4" />
                  <line x1="6" y1="20" x2="6" y2="14" />
                </svg>
                Participation Analytics
              </h2>
            </div>
            {analytics.length > 0 ? (
              (() => {
                const analyticsData = analytics.map(normaliseAnalytics);
                const CustomBarLabel = ({ x, y, width, value }) => (
                  <text
                    x={x + width / 2}
                    y={y - 8}
                    fill="#a78bfa"
                    fontSize={12}
                    fontWeight={700}
                    textAnchor="middle"
                  >
                    {value}
                  </text>
                );
                return (
                  <>
                    <div className="sd-chart-wrap">
                      <ResponsiveContainer width="100%" height={320}>
                        <BarChart
                          data={analyticsData}
                          margin={{ top: 25, right: 20, left: 0, bottom: 0 }}
                        >
                          <CartesianGrid
                            strokeDasharray="3 3"
                            stroke="rgba(148,163,184,0.12)"
                            vertical={true}
                          />
                          <XAxis
                            dataKey="month"
                            stroke="#64748b"
                            fontSize={12}
                            tickLine={false}
                            axisLine={{ stroke: "rgba(148,163,184,0.15)" }}
                          />
                          <YAxis
                            stroke="#64748b"
                            fontSize={12}
                            tickLine={false}
                            axisLine={{ stroke: "rgba(148,163,184,0.15)" }}
                            allowDecimals={false}
                            width={30}
                            label={{
                              value: "Events Attended",
                              angle: -90,
                              position: "insideLeft",
                              fill: "#94a3b8",
                              fontSize: 12,
                              dx: -5,
                            }}
                          />
                          <Tooltip
                            cursor={{ fill: "rgba(139,92,246,0.08)" }}
                            contentStyle={{
                              background: "rgba(20, 12, 60, 0.95)",
                              border: "1px solid rgba(139,92,246,0.4)",
                              borderRadius: 10,
                              color: "#e2e8f0",
                              fontSize: 13,
                              padding: "8px 14px",
                              boxShadow: "0 8px 24px rgba(0,0,0,0.4)",
                            }}
                            labelStyle={{
                              color: "#a78bfa",
                              fontWeight: 700,
                              marginBottom: 4,
                            }}
                            formatter={(val) => [val, "Events Attended"]}
                          />
                          <Bar
                            dataKey="attendanceCount"
                            fill="#8b8cf6"
                            radius={[4, 4, 0, 0]}
                            barSize={48}
                            label={<CustomBarLabel />}
                          >
                            {analyticsData.map((entry, idx) => (
                              <Cell key={`bar-cell-${idx}`} fill="#7c7ef6" />
                            ))}
                          </Bar>
                        </BarChart>
                      </ResponsiveContainer>
                    </div>
                    <p className="sd-analytics__caption">
                      Your participation trends over the last{" "}
                      {analyticsData.length} months
                    </p>
                  </>
                );
              })()
            ) : (
              <p className="sd-empty">No analytics data available yet.</p>
            )}
          </section>
        );

      default:
        return null;
    }
  };

  return (
    <div className="sd-page">
      <Navbar />

      <div className="sd-layout">
        {/* ── Left Sidebar ── */}
        <aside className="sd-sidebar">
          <div className="sd-sidebar__profile">
            <div className="sd-sidebar__avatar">{initials}</div>
            <h3 className="sd-sidebar__name">{fullName}</h3>
            <p className="sd-sidebar__dept">
              {[department, yearOfStudy].filter((v) => v && v !== "--").join(", ")}
            </p>
          </div>

          <nav className="sd-sidebar__nav">
            {SIDEBAR_TABS.map((tab) => (
              <button
                key={tab.key}
                className={`sd-sidebar__tab ${activeTab === tab.key ? "sd-sidebar__tab--active" : ""}`}
                onClick={() => setActiveTab(tab.key)}
              >
                <TabIcon type={tab.icon} />
                {tab.label}
              </button>
            ))}
          </nav>
        </aside>

        {/* ── Main Content ── */}
        <main className="sd-content" data-tab={activeTab}>
          {/* Welcome bar */}
          <section className="sd-welcome">
            <div className="sd-welcome__text">
              <h1 className="sd-welcome__title">
                Welcome back, {firstName}! 👋
              </h1>
              <p className="sd-welcome__sub">
                Here&apos;s what&apos;s happening with your events today
              </p>
            </div>
            <button
              className="sd-btn sd-btn--accent"
              onClick={() => navigate("/events")}
            >
              Browse Events
            </button>
          </section>

          <div key={activeTab} className="tab-fade">{renderContent()}</div>
        </main>
      </div>

      {/* ── QR Code Modal ── */}
      {qrModal && (
        <div className="sd-qr-overlay" onClick={() => setQrModal(null)}>
          <div className="sd-qr-modal" onClick={(e) => e.stopPropagation()}>
            <button
              className="sd-qr-modal__close"
              onClick={() => setQrModal(null)}
            >
              &times;
            </button>
            <h3 className="sd-qr-modal__title">Your QR Code</h3>
            {qrModal.eventTitle && (
              <p className="sd-qr-modal__event">{qrModal.eventTitle}</p>
            )}
            {qrDataUrl ? (
              <img src={qrDataUrl} alt="QR Code" className="sd-qr-modal__img" />
            ) : (
              <p className="sd-qr-modal__error">
                QR code not available. Please check your registration.
              </p>
            )}
            <p className="sd-qr-modal__hint">
              Show this QR code to the organizer for attendance
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
