import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";
import Navbar from "../../../components/Navbar";
import {
  fetchAllOrganizerData,
  createEvent,
  fetchOrganizerEvents,
  deleteEvent,
  fetchRegistrationAnalytics,
  createAnnouncement,
  uploadCertificateTemplate,
} from "../../../services/organizerDashboardService";
import { completeEvent } from "../../../services/collaboratorService";
import {
  requestFeedback,
  fetchEventFeedback,
} from "../../../services/feedbackService";
import AddCollaboratorModal from "../../../components/AddCollaboratorModal";
import { addFaq } from "../../../api/eventDetailsService";
import DescriptionHelper from "../../../components/ai/DescriptionHelper";
import AnnouncementHelper from "../../../components/ai/AnnouncementHelper";
import FeedbackInsights from "../../../components/ai/FeedbackInsights";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import "../../../styles/dashboard/OrganizerDashboard.css";
import "../../../styles/FeedbackCertificate.css";

/* ---------------------------------------------------------------------------
   SIDEBAR TABS — matches Organizer Panel screenshot
   --------------------------------------------------------------------------- */
const SIDEBAR_TABS = [
  { key: "overview", label: "Overview", icon: "grid" },
  { key: "create", label: "Create Event", icon: "plus" },
  { key: "manage", label: "Manage Events", icon: "settings" },
  { key: "attendees", label: "Attendees", icon: "users" },
  { key: "feedback", label: "Feedback", icon: "star" },
  { key: "analytics", label: "Analytics", icon: "chart" },
  { key: "certificates", label: "Certificates", icon: "award" },
  { key: "announcements", label: "Announcements", icon: "bell" },
];

/* ---------------------------------------------------------------------------
   HELPERS
   --------------------------------------------------------------------------- */
const getInitials = (name) => {
  if (!name) return "O";
  const parts = name.trim().split(" ");
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return name.substring(0, 2).toUpperCase();
};

const pick = (obj, ...keys) => {
  for (const k of keys) {
    const v = obj?.[k];
    if (v !== undefined && v !== null && v !== "") return v;
  }
  return null;
};

const toArray = (val) => {
  if (Array.isArray(val)) return val;
  if (val && typeof val === "object") {
    for (const k of [
      "data",
      "content",
      "events",
      "result",
      "items",
      "records",
      "list",
    ]) {
      if (Array.isArray(val[k])) return val[k];
    }
  }
  return [];
};

const parseAmount = (value) => {
  if (typeof value === "number") return Number.isFinite(value) ? value : 0;
  if (typeof value !== "string") return 0;
  const normalized = value.replace(/[^\d.-]/g, "").trim();
  if (!normalized) return 0;
  const parsed = Number(normalized);
  return Number.isFinite(parsed) ? parsed : 0;
};

const calculateRevenueFromEvents = (events) =>
  events.reduce((sum, eventItem) => {
    const registrations = Number(eventItem?.registrations ?? 0);
    const cost = parseAmount(eventItem?.cost);
    if (!Number.isFinite(registrations) || registrations <= 0 || cost <= 0) {
      return sum;
    }
    return sum + registrations * cost;
  }, 0);

// Completed = organizer marked it complete (stats recorded). A past event that isn't completed yet is
// "Ended" and still offers "Mark Complete" (date alone used to hide the button once the event was over).
const eventStatus = (ev, dateVal) => {
  if (ev?.isCompleted) return "Completed";
  const byDate = deriveStatus(dateVal);
  return byDate === "Completed" ? "Ended" : byDate;
};

const deriveStatus = (dateVal) => {
  if (!dateVal) return "Upcoming";
  try {
    const eventDate = new Date(dateVal);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return eventDate < today ? "Completed" : "Upcoming";
  } catch {
    return "Upcoming";
  }
};

const normaliseEvent = (ev) => {
  const date =
    pick(
      ev,
      "eventDate",
      "date",
      "event_date",
      "startDate",
      "start_date",
      "scheduledDate",
    ) ?? "--";
  const regs = Array.isArray(ev?.registers)
    ? ev.registers.length
    : (pick(
        ev,
        "registrations",
        "registrationCount",
        "registration_count",
        "totalRegistrations",
      ) ?? 0);
  return {
    id: pick(ev, "id", "eventId", "event_id"),
    title:
      pick(ev, "name", "title", "eventTitle", "event_title", "eventName") ??
      "--",
    date,
    registrations: regs,
    status: eventStatus(ev, date),
    category:
      pick(ev, "category", "eventCategory", "event_category", "type") ?? "--",
    venue: pick(ev, "venue", "location", "venueName") ?? "--",
    maxPeople: pick(ev, "maxPeople", "maxParticipants", "capacity") ?? "--",
    cost:
      pick(ev, "costOfRegistration", "cost", "price", "registrationCost") ??
      "Free",
  };
};

const normaliseAttendee = (a) => ({
  id: pick(a, "id", "userId", "attendeeId"),
  name: pick(a, "name", "userName", "fullName", "attendeeName") ?? "--",
  email: pick(a, "email", "userEmail", "attendeeEmail") ?? "--",
  event: pick(a, "event", "eventTitle", "eventName", "eventId") ?? "--",
  date:
    pick(a, "date", "registrationDate", "joinedDate", "attendedDate") ?? "--",
});

const normaliseTrend = (t) => ({
  month: pick(t, "month", "monthName", "period", "label") ?? "--",
  registrations:
    pick(t, "registrations", "count", "registrationCount", "total", "value") ??
    0,
});

const formatDateReadable = (dateStr) => {
  if (!dateStr || dateStr === "--") return "--";
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

/* ---------------------------------------------------------------------------
   ICON COMPONENT
   --------------------------------------------------------------------------- */
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
    case "plus":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="16" />
          <line x1="8" y1="12" x2="16" y2="12" />
        </svg>
      );
    case "settings":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <line x1="4" y1="21" x2="4" y2="14" />
          <line x1="4" y1="10" x2="4" y2="3" />
          <line x1="12" y1="21" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12" y2="3" />
          <line x1="20" y1="21" x2="20" y2="16" />
          <line x1="20" y1="12" x2="20" y2="3" />
          <line x1="1" y1="14" x2="7" y2="14" />
          <line x1="9" y1="8" x2="15" y2="8" />
          <line x1="17" y1="16" x2="23" y2="16" />
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
    case "award":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <circle cx="12" cy="8" r="7" />
          <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88" />
        </svg>
      );
    case "bell":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
          <path d="M13.73 21a2 2 0 01-3.46 0" />
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
    default:
      return null;
  }
};

/* ---------------------------------------------------------------------------
   EMPTY FORM STATE
   --------------------------------------------------------------------------- */
const EMPTY_FORM = {
  name: "",
  description: "",
  category: "",
  field: "",
  eventDate: "",
  venue: "",
  maxPeople: "",
  costOfRegistration: "",
};

const CATEGORY_OPTIONS = [
  "Technical",
  "Cultural",
  "Workshop",
  "Sports",
  "Seminar",
  "Bhakti",
  "Music",
  "Hackathon",
  "Social",
  "Academic",
  "Other",
];

/* ---------------------------------------------------------------------------
   MAIN COMPONENT
   --------------------------------------------------------------------------- */
export default function OrganizerDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("overview");

  // -- Overview data ---------------------------------------------------------
  const [data, setData] = useState(null);

  // -- Manage Events ---------------------------------------------------------
  const [myEvents, setMyEvents] = useState([]);
  const [eventsLoading, setEventsLoading] = useState(false);

  // -- Create Event form -----------------------------------------------------
  const [form, setForm] = useState({ ...EMPTY_FORM });
  const [creating, setCreating] = useState(false);
  const [createMsg, setCreateMsg] = useState(null); // { type: 'success'|'error', text }
  // AI-drafted FAQs the organizer chose to add once the event exists; key remounts the helper on reset
  const [pendingFaqs, setPendingFaqs] = useState([]);
  const [aiHelperKey, setAiHelperKey] = useState(0);

  // -- Deleting --------------------------------------------------------------
  const [deletingId, setDeletingId] = useState(null);

  // -- Analytics -------------------------------------------------------------
  const [analyticsData, setAnalyticsData] = useState([]);
  const [analyticsLoading, setAnalyticsLoading] = useState(false);

  // -- Announcements ---------------------------------------------------------
  const [annEventId, setAnnEventId] = useState("");
  const [annTitle, setAnnTitle] = useState("");
  const [annMessage, setAnnMessage] = useState("");
  const [annSending, setAnnSending] = useState(false);
  const [annMsg, setAnnMsg] = useState(null);

  // -- Certificates ----------------------------------------------------------
  const [certEventId, setCertEventId] = useState("");
  const [certFile, setCertFile] = useState(null);
  const [certUploading, setCertUploading] = useState(false);
  const [certMsg, setCertMsg] = useState(null);

  // -- Collaborators ---------------------------------------------------------
  const [collabModal, setCollabModal] = useState(null); // eventId or null
  const [completeMsg, setCompleteMsg] = useState(null);
  const [completingId, setCompletingId] = useState(null);

  // -- Feedback ---------------------------------------------------------------
  const [fbEventId, setFbEventId] = useState("");
  const [fbData, setFbData] = useState([]);
  const [fbLoadedFor, setFbLoadedFor] = useState(""); // event whose feedback is in fbData
  const [fbLoading, setFbLoading] = useState(false);
  const [fbReqLoading, setFbReqLoading] = useState(false);
  const [fbMsg, setFbMsg] = useState(null);

  // -- Fetch overview data ---------------------------------------------------
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await fetchAllOrganizerData();
        if (!cancelled) setData(res);
      } catch {
        /* renders with -- placeholders */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // -- Fetch organizer events when manage tab active -------------------------
  const loadMyEvents = async () => {
    setEventsLoading(true);
    try {
      const list = await fetchOrganizerEvents();
      setMyEvents(list);
    } catch {
      setMyEvents([]);
    } finally {
      setEventsLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === "manage" || activeTab === "overview") loadMyEvents();
  }, [activeTab]);

  // -- Fetch analytics when tab active ---------------------------------------
  useEffect(() => {
    if (activeTab !== "analytics") return;

    let cancelled = false;
    (async () => {
      setAnalyticsLoading(true);
      try {
        const res = await fetchRegistrationAnalytics();
        if (!cancelled) setAnalyticsData(res.map(normaliseTrend));
      } catch (err) {
        console.error("[Analytics] fetch error:", err);
        if (!cancelled) setAnalyticsData([]);
      } finally {
        if (!cancelled) setAnalyticsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeTab, user]);

  // -- Load events list for dropdowns ----------------------------------------
  useEffect(() => {
    if (
      (activeTab === "announcements" ||
        activeTab === "certificates" ||
        activeTab === "collaborators" ||
        activeTab === "feedback") &&
      myEvents.length === 0
    ) {
      loadMyEvents();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]);

  // -- Profile ---------------------------------------------------------------
  const fullName = user?.name || user?.firstName || "Organizer";
  const initials = getInitials(fullName);
  const managedEvents = myEvents.map(normaliseEvent);

  // -- Stats -----------------------------------------------------------------
  const totalEvents =
    pick(
      data,
      "totalEvents",
      "total_events",
      "eventCount",
      "totalEventCount",
    ) ??
    (myEvents.filter((ev) => ev.isCompleted).length || 0);
  const totalRegistrationsFromEvents = managedEvents.reduce(
    (sum, eventItem) => sum + Number(eventItem?.registrations ?? 0),
    0,
  );
  const totalRegistrations = Math.max(
    Number(
      pick(
        data,
        "totalRegistration",
        "totalRegistrations",
        "total_registrations",
        "registrationCount",
        "totalRegistrationCount",
      ) ?? 0,
    ),
    totalRegistrationsFromEvents,
  );
  const attendanceCount =
    pick(
      data,
      "totalAttended",
      "attendanceCount",
      "attendance",
      "totalAttendance",
      "attendence",
      "total_attendance",
    ) ?? 0;
  const revenueFromData = parseAmount(
    pick(
      data,
      "revenue",
      "totalRevenue",
      "total_revenue",
      "platformRevenue",
      "earnings",
    ),
  );
  const revenueFromEvents = calculateRevenueFromEvents(managedEvents);
  const revenue = Math.max(revenueFromData, revenueFromEvents);

  const recentAttendees = toArray(
    data?.recentAttendees ??
      data?.attendees ??
      data?.recent_attendees ??
      data?.attendeeList,
  ).map(normaliseAttendee);

  // -- Create event handler --------------------------------------------------
  const handleCreateEvent = async (e) => {
    e.preventDefault();
    setCreating(true);
    setCreateMsg(null);

    const normalizedField = form.field.trim();
    const eventsDto = {
      name: form.name.trim(),
      description: form.description.trim(),
      category: form.category,
      field: normalizedField,
      eventField: normalizedField,
      eventDate: form.eventDate,
      venue: form.venue.trim(),
      maxPeople: form.maxPeople ? Number(form.maxPeople) : null,
      costOfRegistration: form.costOfRegistration.trim() || "0",
    };

    try {
      const created = await createEvent(eventsDto);
      let faqNote = "";
      const createdId = created?.id ?? created?.eventId;
      if (pendingFaqs.length > 0 && createdId) {
        const saved = await Promise.allSettled(
          pendingFaqs.map((f) => addFaq(createdId, { questions: f.question, answers: f.answer })),
        );
        const ok = saved.filter((r) => r.status === "fulfilled").length;
        faqNote =
          ok === pendingFaqs.length
            ? ` ${ok} FAQs added.`
            : ` ${ok} of ${pendingFaqs.length} FAQs added; add the rest from the event page.`;
      }
      setCreateMsg({
        type: "success",
        text: `Event created successfully! It will be visible after admin approval.${faqNote}`,
      });
      setForm({ ...EMPTY_FORM });
      setPendingFaqs([]);
      setAiHelperKey((k) => k + 1);
      loadMyEvents();
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        "Failed to create event";
      setCreateMsg({ type: "error", text: msg });
    } finally {
      setCreating(false);
    }
  };

  // -- Delete event handler --------------------------------------------------
  const handleDelete = async (eventId) => {
    if (!window.confirm("Are you sure you want to delete this event?")) return;
    setDeletingId(eventId);
    try {
      await deleteEvent(eventId);
      setMyEvents((prev) =>
        prev.filter((ev) => (ev.id ?? ev.eventId) !== eventId),
      );
    } catch (err) {
      alert(err?.response?.data?.message || "Failed to delete event");
    } finally {
      setDeletingId(null);
    }
  };

  // -- Form field handler ----------------------------------------------------
  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  // -- Send announcement handler ---------------------------------------------
  const handleSendAnnouncement = async (e) => {
    e.preventDefault();
    if (!annEventId) {
      setAnnMsg({ type: "error", text: "Please select an event." });
      return;
    }
    if (!annTitle.trim()) {
      setAnnMsg({ type: "error", text: "Title is required." });
      return;
    }
    if (!annMessage.trim()) {
      setAnnMsg({ type: "error", text: "Message is required." });
      return;
    }
    setAnnSending(true);
    setAnnMsg(null);
    try {
      await createAnnouncement(annEventId, {
        title: annTitle.trim(),
        message: annMessage.trim(),
      });
      setAnnMsg({ type: "success", text: "Announcement sent successfully!" });
      setAnnTitle("");
      setAnnMessage("");
    } catch (err) {
      const d = err?.response?.data;
      const msg =
        typeof d === "string"
          ? d
          : d?.message ||
            d?.error ||
            err?.message ||
            "Failed to send announcement";
      setAnnMsg({
        type: "error",
        text: typeof msg === "string" ? msg : JSON.stringify(msg),
      });
    } finally {
      setAnnSending(false);
    }
  };

  // -- Upload certificate handler --------------------------------------------
  const handleUploadCertificate = async (e) => {
    e.preventDefault();
    if (!certEventId) {
      setCertMsg({ type: "error", text: "Please select an event." });
      return;
    }
    if (!certFile) {
      setCertMsg({
        type: "error",
        text: "Please select a certificate template file.",
      });
      return;
    }
    setCertUploading(true);
    setCertMsg(null);
    try {
      const fd = new FormData();
      fd.append("file", certFile);
      fd.append("eventId", certEventId);
      await uploadCertificateTemplate(fd);
      setCertMsg({
        type: "success",
        text: "Certificate template uploaded successfully!",
      });
      setCertFile(null);
    } catch (err) {
      const d = err?.response?.data;
      const msg =
        typeof d === "string"
          ? d
          : d?.message ||
            d?.error ||
            err?.message ||
            "Failed to upload certificate";
      setCertMsg({
        type: "error",
        text: typeof msg === "string" ? msg : JSON.stringify(msg),
      });
    } finally {
      setCertUploading(false);
    }
  };

  // -- Complete event handler ------------------------------------------------
  const handleCompleteEvent = async (eventId) => {
    if (!eventId) {
      setCompleteMsg({ type: "error", text: "Invalid event ID." });
      return;
    }
    if (
      !window.confirm(
        "Mark this event as completed? This will distribute badges and updates.",
      )
    )
      return;

    setCompletingId(eventId);
    setCompleteMsg(null);
    try {
      console.log(`Marking event ${eventId} as complete...`);
      await completeEvent(eventId);
      setCompleteMsg({
        type: "success",
        text: "Event completed successfully!",
      });
      loadMyEvents();
    } catch (err) {
      console.error("Complete event error:", err);
      const d = err?.response?.data;
      const msg =
        typeof d === "string"
          ? d
          : d?.message ||
            d?.error ||
            err?.message ||
            "Failed to complete event";
      setCompleteMsg({ type: "error", text: msg });
    } finally {
      setCompletingId(null);
    }
  };

  // -- Feedback: request feedback for event -----------------------------------
  const handleRequestFeedback = async () => {
    if (!fbEventId) {
      setFbMsg({ type: "error", text: "Please select an event." });
      return;
    }
    setFbReqLoading(true);
    setFbMsg(null);
    try {
      const feedbackUrl = `${window.location.origin}/dashboard`;
      await requestFeedback(Number(fbEventId), feedbackUrl);
      setFbMsg({
        type: "success",
        text: "Feedback request sent to all attendees!",
      });
    } catch (err) {
      const status = err?.response?.status;
      const d = err?.response?.data;
      const msg =
        typeof d === "string" ? d : d?.message || d?.error || err?.message;
      if (status === 403) {
        setFbMsg({
          type: "error",
          text: "Only the event creator can request feedback.",
        });
      } else {
        setFbMsg({ type: "error", text: msg || "Failed to request feedback." });
      }
    } finally {
      setFbReqLoading(false);
    }
  };

  // -- Feedback: load feedback for selected event ----------------------------
  const handleLoadFeedback = async () => {
    if (!fbEventId) return;
    setFbLoading(true);
    setFbMsg(null);
    try {
      const data = await fetchEventFeedback(fbEventId);
      setFbData(data);
      setFbLoadedFor(fbEventId);
    } catch (err) {
      const status = err?.response?.status;
      if (status === 403) {
        setFbMsg({
          type: "error",
          text: "Only the event creator can view feedback.",
        });
      } else {
        setFbMsg({ type: "error", text: "Failed to load feedback." });
      }
      setFbData([]);
    } finally {
      setFbLoading(false);
    }
  };

  /* ---------------------------------------------------------------------------
     RENDER TAB CONTENT
     --------------------------------------------------------------------------- */
  const renderContent = () => {
    switch (activeTab) {
      /* --- OVERVIEW ---------------------------------------------------------- */
      case "overview":
        return (
          <>
            {/* Stat cards */}
            <section className="od-metrics">
              <div className="od-metric-card">
                <span className="od-metric-card__icon od-metric-card__icon--purple">
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
                <div className="od-metric-card__info">
                  <span className="od-metric-card__value">{totalEvents}</span>
                  <span className="od-metric-card__label">Total Events</span>
                </div>
              </div>
              <div className="od-metric-card">
                <span className="od-metric-card__icon od-metric-card__icon--red">
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
                </span>
                <div className="od-metric-card__info">
                  <span className="od-metric-card__value">
                    {totalRegistrations}
                  </span>
                  <span className="od-metric-card__label">
                    Total Registrations
                  </span>
                </div>
              </div>
              <div className="od-metric-card">
                <span className="od-metric-card__icon od-metric-card__icon--green">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <path d="M9 11l3 3L22 4" />
                    <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                  </svg>
                </span>
                <div className="od-metric-card__info">
                  <span className="od-metric-card__value">
                    {attendanceCount}
                  </span>
                  <span className="od-metric-card__label">Attended</span>
                </div>
              </div>
              <div className="od-metric-card">
                <span className="od-metric-card__icon od-metric-card__icon--blue">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <text
                      x="3"
                      y="18"
                      fontSize="16"
                      fontWeight="700"
                      fill="currentColor"
                      stroke="none"
                    >
                      ₹
                    </text>
                  </svg>
                </span>
                <div className="od-metric-card__info">
                  <span className="od-metric-card__value">
                    {`₹${revenue.toLocaleString("en-IN")}`}
                  </span>
                  <span className="od-metric-card__label">Revenue</span>
                </div>
              </div>
            </section>

            {/* Quick-view Manage Events table */}
            <section className="od-card">
              <div className="od-card__header">
                <h2 className="od-card__title">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    style={{ width: 20, height: 20 }}
                  >
                    <line x1="4" y1="21" x2="4" y2="14" />
                    <line x1="4" y1="10" x2="4" y2="3" />
                    <line x1="12" y1="21" x2="12" y2="12" />
                    <line x1="12" y1="8" x2="12" y2="3" />
                    <line x1="20" y1="21" x2="20" y2="16" />
                    <line x1="20" y1="12" x2="20" y2="3" />
                    <line x1="1" y1="14" x2="7" y2="14" />
                    <line x1="9" y1="8" x2="15" y2="8" />
                    <line x1="17" y1="16" x2="23" y2="16" />
                  </svg>
                  Manage Events
                </h2>
                <div
                  style={{
                    display: "flex",
                    gap: "0.75rem",
                    alignItems: "center",
                  }}
                >
                  <button
                    className="od-link-btn"
                    onClick={() => setActiveTab("manage")}
                  >
                    View All
                  </button>
                  <button
                    className="od-btn od-btn--primary"
                    onClick={() => setActiveTab("create")}
                  >
                    + New Event
                  </button>
                </div>
              </div>
              {managedEvents.length > 0 ? (
                <div className="od-table-wrap">
                  <table className="od-table">
                    <thead>
                      <tr>
                        <th>Event Name</th>
                        <th>Date</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {managedEvents.slice(0, 5).map((ev, i) => (
                        <tr key={ev.id ?? i}>
                          <td className="od-table__name">{ev.title}</td>
                          <td>{formatDateReadable(ev.date)}</td>
                          <td>
                            <span
                              className={`od-badge od-badge--${ev.status.toLowerCase().replace(/\s+/g, "-")}`}
                            >
                              {ev.status}
                            </span>
                          </td>
                          <td>
                            <div className="od-actions">
                              {ev.status.toLowerCase() !== "completed" ? (
                                <>
                                  <button
                                    className="od-action-btn"
                                    title="Manage"
                                    onClick={() =>
                                      navigate(
                                        `/dashboard/manage-event/${ev.id}`,
                                        { state: { event: myEvents[i] } },
                                      )
                                    }
                                  >
                                    <svg
                                      viewBox="0 0 24 24"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                    >
                                      <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                                      <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                                    </svg>
                                  </button>
                                  <button
                                    className="od-action-btn"
                                    title="Scan Attendance"
                                    onClick={() => navigate(`/scan/${ev.id}`)}
                                  >
                                    <svg
                                      viewBox="0 0 24 24"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                    >
                                      <rect x="3" y="3" width="7" height="7" />
                                      <rect x="14" y="3" width="7" height="7" />
                                      <rect x="3" y="14" width="7" height="7" />
                                      <rect
                                        x="14"
                                        y="14"
                                        width="3"
                                        height="3"
                                      />
                                      <line x1="21" y1="14" x2="21" y2="21" />
                                      <line x1="14" y1="21" x2="21" y2="21" />
                                    </svg>
                                  </button>
                                  <button
                                    className="od-action-btn"
                                    title="Analytics"
                                    onClick={() => setActiveTab("analytics")}
                                  >
                                    <svg
                                      viewBox="0 0 24 24"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                    >
                                      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                                    </svg>
                                  </button>
                                  <button
                                    className="od-action-btn od-action-btn--danger"
                                    title="Delete"
                                    disabled={deletingId === ev.id}
                                    onClick={() => handleDelete(ev.id)}
                                  >
                                    <svg
                                      viewBox="0 0 24 24"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                    >
                                      <polyline points="3 6 5 6 21 6" />
                                      <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
                                      <path d="M10 11v6" />
                                      <path d="M14 11v6" />
                                      <path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2" />
                                    </svg>
                                  </button>
                                </>
                              ) : (
                                <>
                                  <button
                                    className="od-action-btn"
                                    title="Manage"
                                    onClick={() =>
                                      navigate(
                                        `/dashboard/manage-event/${ev.id}`,
                                        { state: { event: myEvents[i] } },
                                      )
                                    }
                                  >
                                    <svg
                                      viewBox="0 0 24 24"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                    >
                                      <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                                      <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                                    </svg>
                                  </button>
                                  <button
                                    className="od-action-btn"
                                    title="View"
                                    onClick={() =>
                                      navigate(`/events/${ev.id}`, {
                                        state: { event: myEvents[i] },
                                      })
                                    }
                                  >
                                    <svg
                                      viewBox="0 0 24 24"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                    >
                                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                      <circle cx="12" cy="12" r="3" />
                                    </svg>
                                  </button>
                                  <button
                                    className="od-action-btn"
                                    title="Certificate"
                                    onClick={() => {
                                      setCertEventId(String(ev.id));
                                      setActiveTab("certificates");
                                    }}
                                  >
                                    <svg
                                      viewBox="0 0 24 24"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                    >
                                      <circle cx="12" cy="8" r="7" />
                                      <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88" />
                                    </svg>
                                  </button>
                                </>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="od-empty">
                  No events created yet. Click &ldquo;+ New Event&rdquo; to get
                  started.
                </p>
              )}
            </section>
          </>
        );

      /* --- CREATE EVENT ------------------------------------------------------ */
      case "create":
        return (
          <section className="od-card od-create">
            <h2 className="od-card__title">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                style={{ width: 20, height: 20 }}
              >
                <circle cx="12" cy="12" r="10" />
                <line x1="12" y1="8" x2="12" y2="16" />
                <line x1="8" y1="12" x2="16" y2="12" />
              </svg>
              Create New Event
            </h2>
            <p className="od-create__sub">
              Fill in the details below to publish a new event.
            </p>

            {createMsg && (
              <div className={`od-alert od-alert--${createMsg.type}`}>
                {createMsg.type === "success" ? (
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <path d="M9 11l3 3L22 4" />
                    <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                  </svg>
                ) : (
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" y1="9" x2="9" y2="15" />
                    <line x1="9" y1="9" x2="15" y2="15" />
                  </svg>
                )}
                {createMsg.text}
              </div>
            )}

            <form className="od-form" onSubmit={handleCreateEvent}>
              {/* Row 1 */}
              <div className="od-form__row">
                <div className="od-form__group">
                  <label className="od-form__label">Event Name *</label>
                  <input
                    className="od-form__input"
                    type="text"
                    name="name"
                    value={form.name}
                    onChange={handleFormChange}
                    placeholder="e.g. TechFest 2026"
                    required
                  />
                </div>
                <div className="od-form__group">
                  <label className="od-form__label">Category *</label>
                  <select
                    className="od-form__input"
                    name="category"
                    value={form.category}
                    onChange={handleFormChange}
                    required
                  >
                    <option value="">Select category</option>
                    {CATEGORY_OPTIONS.map((c) => (
                      <option key={c} value={c}>
                        {c}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Row 2 — description */}
              <div className="od-form__group od-form__group--full">
                <label className="od-form__label">Description *</label>
                <textarea
                  className="od-form__input od-form__textarea"
                  name="description"
                  value={form.description}
                  onChange={handleFormChange}
                  placeholder="Describe the event..."
                  rows={4}
                  required
                />
                <DescriptionHelper
                  key={aiHelperKey}
                  form={form}
                  onUseDescription={(text) =>
                    setForm((prev) => ({ ...prev, description: text }))
                  }
                  onFaqsChange={setPendingFaqs}
                />
              </div>

              {/* Row 3 */}
              <div className="od-form__row">
                <div className="od-form__group">
                  <label className="od-form__label">Field / Department</label>
                  <input
                    className="od-form__input"
                    type="text"
                    name="field"
                    value={form.field}
                    onChange={handleFormChange}
                    placeholder="e.g. Computer Science"
                  />
                </div>
                <div className="od-form__group">
                  <label className="od-form__label">Event Date *</label>
                  <input
                    className="od-form__input"
                    type="date"
                    name="eventDate"
                    value={form.eventDate}
                    onChange={handleFormChange}
                    required
                  />
                </div>
              </div>

              {/* Row 4 */}
              <div className="od-form__row">
                <div className="od-form__group">
                  <label className="od-form__label">Venue *</label>
                  <input
                    className="od-form__input"
                    type="text"
                    name="venue"
                    value={form.venue}
                    onChange={handleFormChange}
                    placeholder="e.g. Convention Center, Bengaluru"
                    required
                  />
                </div>
                <div className="od-form__group">
                  <label className="od-form__label">Max People</label>
                  <input
                    className="od-form__input"
                    type="number"
                    name="maxPeople"
                    value={form.maxPeople}
                    onChange={handleFormChange}
                    placeholder="e.g. 500"
                    min={1}
                  />
                </div>
              </div>

              {/* Row 5 */}
              <div className="od-form__row">
                <div className="od-form__group">
                  <label className="od-form__label">
                    Registration Cost (₹)
                  </label>
                  <input
                    className="od-form__input"
                    type="text"
                    name="costOfRegistration"
                    value={form.costOfRegistration}
                    onChange={handleFormChange}
                    placeholder="0 for free"
                  />
                </div>
              </div>

              {/* Submit row */}
              <div className="od-form__actions">
                <button
                  className="od-btn od-btn--outline"
                  type="button"
                  onClick={() => {
                    setForm({ ...EMPTY_FORM });
                    setCreateMsg(null);
                    setPendingFaqs([]);
                    setAiHelperKey((k) => k + 1);
                  }}
                >
                  Reset
                </button>
                <button
                  className="od-btn od-btn--primary"
                  type="submit"
                  disabled={creating}
                >
                  {creating ? (
                    <>
                      <span className="od-spinner" /> Creating...
                    </>
                  ) : (
                    "+ Create Event"
                  )}
                </button>
              </div>
            </form>
          </section>
        );

      /* --- MANAGE EVENTS ----------------------------------------------------- */
      case "manage": {
        const upcomingEvents = managedEvents.filter(
          (ev) => ev.status.toLowerCase() !== "completed",
        );
        const completedEvents = managedEvents.filter(
          (ev) => ev.status.toLowerCase() === "completed",
        );

        const renderEventRow = (ev, i) => {
          const rawIndex = managedEvents.indexOf(ev);
          return (
            <tr key={ev.id ?? i}>
              <td className="od-table__name">{ev.title}</td>
              <td>{formatDateReadable(ev.date)}</td>
              <td>
                <span
                  className={`od-badge od-badge--${ev.status.toLowerCase().replace(/\s+/g, "-")}`}
                >
                  {ev.status}
                </span>
              </td>
              <td>
                <div className="od-actions">
                  {ev.status.toLowerCase() !== "completed" ? (
                    <>
                      <button
                        className="od-action-btn"
                        title="Manage"
                        onClick={() =>
                          navigate(`/dashboard/manage-event/${ev.id}`, {
                            state: { event: myEvents[rawIndex] },
                          })
                        }
                      >
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                          <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                        </svg>
                      </button>
                      <button
                        className="od-action-btn"
                        title="Scan Attendance"
                        onClick={() =>
                          navigate(`/scan/${ev.id}`, {
                            state: { eventName: ev.title },
                          })
                        }
                      >
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <rect x="3" y="3" width="7" height="7" />
                          <rect x="14" y="3" width="7" height="7" />
                          <rect x="3" y="14" width="7" height="7" />
                          <rect x="14" y="14" width="3" height="3" />
                          <line x1="21" y1="14" x2="21" y2="21" />
                          <line x1="14" y1="21" x2="21" y2="21" />
                        </svg>
                      </button>
                      <button
                        className="od-action-btn"
                        title="Analytics"
                        onClick={() => setActiveTab("analytics")}
                      >
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                        </svg>
                      </button>
                      <button
                        className="fb-btn fb-btn--primary"
                        style={{
                          fontSize: "0.78rem",
                          padding: "0.4rem 0.8rem",
                        }}
                        onClick={() => setCollabModal(ev.id)}
                      >
                        + Collaborator
                      </button>
                      <button
                        className="fb-btn fb-btn--ghost"
                        style={{
                          fontSize: "0.78rem",
                          padding: "0.4rem 0.8rem",
                        }}
                        onClick={() => handleCompleteEvent(ev.id)}
                        disabled={completingId === ev.id}
                      >
                        {completingId === ev.id
                          ? "Completing..."
                          : "Mark Complete"}
                      </button>
                      <button
                        className="od-action-btn od-action-btn--danger"
                        title="Delete"
                        disabled={deletingId === ev.id}
                        onClick={() => handleDelete(ev.id)}
                      >
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
                          <path d="M10 11v6" />
                          <path d="M14 11v6" />
                          <path d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2" />
                        </svg>
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        className="od-action-btn"
                        title="View"
                        onClick={() =>
                          navigate(`/events/${ev.id}`, {
                            state: { event: myEvents[rawIndex] },
                          })
                        }
                      >
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                          <circle cx="12" cy="12" r="3" />
                        </svg>
                      </button>
                      <button
                        className="od-action-btn"
                        title="Certificate"
                        onClick={() => {
                          setCertEventId(String(ev.id));
                          setActiveTab("certificates");
                        }}
                      >
                        <svg
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <circle cx="12" cy="8" r="7" />
                          <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88" />
                        </svg>
                      </button>
                    </>
                  )}
                </div>
              </td>
            </tr>
          );
        };

        return (
          <>
            {completeMsg && (
              <div className={`od-alert od-alert--${completeMsg.type}`}>
                {completeMsg.text}
              </div>
            )}

            <section className="od-card">
              <div className="od-card__header">
                <h2 className="od-card__title">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    style={{ width: 20, height: 20 }}
                  >
                    <rect x="3" y="4" width="18" height="18" rx="2" />
                    <line x1="16" y1="2" x2="16" y2="6" />
                    <line x1="8" y1="2" x2="8" y2="6" />
                    <line x1="3" y1="10" x2="21" y2="10" />
                  </svg>
                  Upcoming Events
                </h2>
                <button
                  className="od-btn od-btn--primary"
                  onClick={() => setActiveTab("create")}
                >
                  + New Event
                </button>
              </div>

              {eventsLoading ? (
                <div className="od-loading-row">
                  <span className="od-spinner" /> Loading events...
                </div>
              ) : upcomingEvents.length > 0 ? (
                <div className="od-table-wrap">
                  <table className="od-table od-table--manage">
                    <thead>
                      <tr>
                        <th>Event Name</th>
                        <th>Date</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>{upcomingEvents.map(renderEventRow)}</tbody>
                  </table>
                </div>
              ) : (
                <p className="od-empty">
                  No upcoming events. Click &ldquo;+ New Event&rdquo; to create
                  one.
                </p>
              )}
            </section>

            <section className="od-card" style={{ marginTop: "1.5rem" }}>
              <div className="od-card__header">
                <h2 className="od-card__title">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    style={{ width: 20, height: 20 }}
                  >
                    <path d="M9 11l3 3L22 4" />
                    <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                  </svg>
                  Completed Events
                </h2>
              </div>

              {completedEvents.length > 0 ? (
                <div className="od-table-wrap">
                  <table className="od-table od-table--manage">
                    <thead>
                      <tr>
                        <th>Event Name</th>
                        <th>Date</th>
                        <th>Status</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>{completedEvents.map(renderEventRow)}</tbody>
                  </table>
                </div>
              ) : (
                <p className="od-empty">No completed events yet.</p>
              )}
            </section>

            {collabModal && (
              <AddCollaboratorModal
                eventId={collabModal}
                onClose={() => setCollabModal(null)}
                onSuccess={() => setCollabModal(null)}
              />
            )}
          </>
        );
      }

      /* --- ATTENDEES --------------------------------------------------------- */
      case "attendees":
        return (
          <section className="od-card">
            <h2 className="od-card__title">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                style={{ width: 20, height: 20 }}
              >
                <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 00-3-3.87" />
                <path d="M16 3.13a4 4 0 010 7.75" />
              </svg>
              All Attendees
            </h2>
            {recentAttendees.length > 0 ? (
              <div className="od-table-wrap">
                <table className="od-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Email</th>
                      <th>Event</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentAttendees.map((a, i) => (
                      <tr key={a.id ?? i}>
                        <td>{a.name}</td>
                        <td>{a.email}</td>
                        <td>{a.event}</td>
                        <td>{formatDateReadable(a.date)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="od-empty">No attendees yet.</p>
            )}
          </section>
        );

      /* --- ANALYTICS --------------------------------------------------------- */
      case "analytics":
        return (
          <section className="od-card">
            <h2 className="od-card__title">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                style={{ width: 20, height: 20 }}
              >
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>
              Registration Trends
            </h2>
            {analyticsLoading ? (
              <div className="od-loading-row">
                <span className="od-spinner" /> Loading analytics...
              </div>
            ) : analyticsData.length > 0 ? (
              <>
                <div className="od-chart-wrap">
                  <ResponsiveContainer width="100%" height={320}>
                    <AreaChart
                      data={analyticsData}
                      margin={{ top: 10, right: 30, left: 0, bottom: 0 }}
                    >
                      <defs>
                        <linearGradient
                          id="regGradient"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="5%"
                            stopColor="#7c3aed"
                            stopOpacity={0.4}
                          />
                          <stop
                            offset="95%"
                            stopColor="#7c3aed"
                            stopOpacity={0}
                          />
                        </linearGradient>
                      </defs>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke="rgba(148,163,184,0.08)"
                        vertical={false}
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
                        axisLine={false}
                        allowDecimals={false}
                      />
                      <Tooltip
                        contentStyle={{
                          background: "#1e1b4b",
                          border: "1px solid rgba(139,92,246,0.3)",
                          borderRadius: 10,
                          color: "#e2e8f0",
                          fontSize: 13,
                        }}
                        labelStyle={{ color: "#a78bfa", fontWeight: 600 }}
                        itemStyle={{ color: "#e2e8f0" }}
                      />
                      <Area
                        type="monotone"
                        dataKey="registrations"
                        stroke="#7c3aed"
                        strokeWidth={2.5}
                        fill="url(#regGradient)"
                        dot={{
                          r: 4,
                          fill: "#7c3aed",
                          stroke: "#1e1b4b",
                          strokeWidth: 2,
                        }}
                        activeDot={{ r: 6, fill: "#a78bfa" }}
                        name="Registrations"
                      />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
                <p className="od-chart-caption">
                  Event registration trends over the last 6 months
                </p>
              </>
            ) : (
              <p className="od-empty">No registration trends data yet.</p>
            )}
          </section>
        );

      /* --- CERTIFICATES ---------------------------------------------------- */
      case "certificates":
        return (
          <section className="od-card">
            <h2 className="od-card__title">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                style={{ width: 20, height: 20 }}
              >
                <circle cx="12" cy="8" r="7" />
                <polyline points="8.21 13.89 7 23 12 20 17 23 15.79 13.88" />
              </svg>
              Generate Certificates
            </h2>

            {certMsg && (
              <div className={`od-alert od-alert--${certMsg.type}`}>
                {certMsg.type === "success" ? (
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <path d="M9 11l3 3L22 4" />
                    <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                  </svg>
                ) : (
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" y1="9" x2="9" y2="15" />
                    <line x1="9" y1="9" x2="15" y2="15" />
                  </svg>
                )}
                {certMsg.text}
              </div>
            )}

            <form className="od-cert-form" onSubmit={handleUploadCertificate}>
              <div className="od-cert-form__top">
                <div className="od-form__group">
                  <label className="od-form__label">Select Event</label>
                  <select
                    className="od-form__input"
                    value={certEventId}
                    onChange={(e) => setCertEventId(e.target.value)}
                    required
                  >
                    <option value="">-- Choose Event --</option>
                    {myEvents.map((ev) => {
                      const id = ev.id ?? ev.eventId;
                      const name =
                        ev.name ?? ev.title ?? ev.eventName ?? "Untitled";
                      return (
                        <option key={id} value={id}>
                          {name}
                        </option>
                      );
                    })}
                  </select>
                </div>

                <div className="od-form__group">
                  <label className="od-form__label">Certificate Template</label>
                  <input
                    className="od-form__input"
                    type="file"
                    accept=".pdf,.png,.jpg,.jpeg,.svg"
                    onChange={(e) => setCertFile(e.target.files[0] || null)}
                  />
                  {certFile && (
                    <span
                      style={{
                        fontSize: "0.75rem",
                        color: "#94a3b8",
                        marginTop: 4,
                      }}
                    >
                      {certFile.name}
                    </span>
                  )}
                </div>

                <button
                  className="od-btn od-btn--primary od-cert-btn"
                  type="submit"
                  disabled={certUploading}
                >
                  {certUploading ? (
                    <>
                      <span className="od-spinner" /> Uploading...
                    </>
                  ) : (
                    <>
                      <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        style={{ width: 16, height: 16 }}
                      >
                        <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4" />
                        <polyline points="17 8 12 3 7 8" />
                        <line x1="12" y1="3" x2="12" y2="15" />
                      </svg>
                      Generate All
                    </>
                  )}
                </button>
              </div>
            </form>
          </section>
        );

      /* --- ANNOUNCEMENTS --------------------------------------------------- */
      case "announcements":
        return (
          <section className="od-card">
            <h2 className="od-card__title">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                style={{ width: 20, height: 20 }}
              >
                <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 01-3.46 0" />
              </svg>
              Send Announcement
            </h2>

            {annMsg && (
              <div className={`od-alert od-alert--${annMsg.type}`}>
                {annMsg.type === "success" ? (
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <path d="M9 11l3 3L22 4" />
                    <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                  </svg>
                ) : (
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" y1="9" x2="9" y2="15" />
                    <line x1="9" y1="9" x2="15" y2="15" />
                  </svg>
                )}
                {annMsg.text}
              </div>
            )}

            <form className="od-ann-form" onSubmit={handleSendAnnouncement}>
              <div className="od-form__group">
                <label className="od-form__label">Event</label>
                <select
                  className="od-form__input od-ann-select"
                  value={annEventId}
                  onChange={(e) => setAnnEventId(e.target.value)}
                  required
                >
                  <option value="">-- Select Event --</option>
                  {myEvents.map((ev) => {
                    const id = ev.id ?? ev.eventId;
                    const name =
                      ev.name ?? ev.title ?? ev.eventName ?? "Untitled";
                    return (
                      <option key={id} value={id}>
                        {name}
                      </option>
                    );
                  })}
                </select>
              </div>

              <AnnouncementHelper
                eventId={annEventId}
                onUse={(draft) => {
                  setAnnTitle(draft.title);
                  setAnnMessage(draft.message);
                  setAnnMsg(null);
                }}
              />

              <div className="od-form__group od-form__group--full">
                <label className="od-form__label">Title</label>
                <input
                  className="od-form__input"
                  type="text"
                  value={annTitle}
                  onChange={(e) => setAnnTitle(e.target.value)}
                  placeholder="Announcement title..."
                  required
                />
              </div>

              <div className="od-form__group od-form__group--full">
                <label className="od-form__label">Message</label>
                <textarea
                  className="od-form__input od-form__textarea"
                  value={annMessage}
                  onChange={(e) => setAnnMessage(e.target.value)}
                  placeholder="Type your announcement..."
                  rows={5}
                  required
                />
              </div>

              <button
                className="od-btn od-btn--primary od-ann-send-btn"
                type="submit"
                disabled={annSending}
              >
                {annSending ? (
                  <>
                    <span className="od-spinner" /> Sending...
                  </>
                ) : (
                  <>
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      style={{ width: 16, height: 16 }}
                    >
                      <line x1="22" y1="2" x2="11" y2="13" />
                      <polygon points="22 2 15 22 11 13 2 9 22 2" />
                    </svg>
                    Send
                  </>
                )}
              </button>
            </form>
          </section>
        );

      /* --- COLLABORATORS ---------------------------------------------------- */
      case "collaborators":
        return (
          <section className="od-card">
            <h2 className="od-card__title">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                style={{ width: 20, height: 20 }}
              >
                <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
                <circle cx="8.5" cy="7" r="4" />
                <line x1="20" y1="8" x2="20" y2="14" />
                <line x1="23" y1="11" x2="17" y2="11" />
              </svg>
              Manage Collaborators
            </h2>

            {completeMsg && (
              <div className={`od-alert od-alert--${completeMsg.type}`}>
                {completeMsg.text}
              </div>
            )}

            {myEvents.length > 0 ? (
              <div className="od-table-wrap">
                <table className="od-table">
                  <thead>
                    <tr>
                      <th>Event</th>
                      <th>Date</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {managedEvents.map((ev) => (
                      <tr key={ev.id}>
                        <td className="od-table__name">{ev.title}</td>
                        <td>{formatDateReadable(ev.date)}</td>
                        <td>
                          <span
                            className={`od-badge od-badge--${ev.status.toLowerCase().replace(/\s+/g, "-")}`}
                          >
                            {ev.status}
                          </span>
                        </td>
                        <td>
                          <div className="od-actions">
                            <button
                              className="fb-btn fb-btn--primary"
                              style={{
                                fontSize: "0.78rem",
                                padding: "0.4rem 0.8rem",
                              }}
                              onClick={() => setCollabModal(ev.id)}
                            >
                              + Add Collaborator
                            </button>
                            {ev.status.toLowerCase() !== "completed" && (
                              <button
                                className="fb-btn fb-btn--ghost"
                                style={{
                                  fontSize: "0.78rem",
                                  padding: "0.4rem 0.8rem",
                                }}
                                onClick={() => handleCompleteEvent(ev.id)}
                                disabled={completingId === ev.id}
                              >
                                {completingId === ev.id
                                  ? "Completing..."
                                  : "Mark Complete"}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="od-empty">No events yet.</p>
            )}

            {collabModal && (
              <AddCollaboratorModal
                eventId={collabModal}
                onClose={() => setCollabModal(null)}
                onSuccess={() => setCollabModal(null)}
              />
            )}
          </section>
        );

      /* --- FEEDBACK ------------------------------------------------------- */
      case "feedback":
        return (
          <section className="od-card">
            <h2 className="od-card__title">
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                style={{ width: 20, height: 20 }}
              >
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
              </svg>
              Event Feedback
            </h2>

            {fbMsg && (
              <div className={`od-alert od-alert--${fbMsg.type}`}>
                {fbMsg.type === "success" ? (
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <path d="M9 11l3 3L22 4" />
                  </svg>
                ) : (
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <circle cx="12" cy="12" r="10" />
                    <line x1="15" y1="9" x2="9" y2="15" />
                    <line x1="9" y1="9" x2="15" y2="15" />
                  </svg>
                )}
                {fbMsg.text}
              </div>
            )}

            <div className="fb-request-row" style={{ marginBottom: "1.5rem" }}>
              <div className="fb-field" style={{ flex: 1 }}>
                <label className="fb-field__label">Event</label>
                <select
                  className="fb-field__input"
                  value={fbEventId}
                  onChange={(e) => setFbEventId(e.target.value)}
                >
                  <option value="">-- Select Event --</option>
                  {myEvents.map((ev) => {
                    const id = ev.id ?? ev.eventId;
                    const name =
                      ev.name ?? ev.title ?? ev.eventName ?? "Untitled";
                    return (
                      <option key={id} value={id}>
                        {name}
                      </option>
                    );
                  })}
                </select>
              </div>

              <button
                type="button"
                className="fb-btn fb-btn--primary"
                onClick={handleRequestFeedback}
                disabled={fbReqLoading || !fbEventId}
                style={{ alignSelf: "flex-end" }}
              >
                {fbReqLoading ? (
                  <>
                    <span className="fb-spinner" /> Sending...
                  </>
                ) : (
                  "Request Feedback"
                )}
              </button>

              <button
                type="button"
                className="fb-btn fb-btn--ghost"
                onClick={handleLoadFeedback}
                disabled={fbLoading || !fbEventId}
                style={{ alignSelf: "flex-end" }}
              >
                {fbLoading ? (
                  <>
                    <span className="fb-spinner" /> Loading...
                  </>
                ) : (
                  "View Feedback"
                )}
              </button>
            </div>

            {fbEventId && <FeedbackInsights key={fbEventId} eventId={fbEventId} />}

            {fbLoading ? (
              <div className="fb-loading">
                <span className="fb-spinner" /> Loading feedback...
              </div>
            ) : fbLoadedFor === fbEventId && fbData.length > 0 ? (
              <div className="od-table-wrap">
                <table className="fb-feedback-table">
                  <thead>
                    <tr>
                      <th>User ID</th>
                      <th>Rating</th>
                      <th>Feedback</th>
                    </tr>
                  </thead>
                  <tbody>
                    {fbData.map((fb, i) => (
                      <tr key={fb?.id ?? i}>
                        <td>{fb?.userId ?? "--"}</td>
                        <td>
                          <span className="fb-stars-display">
                            {"★".repeat(fb?.noOfStars || 0)}
                            {"☆".repeat(5 - (fb?.noOfStars || 0))}
                          </span>
                          <span
                            style={{
                              color: "#94a3b8",
                              fontSize: "0.75rem",
                              marginLeft: 6,
                            }}
                          >
                            {fb?.noOfStars ?? 0}/5
                          </span>
                        </td>
                        <td>
                          {fb?.feedBack || (
                            <span style={{ color: "#64748b" }}>No comment</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="fb-empty">
                {!fbEventId
                  ? "Select an event to view or request feedback."
                  : fbLoadedFor === fbEventId
                    ? "No feedback received yet for this event."
                    : "Click “View Feedback” to see individual responses."}
              </p>
            )}
          </section>
        );

      default:
        return null;
    }
  };

  /* ---------------------------------------------------------------------------
     RENDER
     --------------------------------------------------------------------------- */
  return (
    <div className="od-page">
      <Navbar />
      <div className="od-layout">
        {/* -- Sidebar -- */}
        <aside className="od-sidebar">
          <div className="od-sidebar__profile">
            <div className="od-sidebar__avatar">{initials}</div>
            <h3 className="od-sidebar__name">Organizer Panel</h3>
            <p className="od-sidebar__role">Event Management</p>
          </div>
          <hr className="od-sidebar__divider" />
          <nav className="od-sidebar__nav">
            {SIDEBAR_TABS.map((tab) => (
              <button
                key={tab.key}
                className={`od-sidebar__tab ${activeTab === tab.key ? "od-sidebar__tab--active" : ""}`}
                onClick={() => setActiveTab(tab.key)}
              >
                <TabIcon type={tab.icon} />
                {tab.label}
              </button>
            ))}
          </nav>
        </aside>

        {/* -- Main content -- */}
        <main className="od-content">{renderContent()}</main>
      </div>
    </div>
  );
}
