import { useState, useEffect } from "react";
import useTabScroll from "../../../hooks/useTabScroll";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../context/AuthContext";
import Navbar from "../../../components/Navbar";
import StudentReports from "../../../components/social/StudentReports";
import EngagementPanel from "../../../components/admin/EngagementPanel";
import {
  fetchAllAdminData,
  fetchAllEvents,
  fetchPendingEventCount,
  fetchOnboardRequests,
  fetchStudents,
  fetchOrganizers,
  searchUsers,
  acceptOrganizer,
  acceptAdmin,
  approveEvent,
  rejectEvent,
} from "../../../services/adminDashboardService";
import * as profileApi from "../../../services/adminProfileService";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
} from "recharts";
import "../../../styles/dashboard/AdminDashboard.css";

const PIE_COLORS = ["#8b5cf6", "#06b6d4", "#f59e0b", "#10b981", "#ef4444"];

const SIDEBAR_TABS = [
  { key: "overview", label: "Overview", icon: "grid" },
  { key: "users", label: "Users", icon: "users" },
  { key: "events", label: "Events", icon: "calendar" },
  { key: "analytics", label: "Analytics", icon: "chart" },
  { key: "onboard-organizer", label: "Accept Organizer", icon: "user-check" },
  { key: "onboard-admin", label: "Accept Admin", icon: "shield" },
  { key: "reports", label: "Reports / Flags", icon: "flag" },
  { key: "profile-config", label: "Profile Config", icon: "settings" },
];

const getInitials = (name) => {
  if (!name) return "A";
  const parts = name.trim().split(" ");
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return name.substring(0, 2).toUpperCase();
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
    case "flag":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z" />
          <line x1="4" y1="22" x2="4" y2="15" />
        </svg>
      );
    case "user-check":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
          <circle cx="8.5" cy="7" r="4" />
          <polyline points="17 11 19 13 23 9" />
        </svg>
      );
    case "shield":
      return (
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
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
          <circle cx="12" cy="12" r="3" />
          <path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 01-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z" />
        </svg>
      );
    default:
      return null;
  }
};

const toArray = (val) => {
  if (Array.isArray(val)) return val;
  if (val && typeof val === "object") {
    for (const k of [
      "data",
      "content",
      "events",
      "users",
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

/* Pick the first non-null, non-empty value from an object across multiple possible keys */
const pick = (obj, ...keys) => {
  for (const k of keys) {
    const v = obj?.[k];
    if (v !== undefined && v !== null && v !== "") return v;
  }
  return null;
};

/* Normalise one user object */
const normaliseUser = /* eslint-disable-line no-unused-vars */ (u) => {
  const rawRole = pick(u, "role", "userRole", "roles");
  const role = Array.isArray(rawRole) ? rawRole[0] : rawRole;
  return {
    id: pick(u, "id", "userId", "user_id"),
    name:
      pick(u, "name", "userName", "fullName", "username", "displayName") ??
      "--",
    email: pick(u, "email", "userEmail", "emailAddress") ?? "--",
    role:
      (typeof role === "string" ? role.replace(/^ROLE_/i, "") : role) ?? "--",
    joinedDate:
      pick(
        u,
        "joinedDate",
        "joined_date",
        "createdAt",
        "created_at",
        "registrationDate",
        "dateJoined",
      ) ?? "--",
  };
};

/* Normalise one event object from EventSummaryDto */
const normaliseEvent = (ev) => {
  const rawDate = pick(
    ev,
    "date",
    "eventDate",
    "startDate",
    "start_date",
    "scheduledDate",
  );
  let formattedDate = "--";
  if (rawDate && rawDate !== "--") {
    try {
      const d = new Date(rawDate);
      if (!isNaN(d)) {
        formattedDate =
          d.toLocaleDateString("en-US", {
            month: "short",
            day: "numeric",
            year: "numeric",
          }) +
          "  " +
          d.toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" });
      } else {
        formattedDate = rawDate;
      }
    } catch {
      formattedDate = rawDate;
    }
  }
  return {
    id: pick(ev, "id", "eventId", "event_id"),
    title:
      pick(ev, "title", "eventTitle", "event_title", "name", "eventName") ??
      "--",
    organizerId:
      pick(
        ev,
        "organizerId",
        "organizer_id",
        "organizer",
        "organizerName",
        "createdBy",
        "userId",
        "user_id",
        "ownerId",
        "owner_id",
        "orgId",
      ) ?? "--",
    date: formattedDate,
    status:
      pick(ev, "status", "eventStatus", "approvalStatus") ??
      (() => {
        const raw = pick(
          ev,
          "date",
          "eventDate",
          "startDate",
          "start_date",
          "scheduledDate",
        );
        if (!raw) return "--";
        try {
          return new Date(raw) < new Date() ? "Completed" : "Upcoming";
        } catch {
          return "--";
        }
      })(),
    image: pick(ev, "image", "imageUrl", "img", "thumbnail") ?? null,
    description: pick(ev, "description", "shortDescription", "summary") ?? "",
    isApproved: ev?.isApproved ?? ev?.approved ?? false,
  };
};

/* Normalise one analytics entry */
const normaliseAnalytics = (a) => ({
  month: pick(a, "month", "monthName", "period", "label") ?? "--",
  users: pick(a, "users", "userCount", "newUsers", "user_count") ?? 0,
  events: pick(a, "events", "eventCount", "newEvents", "event_count") ?? 0,
});

/* Normalise one role-distribution entry */
const normaliseRole = (r) => ({
  role: pick(r, "role", "roleName", "name", "label") ?? "--",
  count: pick(r, "count", "total", "value", "userCount") ?? 0,
});

export default function AdminDashboard() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("overview");
  useTabScroll(activeTab, ".admin-sidebar__nav");
  const [data, setData] = useState(null);

  // -- Pageable events state -------------------------------------------------
  const [allEvents, setAllEvents] = useState([]);
  const [eventsPage, setEventsPage] = useState(0);
  // const eventsTotalPages = 0;
  // const setEventsTotalPages = () => {};
  const [eventsLoading, setEventsLoading] = useState(false);
  const [eventSearch, setEventSearch] = useState("");

  // -- Onboarding state ------------------------------------------------------
  const [onboardRequests, setOnboardRequests] = useState([]);
  const [onboardLoading, setOnboardLoading] = useState(false);
  const [acceptingId, setAcceptingId] = useState(null);

  // -- Students & Organizers state -------------------------------------------
  const [students, setStudents] = useState([]);
  const [organizers, setOrganizers] = useState([]);
  const [, setUsersLoading] = useState(false);
  const [userSearchEmail, setUserSearchEmail] = useState("");
  const [userSearchResults, setUserSearchResults] = useState([]);
  const [userSearching, setUserSearching] = useState(false);

  // -- Profile Config state --------------------------------------------------
  const [pcSection, setPcSection] = useState("badges"); // badges|interests|notifications|social|preferences
  const [pcItems, setPcItems] = useState([]);
  const [pcLoading, setPcLoading] = useState(false);
  const [pcSaving, setPcSaving] = useState(false);
  const [pcError, setPcError] = useState(null);
  const [pcModal, setPcModal] = useState(null); // null | 'create' | 'edit'
  const [pcEditId, setPcEditId] = useState(null);
  const [pcForm, setPcForm] = useState({});

  useEffect(() => {
    // No "already fetched" ref guard: under StrictMode the first run is cancelled on the simulated
    // unmount, and the guard then stopped the second run from ever fetching (cards stayed "--").
    let cancelled = false;
    (async () => {
      try {
        const res = await fetchAllAdminData();
        if (!cancelled) setData(res);
      } catch {
        /* renders with -- placeholders */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  // -- Overview: counts of items waiting for the admin ------------------------
  const [attention, setAttention] = useState(null); // { organizers, admins, events } or null while loading
  useEffect(() => {
    if (activeTab !== "overview") return;
    let cancelled = false;
    (async () => {
      const [reqRes, evRes] = await Promise.allSettled([
        fetchOnboardRequests(),
        fetchPendingEventCount(),
      ]);
      if (cancelled) return;
      const requests =
        reqRes.status === "fulfilled" && Array.isArray(reqRes.value) ? reqRes.value : [];
      setAttention({
        organizers: requests.filter((r) => r.roleType === "ORGANIZER").length,
        admins: requests.filter((r) => r.roleType === "ADMIN").length,
        events: evRes.status === "fulfilled" ? evRes.value : 0,
      });
    })();
    return () => {
      cancelled = true;
    };
  }, [activeTab]);

  // -- Fetch pageable events when Events tab is active -----------------------
  useEffect(() => {
    if (activeTab !== "events") return;
    let cancelled = false;
    (async () => {
      setEventsLoading(true);
      try {
        const res = await fetchAllEvents(0, 9999);
        if (cancelled) return;
        if (res && typeof res === "object" && !Array.isArray(res)) {
          const content = res.content ?? res.data ?? res.items ?? [];
          setAllEvents(
            Array.isArray(content) ? content.map(normaliseEvent) : [],
          );
        } else if (Array.isArray(res)) {
          setAllEvents(res.map(normaliseEvent));
        }
      } catch (err) {
        console.error("[Admin Events] fetch error:", err);
        if (!cancelled) setAllEvents([]);
      } finally {
        if (!cancelled) setEventsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeTab]);

  // -- Fetch students & organizers when Users tab is active ------------------
  useEffect(() => {
    if (activeTab !== "users" && activeTab !== "overview") return;
    if (students.length > 0 || organizers.length > 0) return;
    let cancelled = false;
    (async () => {
      setUsersLoading(true);
      try {
        const [stuRes, orgRes] = await Promise.allSettled([
          fetchStudents(),
          fetchOrganizers(),
        ]);
        if (cancelled) return;
        setStudents(stuRes.status === "fulfilled" ? stuRes.value : []);
        setOrganizers(orgRes.status === "fulfilled" ? orgRes.value : []);
      } catch (err) {
        console.error("[Admin Users] fetch error:", err);
      } finally {
        if (!cancelled) setUsersLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeTab, students.length, organizers.length]);

  // -- User search by email handler ------------------------------------------
  const handleUserSearch = async (e) => {
    e.preventDefault();
    const query = userSearchEmail.trim().toLowerCase();
    if (!query) return;
    setUserSearching(true);
    try {
      // One row per account, with every role it has (an organizer is usually a student too)
      setUserSearchResults(await searchUsers(query));
    } catch (err) {
      console.error("[Admin UserSearch] error:", err);
      setUserSearchResults([]);
    } finally {
      setUserSearching(false);
    }
  };

  // -- Approve / reject event handlers ---------------------------------------
  const handleApproveEvent = async (eventId) => {
    try {
      await approveEvent(eventId);
      setAllEvents((prev) =>
        prev.map((ev) =>
          ev.id === eventId ? { ...ev, isApproved: true } : ev,
        ),
      );
    } catch (err) {
      alert(
        err?.response?.data?.message ||
          err?.message ||
          "Failed to approve event",
      );
    }
  };

  const handleRejectEvent = async (eventId) => {
    if (!window.confirm("Reject and remove this event?")) return;
    try {
      await rejectEvent(eventId);
      setAllEvents((prev) => prev.filter((ev) => ev.id !== eventId));
    } catch (err) {
      alert(
        err?.response?.data?.message ||
          err?.message ||
          "Failed to reject event",
      );
    }
  };

  // -- Fetch onboard requests when either onboarding tab is active -----------
  useEffect(() => {
    if (activeTab !== "onboard-organizer" && activeTab !== "onboard-admin")
      return;
    let cancelled = false;
    (async () => {
      setOnboardLoading(true);
      try {
        const res = await fetchOnboardRequests();
        if (!cancelled) setOnboardRequests(Array.isArray(res) ? res : []);
      } catch (err) {
        console.error("[Onboard] fetch error:", err);
        if (!cancelled) setOnboardRequests([]);
      } finally {
        if (!cancelled) setOnboardLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeTab]);

  // -- Onboard action handlers -----------------------------------------------
  const handleAcceptOrganizer = async (email, id) => {
    setAcceptingId(id);
    console.log("[AcceptOrganizer] sending email:", email, "| row id:", id);
    try {
      await acceptOrganizer(email);
      setOnboardRequests((prev) =>
        prev.filter((r) => (r.id ?? r.userId) !== id),
      );
    } catch (err) {
      const resp = err?.response;
      console.error("[AcceptOrganizer] status:", resp?.status);
      console.error(
        "[AcceptOrganizer] response data:",
        JSON.stringify(resp?.data),
      );
      const msg =
        resp?.data?.message ||
        resp?.data?.error ||
        resp?.data?.detail ||
        (typeof resp?.data === "string" ? resp.data : null) ||
        err.message ||
        "Failed to accept organizer";
      alert(`Organizer accept failed (${resp?.status ?? "?"}):\n${msg}`);
    } finally {
      setAcceptingId(null);
    }
  };

  const handleAcceptAdmin = async (email, id) => {
    setAcceptingId(id);
    console.log("[AcceptAdmin] sending email:", email, "| row id:", id);
    try {
      await acceptAdmin(email);
      setOnboardRequests((prev) =>
        prev.filter((r) => (r.id ?? r.userId) !== id),
      );
    } catch (err) {
      const resp = err?.response;
      console.error("[AcceptAdmin] status:", resp?.status);
      console.error("[AcceptAdmin] response data:", JSON.stringify(resp?.data));
      const msg =
        resp?.data?.message ||
        resp?.data?.error ||
        resp?.data?.detail ||
        (typeof resp?.data === "string" ? resp.data : null) ||
        err.message ||
        "Failed to accept admin";
      alert(`Admin accept failed (${resp?.status ?? "?"}):\n${msg}`);
    } finally {
      setAcceptingId(null);
    }
  };

  /* ----------------------------------------------------------------------------
     PROFILE CONFIG — CRUD helpers
     ---------------------------------------------------------------------------- */
  const PC_SECTIONS = [
    {
      key: "badges",
      label: "Badges",
      description: "Recognition labels available across the platform.",
    },
    {
      key: "interests",
      label: "Interests",
      description: "Interest categories users can choose from.",
    },
    {
      key: "notifications",
      label: "Notifications",
      description: "Notification types available in user settings.",
    },
    {
      key: "social",
      label: "Social Profiles",
      description: "Supported social platforms for user profile links.",
    },
    {
      key: "preferences",
      label: "Preferences",
      description: "Volunteer and profile preference options.",
    },
  ];

  // Field definitions — keys match exact Java entity field names
  const PC_FIELDS = {
    badges: [
      { key: "badgeName", label: "Badge Name", required: true },
      { key: "badgeDescription", label: "Description" },
    ],
    interests: [
      { key: "interestName", label: "Interest Name", required: true },
    ],
    notifications: [
      { key: "notificationType", label: "Notification Type", required: true },
    ],
    social: [{ key: "platform", label: "Platform Name", required: true }],
    preferences: [
      { key: "preferenceType", label: "Preference Type", required: true },
    ],
  };

  // Columns to show in the list table
  const PC_COLUMNS = {
    badges: ["id", "badgeName", "badgeDescription"],
    interests: ["interestId", "interestName"],
    notifications: ["id", "notificationType"],
    social: ["id", "platform"],
    preferences: ["preferenceId", "preferenceType"],
  };

  // Primary-key field name per section (varies across entities)
  const PC_ID_KEY = {
    badges: "id",
    interests: "interestId",
    notifications: "id",
    social: "id",
    preferences: "preferenceId",
  };

  // Fetch by section (no id — endpoints no longer use path variable)
  const pcFetch = async (section) => {
    switch (section) {
      case "badges":
        return profileApi.getBadges();
      case "interests":
        return profileApi.getInterests();
      case "notifications":
        return profileApi.getNotifications();
      case "social":
        return profileApi.getSocialProfiles();
      case "preferences":
        return profileApi.getPreferences();
      default:
        return null;
    }
  };

  const pcCreate = async (section, body) => {
    switch (section) {
      case "badges":
        return profileApi.createBadge(body);
      case "interests":
        return profileApi.createInterest(body);
      case "notifications":
        return profileApi.createNotification(body);
      case "social":
        return profileApi.addSocialProfile(body);
      case "preferences":
        return profileApi.createPreference(body);
      default:
        return null;
    }
  };

  const pcUpdate = async (section, body) => {
    switch (section) {
      case "badges":
        return profileApi.updateBadge(body);
      case "interests":
        return profileApi.updateInterest(body);
      case "notifications":
        return profileApi.updateNotification(body);
      case "social":
        return profileApi.updateSocialProfile(body);
      case "preferences":
        return profileApi.updatePreference(body);
      default:
        return null;
    }
  };

  const pcDelete = async (section, id) => {
    switch (section) {
      case "badges":
        return profileApi.deleteBadge(id);
      case "interests":
        return profileApi.deleteInterest(id);
      case "notifications":
        return profileApi.deleteNotification(id);
      case "social":
        return profileApi.deleteSocialProfile(id);
      case "preferences":
        return profileApi.deletePreference(id);
      default:
        return null;
    }
  };

  // Load the active section's items when it changes while profile-config is open.
  // A response for a section the admin already left is ignored.
  useEffect(() => {
    if (activeTab !== "profile-config") return;
    let cancelled = false;
    (async () => {
      setPcLoading(true);
      setPcError(null);
      try {
        const items = await pcFetch(pcSection);
        if (!cancelled) setPcItems(Array.isArray(items) ? items : items ? [items] : []);
      } catch (err) {
        console.error("[ProfileConfig] load error:", err);
        if (!cancelled) setPcItems([]);
      } finally {
        if (!cancelled) setPcLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeTab, pcSection]);

  const handlePcOpenCreate = () => {
    const empty = {};
    PC_FIELDS[pcSection].forEach((f) => {
      empty[f.key] = "";
    });
    setPcForm(empty);
    setPcEditId(null);
    setPcModal("create");
    setPcError(null);
  };

  const handlePcOpenEdit = async (id) => {
    setPcSaving(true);
    setPcError(null);
    try {
      const item = pcItems.find((i) => i[PC_ID_KEY[pcSection]] === id);
      if (!item) throw new Error("Item not found");
      const formData = {};
      PC_FIELDS[pcSection].forEach((f) => {
        formData[f.key] = item[f.key] ?? "";
      });
      setPcForm(formData);
      setPcEditId(id);
      setPcModal("edit");
    } catch (err) {
      console.error("[ProfileConfig] fetch item error:", err);
      setPcError(
        typeof err?.response?.data === "string"
          ? err.response.data
          : err.message || "Failed to load item",
      );
    } finally {
      setPcSaving(false);
    }
  };

  const handlePcSave = async (e) => {
    e.preventDefault();
    setPcSaving(true);
    setPcError(null);
    try {
      const payload =
        pcModal === "edit" && pcEditId != null
          ? { ...pcForm, [PC_ID_KEY[pcSection]]: pcEditId }
          : pcForm;
      if (pcModal === "edit" && pcEditId != null) {
        const updated = await pcUpdate(pcSection, payload);
        setPcItems((prev) =>
          prev.map((i) =>
            i[PC_ID_KEY[pcSection]] === pcEditId
              ? { ...i, ...updated, ...payload }
              : i,
          ),
        );
      } else {
        const created = await pcCreate(pcSection, payload);
        if (created) setPcItems((prev) => [...prev, created]);
      }
      setPcModal(null);
    } catch (err) {
      console.error("[ProfileConfig] save error:", err);
      const d = err?.response?.data;
      setPcError(
        typeof d === "string"
          ? d
          : d?.message || d?.error || err.message || "Save failed",
      );
    } finally {
      setPcSaving(false);
    }
  };

  const handlePcDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this item?")) return;
    try {
      await pcDelete(pcSection, id);
      setPcItems((prev) => prev.filter((i) => i[PC_ID_KEY[pcSection]] !== id));
    } catch (err) {
      console.error("[ProfileConfig] delete error:", err);
      alert(
        typeof err?.response?.data === "string"
          ? err.response.data
          : "Delete failed",
      );
    }
  };

  const handlePcLookup = async () => {
    setPcLoading(true);
    setPcError(null);
    try {
      const items = await pcFetch(pcSection);
      setPcItems(Array.isArray(items) ? items : items ? [items] : []);
    } catch (err) {
      console.error("[ProfileConfig] refresh error:", err);
      setPcError(
        typeof err?.response?.data === "string"
          ? err.response.data
          : "Failed to refresh",
      );
    } finally {
      setPcLoading(false);
    }
  };

  const fullName = user?.name || user?.firstName || "Admin";
  const initials = getInitials(fullName);

  /* Flexible extraction — handles any field name variant the API may return */
  const totalUsers =
    pick(data, "totalUsers", "total_users", "userCount", "totalUserCount") ??
    (students.length + organizers.length > 0
      ? students.length + organizers.length
      : "--");
  const totalEvents =
    pick(
      data,
      "totalEvents",
      "total_events",
      "eventCount",
      "totalEventCount",
    ) ?? "--";
  const activeOrganizers =
    pick(
      data,
      "activeOrganizers",
      "active_organizers",
      "organizerCount",
      "totalOrganizers",
      "organizers",
    ) ?? (organizers.length > 0 ? organizers.length : "--");
  const platformRevenue =
    pick(
      data,
      "platformRevenue",
      "platform_revenue",
      "totalRevenue",
      "revenue",
      "total_revenue",
    ) ?? "--";

  // const recentUsers = toArray(
  //   data?.recentUsers ??
  //     data?.users ??
  //     data?.recent_users ??
  //     data?.latestUsers ??
  //     data?.userList,
  // ).map(normaliseUser);
  // const pendingEvents = toArray(
  //   data?.pendingEvents ??
  //     data?.events ??
  //     data?.pending_events ??
  //     data?.eventList ??
  //     data?.eventRequests,
  // ).map(normaliseEvent);
  const systemAnalytics = toArray(
    data?.systemAnalytics ??
      data?.analytics ??
      data?.system_analytics ??
      data?.monthlyTrends ??
      data?.trend,
  ).map(normaliseAnalytics);
  const roleDistribution = toArray(
    data?.roleDistribution ??
      data?.roles ??
      data?.role_distribution ??
      data?.roleStats ??
      data?.roleBreakdown,
  ).map(normaliseRole);

  const renderContent = () => {
    switch (activeTab) {
      case "overview":
        return (
          <>
            <section className="admin-metrics">
              <div className="admin-metric-card">
                <span className="admin-metric-card__icon admin-metric-card__icon--blue">
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
                <div className="admin-metric-card__info">
                  <span className="admin-metric-card__value">{totalUsers}</span>
                  <span className="admin-metric-card__label">Total Users</span>
                </div>
              </div>
              <div className="admin-metric-card">
                <span className="admin-metric-card__icon admin-metric-card__icon--green">
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
                <div className="admin-metric-card__info">
                  <span className="admin-metric-card__value">{totalEvents}</span>
                  <span className="admin-metric-card__label">Total Events</span>
                </div>
              </div>
              <div className="admin-metric-card">
                <span className="admin-metric-card__icon admin-metric-card__icon--purple">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                    <circle cx="12" cy="7" r="4" />
                  </svg>
                </span>
                <div className="admin-metric-card__info">
                  <span className="admin-metric-card__value">
                    {activeOrganizers}
                  </span>
                  <span className="admin-metric-card__label">
                    Active Organizers
                  </span>
                </div>
              </div>
              <div className="admin-metric-card">
                <span className="admin-metric-card__icon admin-metric-card__icon--amber">
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <line x1="12" y1="1" x2="12" y2="23" />
                    <path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6" />
                  </svg>
                </span>
                <div className="admin-metric-card__info">
                  <span className="admin-metric-card__value">
                    {typeof platformRevenue === "number"
                      ? `₹${platformRevenue.toLocaleString("en-IN")}`
                      : platformRevenue}
                  </span>
                  <span className="admin-metric-card__label">
                    Platform Revenue
                  </span>
                </div>
              </div>
            </section>

            <section className="admin-card admin-attention">
              <h2 className="admin-card__title">Needs your attention</h2>
              {attention === null ? (
                <p className="admin-empty">Loading…</p>
              ) : (
                <>
                  <div className="admin-attention__list">
                    {[
                      { key: "onboard-organizer", count: attention.organizers, label: "Organizer requests", hint: "Users asking to host events" },
                      { key: "events", count: attention.events, label: "Events awaiting approval", hint: "Hidden from students until you approve them" },
                      { key: "onboard-admin", count: attention.admins, label: "Admin requests", hint: "Users asking for admin access" },
                    ].map((item) => (
                      <div className="admin-attention__row" key={item.key}>
                        <span className={`admin-attention__count${item.count ? "" : " admin-attention__count--zero"}`}>
                          {item.count}
                        </span>
                        <span className="admin-attention__text">
                          <span className="admin-attention__label">{item.label}</span>
                          <span className="admin-attention__hint">{item.hint}</span>
                        </span>
                        <button
                          type="button"
                          className="admin-accept-btn admin-accept-btn--admin"
                          onClick={() => setActiveTab(item.key)}
                        >
                          {item.count ? "Review" : "Open"}
                        </button>
                      </div>
                    ))}
                  </div>
                  {!attention.organizers && !attention.events && !attention.admins && (
                    <p className="admin-attention__done">
                      All caught up — nothing is waiting for review. New organizer requests and events will appear here.
                    </p>
                  )}
                </>
              )}
            </section>
          </>
        );

      case "users":
        return (
          <section className="admin-card">
            <h2 className="admin-card__title">Search Users</h2>
            <form
              className="admin-search-form"
              onSubmit={handleUserSearch}
              style={{
                display: "flex",
                gap: "0.75rem",
                marginBottom: "1.5rem",
                alignItems: "flex-end",
              }}
            >
              <div style={{ flex: 1 }}>
                <label
                  style={{
                    display: "block",
                    fontSize: "0.82rem",
                    color: "#94a3b8",
                    marginBottom: "0.4rem",
                  }}
                >
                  Search by email or name
                </label>
                <input
                  type="text"
                  className="od-form__input"
                  placeholder="Enter email or name..."
                  value={userSearchEmail}
                  onChange={(e) => setUserSearchEmail(e.target.value)}
                  style={{
                    width: "100%",
                    padding: "0.6rem 1rem",
                    background: "rgba(139,92,246,0.06)",
                    border: "1px solid rgba(139,92,246,0.2)",
                    borderRadius: "8px",
                    color: "#e2e8f0",
                    fontSize: "0.9rem",
                  }}
                />
              </div>
              <button
                type="submit"
                className="admin-accept-btn"
                disabled={userSearching || !userSearchEmail.trim()}
                style={{ padding: "0.6rem 1.5rem", whiteSpace: "nowrap" }}
              >
                {userSearching ? "Searching…" : "Search"}
              </button>
            </form>

            {userSearchResults.length > 0 ? (
              <div className="admin-table-wrap">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Name</th>
                      <th>Email</th>
                      <th>Role</th>
                    </tr>
                  </thead>
                  <tbody>
                    {userSearchResults.map((u) => (
                      <tr key={u.id}>
                        <td>{u.id}</td>
                        <td>{u.name || "--"}</td>
                        <td>{u.email}</td>
                        <td>
                          {(u.roles?.length ? u.roles : ["--"]).map((role) => (
                            <span
                              key={role}
                              className={`admin-badge admin-badge--${role.toLowerCase()}`}
                              style={{ marginRight: 4 }}
                            >
                              {role}
                            </span>
                          ))}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : userSearchEmail.trim() && !userSearching ? (
              <p className="admin-empty">
                No users found matching &ldquo;{userSearchEmail}&rdquo;.
              </p>
            ) : (
              <p className="admin-empty">
                Enter an email or name to search for students and organizers.
              </p>
            )}
          </section>
        );

      case "events": {
        const EVENTS_PER_PAGE = 8;
        const q = eventSearch.trim().toLowerCase();
        const filtered = q
          ? allEvents.filter(
              (ev) =>
                (ev.title ?? "").toLowerCase().includes(q) ||
                (ev.description ?? "").toLowerCase().includes(q) ||
                (ev.status ?? "").toLowerCase().includes(q),
            )
          : allEvents;
        const totalPgs = Math.ceil(filtered.length / EVENTS_PER_PAGE) || 1;
        const safePage = Math.min(eventsPage, totalPgs - 1);
        const pageEvents = filtered.slice(
          safePage * EVENTS_PER_PAGE,
          (safePage + 1) * EVENTS_PER_PAGE,
        );

        return (
          <section className="admin-card">
            <h2 className="admin-card__title">All Events</h2>

            <div
              style={{
                display: "flex",
                gap: "0.75rem",
                marginBottom: "1.5rem",
                alignItems: "flex-end",
              }}
            >
              <div style={{ flex: 1 }}>
                <label
                  style={{
                    display: "block",
                    fontSize: "0.82rem",
                    color: "#94a3b8",
                    marginBottom: "0.4rem",
                  }}
                >
                  Search events
                </label>
                <input
                  type="text"
                  className="od-form__input"
                  placeholder="Search by title, description, or status..."
                  value={eventSearch}
                  onChange={(e) => {
                    setEventSearch(e.target.value);
                    setEventsPage(0);
                  }}
                  style={{
                    width: "100%",
                    padding: "0.6rem 1rem",
                    background: "rgba(139,92,246,0.06)",
                    border: "1px solid rgba(139,92,246,0.2)",
                    borderRadius: "8px",
                    color: "#e2e8f0",
                    fontSize: "0.9rem",
                  }}
                />
              </div>
            </div>

            {eventsLoading ? (
              <p className="admin-empty">Loading events…</p>
            ) : pageEvents.length > 0 ? (
              <>
                <div className="admin-table-wrap">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        <th>Event</th>
                        <th>Date & Time</th>
                        <th>Status</th>
                        <th>Approval</th>
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pageEvents.map((ev, i) => (
                        <tr key={ev?.id ?? i}>
                          <td>{ev?.title ?? "--"}</td>
                          <td className="admin-date-cell">{ev?.date ?? "--"}</td>
                          <td>
                            <span
                              className={`admin-badge admin-badge--${(ev?.status || "").toLowerCase()}`}
                            >
                              {ev?.status ?? "--"}
                            </span>
                          </td>
                          <td>
                            {ev?.isApproved ? (
                              <span
                                className="admin-badge admin-badge--completed"
                                style={{ fontSize: "0.75rem" }}
                              >
                                Approved
                              </span>
                            ) : (
                              <span
                                className="admin-badge admin-badge--upcoming"
                                style={{ fontSize: "0.75rem" }}
                              >
                                Pending
                              </span>
                            )}
                          </td>
                          <td>
                            <div
                              style={{
                                display: "flex",
                                gap: "0.5rem",
                                flexWrap: "wrap",
                              }}
                            >
                              {!ev?.isApproved && (
                                <>
                                  <button
                                    className="admin-edit-btn"
                                    style={{
                                      background: "rgba(34,197,94,0.15)",
                                      color: "#22c55e",
                                    }}
                                    onClick={() => handleApproveEvent(ev.id)}
                                    title="Approve Event"
                                  >
                                    <svg
                                      viewBox="0 0 24 24"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                      width="16"
                                      height="16"
                                    >
                                      <path d="M9 11l3 3L22 4" />
                                      <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
                                    </svg>
                                    Approve
                                  </button>
                                  <button
                                    className="admin-edit-btn"
                                    style={{
                                      background: "rgba(239,68,68,0.15)",
                                      color: "#ef4444",
                                    }}
                                    onClick={() => handleRejectEvent(ev.id)}
                                    title="Reject Event"
                                  >
                                    <svg
                                      viewBox="0 0 24 24"
                                      fill="none"
                                      stroke="currentColor"
                                      strokeWidth="2"
                                      width="16"
                                      height="16"
                                    >
                                      <circle cx="12" cy="12" r="10" />
                                      <line x1="15" y1="9" x2="9" y2="15" />
                                      <line x1="9" y1="9" x2="15" y2="15" />
                                    </svg>
                                    Reject
                                  </button>
                                </>
                              )}
                              <button
                                className="admin-edit-btn"
                                onClick={() =>
                                  navigate(`/dashboard/manage-event/${ev.id}`, {
                                    state: { event: ev },
                                  })
                                }
                                title="Edit Event"
                              >
                                <svg
                                  viewBox="0 0 24 24"
                                  fill="none"
                                  stroke="currentColor"
                                  strokeWidth="2"
                                  width="16"
                                  height="16"
                                >
                                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                                  <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                                </svg>
                                Edit
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="admin-pagination">
                  <button
                    className="admin-pagination__btn"
                    disabled={safePage === 0}
                    onClick={() => setEventsPage((p) => Math.max(0, p - 1))}
                  >
                    ‹ Previous
                  </button>
                  <span className="admin-pagination__info">
                    Page {safePage + 1} of {totalPgs}
                    {q && (
                      <>
                        {" "}
                        &middot; {filtered.length} result
                        {filtered.length !== 1 ? "s" : ""}
                      </>
                    )}
                  </span>
                  <button
                    className="admin-pagination__btn"
                    disabled={safePage + 1 >= totalPgs}
                    onClick={() => setEventsPage((p) => p + 1)}
                  >
                    Next ›
                  </button>
                </div>
              </>
            ) : (
              <p className="admin-empty">
                {q
                  ? `No events matching "${eventSearch}".`
                  : "No events found."}
              </p>
            )}
          </section>
        );
      }
      case "analytics":
        return (
          <>
            <EngagementPanel />
            <section className="admin-card">
              <h2 className="admin-card__title">System Analytics</h2>
              {systemAnalytics.length > 0 ? (
                <div className="admin-chart-wrap">
                  <ResponsiveContainer width="100%" height={300}>
                    <AreaChart data={systemAnalytics}>
                      <defs>
                        <linearGradient
                          id="adGradUsers"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="5%"
                            stopColor="#8b5cf6"
                            stopOpacity={0.3}
                          />
                          <stop
                            offset="95%"
                            stopColor="#8b5cf6"
                            stopOpacity={0}
                          />
                        </linearGradient>
                        <linearGradient
                          id="adGradEvents"
                          x1="0"
                          y1="0"
                          x2="0"
                          y2="1"
                        >
                          <stop
                            offset="5%"
                            stopColor="#06b6d4"
                            stopOpacity={0.3}
                          />
                          <stop
                            offset="95%"
                            stopColor="#06b6d4"
                            stopOpacity={0}
                          />
                        </linearGradient>
                      </defs>
                      <CartesianGrid
                        strokeDasharray="3 3"
                        stroke="rgba(148,163,184,0.08)"
                      />
                      <XAxis
                        dataKey="month"
                        stroke="#64748b"
                        fontSize={12}
                        tickLine={false}
                        axisLine={false}
                      />
                      <YAxis
                        stroke="#64748b"
                        fontSize={12}
                        tickLine={false}
                        axisLine={false}
                      />
                      <Tooltip
                        contentStyle={{
                          background: "#1e1b4b",
                          border: "1px solid rgba(139,92,246,0.3)",
                          borderRadius: 8,
                          color: "#e2e8f0",
                        }}
                      />
                      <Area
                        type="monotone"
                        dataKey="users"
                        name="New users"
                        stroke="#8b5cf6"
                        fill="url(#adGradUsers)"
                        strokeWidth={2}
                      />
                      <Area
                        type="monotone"
                        dataKey="events"
                        name="Events"
                        stroke="#06b6d4"
                        fill="url(#adGradEvents)"
                        strokeWidth={2}
                      />
                      <Legend wrapperStyle={{ fontSize: 12 }} />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <p className="admin-empty">No system analytics data yet.</p>
              )}
            </section>

            <section className="admin-card">
              <h2 className="admin-card__title">User Role Distribution</h2>
              {roleDistribution.length > 0 ? (
                <div className="admin-chart-wrap">
                  <ResponsiveContainer width="100%" height={300}>
                    <PieChart>
                      <Pie
                        data={roleDistribution}
                        dataKey="count"
                        nameKey="role"
                        cx="50%"
                        cy="50%"
                        outerRadius={110}
                        innerRadius={55}
                        paddingAngle={4}
                        label={({ role }) => role}
                      >
                        {roleDistribution.map((_, idx) => (
                          <Cell
                            key={idx}
                            fill={PIE_COLORS[idx % PIE_COLORS.length]}
                          />
                        ))}
                      </Pie>
                      <Tooltip
                        contentStyle={{
                          background: "#1e1b4b",
                          border: "1px solid rgba(139,92,246,0.3)",
                          borderRadius: 8,
                          color: "#e2e8f0",
                        }}
                      />
                      <Legend
                        wrapperStyle={{ color: "#94a3b8", fontSize: 12 }}
                      />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <p className="admin-empty">No role distribution data yet.</p>
              )}
            </section>
          </>
        );

      case "reports":
        return <StudentReports />;

      case "onboard-organizer": {
        const orgRequests = onboardRequests.filter(
          (r) => r.roleType === "ORGANIZER",
        );
        return (
          <section className="admin-card">
            <h2 className="admin-card__title">Pending Organizer Requests</h2>
            {onboardLoading ? (
              <p className="admin-empty">Loading requests…</p>
            ) : orgRequests.length > 0 ? (
              <div className="admin-table-wrap">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Role Type</th>
                      <th>Email</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {orgRequests.map((req, i) => {
                      const rid = req.id ?? req.userId ?? req.user_id;
                      const remail =
                        req.username ?? req.email ?? req.userName ?? "";
                      return (
                        <tr key={rid ?? i}>
                          <td>{rid ?? "--"}</td>
                          <td>
                            <span
                              className={`admin-badge admin-badge--${(req.roleType || "").toLowerCase()}`}
                            >
                              {req.roleType ?? "--"}
                            </span>
                          </td>
                          <td>{remail || "--"}</td>
                          <td>
                            <button
                              className="admin-accept-btn"
                              disabled={acceptingId === rid}
                              onClick={() => handleAcceptOrganizer(remail, rid)}
                            >
                              {acceptingId === rid
                                ? "Accepting…"
                                : "Accept Organizer"}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="admin-empty">No pending organizer requests.</p>
            )}
          </section>
        );
      }

      case "onboard-admin": {
        const adminRequests = onboardRequests.filter(
          (r) => r.roleType === "ADMIN",
        );
        return (
          <section className="admin-card">
            <h2 className="admin-card__title">Pending Admin Requests</h2>
            {onboardLoading ? (
              <p className="admin-empty">Loading requests…</p>
            ) : adminRequests.length > 0 ? (
              <div className="admin-table-wrap">
                <table className="admin-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Role Type</th>
                      <th>Email</th>
                      <th>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {adminRequests.map((req, i) => {
                      const rid = req.id ?? req.userId ?? req.user_id;
                      const remail =
                        req.username ?? req.email ?? req.userName ?? "";
                      return (
                        <tr key={rid ?? i}>
                          <td>{rid ?? "--"}</td>
                          <td>
                            <span
                              className={`admin-badge admin-badge--${(req.roleType || "").toLowerCase()}`}
                            >
                              {req.roleType ?? "--"}
                            </span>
                          </td>
                          <td>{remail || "--"}</td>
                          <td>
                            <button
                              className="admin-accept-btn admin-accept-btn--admin"
                              disabled={acceptingId === rid}
                              onClick={() => handleAcceptAdmin(remail, rid)}
                            >
                              {acceptingId === rid
                                ? "Accepting…"
                                : "Accept Admin"}
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="admin-empty">No pending admin requests.</p>
            )}
          </section>
        );
      }

      case "profile-config":
        return (
          <>
            {/* Section sub-tabs */}
            <div className="pc-tabs">
              {PC_SECTIONS.map((s) => (
                <button
                  key={s.key}
                  className={`pc-tabs__btn ${pcSection === s.key ? "pc-tabs__btn--active" : ""}`}
                  onClick={() => setPcSection(s.key)}
                >
                  {s.label}
                </button>
              ))}
            </div>

            <section className="admin-card">
              <div className="admin-card__header">
                <h2 className="admin-card__title">
                  {PC_SECTIONS.find((s) => s.key === pcSection)?.label}
                </h2>
                <div className="pc-actions">
                  <button
                    className="pc-btn pc-btn--primary"
                    onClick={handlePcOpenCreate}
                  >
                    + Create
                  </button>
                  <button
                    className="pc-btn pc-btn--secondary"
                    onClick={handlePcLookup}
                    disabled={pcSaving}
                  >
                    Refresh List
                  </button>
                </div>
              </div>

              <div
                style={{
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
                  gap: "0.9rem",
                  marginBottom: "1.25rem",
                }}
              >
                {PC_SECTIONS.map((section) => {
                  const isActive = section.key === pcSection;
                  return (
                    <button
                      key={section.key}
                      type="button"
                      onClick={() => setPcSection(section.key)}
                      style={{
                        textAlign: "left",
                        borderRadius: "14px",
                        border: isActive
                          ? "1px solid rgba(59,130,246,0.45)"
                          : "1px solid rgba(148,163,184,0.18)",
                        background: isActive
                          ? "rgba(59,130,246,0.12)"
                          : "rgba(15,23,42,0.55)",
                        padding: "1rem",
                        color: "#e2e8f0",
                      }}
                    >
                      <div style={{ fontWeight: 700, marginBottom: "0.35rem" }}>
                        {section.label}
                      </div>
                      <div
                        style={{
                          fontSize: "0.82rem",
                          color: "#94a3b8",
                          lineHeight: 1.5,
                        }}
                      >
                        {section.description}
                      </div>
                      <div
                        style={{
                          marginTop: "0.6rem",
                          fontSize: "0.78rem",
                          color: isActive ? "#93c5fd" : "#cbd5e1",
                        }}
                      >
                        {isActive
                          ? `${pcItems.length} item${pcItems.length !== 1 ? "s" : ""} loaded`
                          : "Open section"}
                      </div>
                    </button>
                  );
                })}
              </div>

              {pcError && <p className="pc-error">{pcError}</p>}

              {pcLoading ? (
                <p className="admin-empty">Loading…</p>
              ) : pcItems.length > 0 ? (
                <div className="admin-table-wrap">
                  <table className="admin-table">
                    <thead>
                      <tr>
                        {PC_COLUMNS[pcSection].map((col) => (
                          <th key={col}>
                            {col.charAt(0).toUpperCase() + col.slice(1)}
                          </th>
                        ))}
                        <th>Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pcItems.map((item, idx) => {
                        const itemId = item[PC_ID_KEY[pcSection]];
                        return (
                          <tr key={itemId ?? idx}>
                            {PC_COLUMNS[pcSection].map((col) => (
                              <td key={col}>
                                {typeof item[col] === "string" &&
                                item[col].length > 60
                                  ? item[col].substring(0, 60) + "…"
                                  : (item[col] ?? "--")}
                              </td>
                            ))}
                            <td className="pc-action-cell">
                              <button
                                className="pc-btn pc-btn--edit"
                                onClick={() => handlePcOpenEdit(itemId)}
                                title="Edit"
                              >
                                <svg
                                  viewBox="0 0 24 24"
                                  fill="none"
                                  stroke="currentColor"
                                  strokeWidth="2"
                                  width="14"
                                  height="14"
                                >
                                  <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                                  <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                                </svg>
                              </button>
                              <button
                                className="pc-btn pc-btn--delete"
                                onClick={() => handlePcDelete(itemId)}
                                title="Delete"
                              >
                                <svg
                                  viewBox="0 0 24 24"
                                  fill="none"
                                  stroke="currentColor"
                                  strokeWidth="2"
                                  width="14"
                                  height="14"
                                >
                                  <polyline points="3 6 5 6 21 6" />
                                  <path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" />
                                  <path d="M10 11v6" />
                                  <path d="M14 11v6" />
                                  <path d="M9 6V3h6v3" />
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
                <p className="admin-empty">No items found for this section yet.</p>
              )}
            </section>

            {/* Create / Edit Modal */}
            {pcModal && (
              <div className="pc-overlay" onClick={() => setPcModal(null)}>
                <div className="pc-modal" onClick={(e) => e.stopPropagation()}>
                  <div className="pc-modal__header">
                    <h3 className="pc-modal__title">
                      {pcModal === "edit" ? "Edit" : "Create"}{" "}
                      {PC_SECTIONS.find(
                        (s) => s.key === pcSection,
                      )?.label.replace(/s$/, "")}
                    </h3>
                    <button
                      className="pc-modal__close"
                      onClick={() => setPcModal(null)}
                    >
                      &times;
                    </button>
                  </div>
                  <form onSubmit={handlePcSave} className="pc-modal__body">
                    {PC_FIELDS[pcSection].map((f) => (
                      <div key={f.key} className="pc-field">
                        <label className="pc-field__label">
                          {f.label}
                          {f.required && " *"}
                        </label>
                        {f.textarea ? (
                          <textarea
                            className="pc-field__input pc-field__textarea"
                            value={pcForm[f.key] || ""}
                            onChange={(e) =>
                              setPcForm((prev) => ({
                                ...prev,
                                [f.key]: e.target.value,
                              }))
                            }
                            rows={3}
                            required={f.required}
                          />
                        ) : (
                          <input
                            className="pc-field__input"
                            type={f.type || "text"}
                            value={pcForm[f.key] || ""}
                            onChange={(e) =>
                              setPcForm((prev) => ({
                                ...prev,
                                [f.key]:
                                  f.type === "number"
                                    ? Number(e.target.value)
                                    : e.target.value,
                              }))
                            }
                            required={f.required}
                          />
                        )}
                      </div>
                    ))}
                    {pcError && <p className="pc-error">{pcError}</p>}
                    <div className="pc-modal__footer">
                      <button
                        type="button"
                        className="pc-btn pc-btn--secondary"
                        onClick={() => setPcModal(null)}
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        className="pc-btn pc-btn--primary"
                        disabled={pcSaving}
                      >
                        {pcSaving
                          ? "Saving…"
                          : pcModal === "edit"
                            ? "Update"
                            : "Create"}
                      </button>
                    </div>
                  </form>
                </div>
              </div>
            )}
          </>
        );

      default:
        return null;
    }
  };

  return (
    <div className="admin-page">
      <Navbar />
      <div className="admin-layout">
        <aside className="admin-sidebar">
          <div className="admin-sidebar__profile">
            <div className="admin-sidebar__avatar">{initials}</div>
            <h3 className="admin-sidebar__name">{fullName}</h3>
            <p className="admin-sidebar__role">Administrator</p>
          </div>
          <nav className="admin-sidebar__nav">
            {SIDEBAR_TABS.map((tab) => (
              <button
                key={tab.key}
                className={`admin-sidebar__tab ${activeTab === tab.key ? "admin-sidebar__tab--active" : ""}`}
                onClick={() => setActiveTab(tab.key)}
              >
                <TabIcon type={tab.icon} />
                {tab.label}
              </button>
            ))}
          </nav>
        </aside>

        <main className="admin-content" data-tab={activeTab}>
          <section className="admin-welcome">
            <div className="admin-welcome__text">
              <h1 className="admin-welcome__title">Admin Dashboard</h1>
              <p className="admin-welcome__sub">
                Platform overview &amp; management
              </p>
            </div>
          </section>
          <div key={activeTab} className="tab-fade">{renderContent()}</div>
        </main>
      </div>
    </div>
  );
}
