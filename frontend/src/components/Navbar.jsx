import { useState, useRef, useEffect, useCallback } from "react";
import { useNavigate, Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { resolveRole } from "../utils/roleResolver";
import { fetchUserAnnouncements } from "../api/announcementService";
import { fetchCollaboratorEvents } from "../services/dashboardService";
import { getBuddyNotifications, markBuddyNotificationsRead } from "../api/socialService";
import "../styles/Home.css";

const getInitials = (name) => {
  if (!name) return "U";
  const parts = name.split(" ");
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return name.substring(0, 2).toUpperCase();
};

const BUDDY_TITLES = {
  BUDDY_REQUEST: "New friend request",
  BUDDY_ACCEPTED: "Request accepted",
  FRIEND_GOING: "Your friend is going",
  ACHIEVEMENT: "Nice work",
  NEW_EVENT: "New event",
  DIGEST: "Your week on campus",
  SEAT_OPENED: "You got a seat",
  EVENT_RECAP: "Thanks for coming",
  SAVED_SOON: "Tomorrow: an event you saved",
  ALMOST_FULL: "Almost full",
  FEEDBACK_ASK: "How was it?",
};

const timeAgo = (dateStr) => {
  if (!dateStr) return "";
  const diff = Date.now() - new Date(dateStr).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return "Just now";
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  return `${days}d ago`;
};

export default function Navbar() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, forceLogout } = useAuth();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [notifLoading, setNotifLoading] = useState(false);
  
  const dropdownRef = useRef(null);
  const notifRef = useRef(null);

  const role = user ? resolveRole(user) : null;
  
  

  // Eagerly check collaborator status on mount (only for students)
  useEffect(() => {
    if (user && role === "STUDENT") {
      fetchCollaboratorEvents()
        .then((/* data */) => {
          // (Array.isArray(data) && data.length > 0);
        })
        .catch(() => {});
    }
  }, [user, role]);

  // Close dropdown when clicking outside
  useEffect(() => {
    const handler = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
      if (notifRef.current && !notifRef.current.contains(e.target)) {
        setNotifOpen(false);
      }
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  // Fetch notifications when panel opens
  const loadNotifications = useCallback(async () => {
    if (!user) return;
    setNotifLoading(true);
    try {
      const [announcementData, buddyData] = await Promise.all([
        fetchUserAnnouncements().catch(() => []),
        getBuddyNotifications().catch(() => []),
      ]);
      const announcements = (Array.isArray(announcementData) ? announcementData : []).map((n) => ({
        key: `a-${n.id}`, title: n.title, message: n.announcement, createdAt: n.createdAt, unread: true,
      }));
      const buddies = (Array.isArray(buddyData) ? buddyData : []).map((n) => ({
        key: `b-${n.id}`, title: BUDDY_TITLES[n.type] || "Friends", message: n.message, createdAt: n.createdAt,
        unread: !n.read,
        to: n.type === "FEEDBACK_ASK" ? "/dashboard?tab=feedback"
          : n.type === "BUDDY_REQUEST" ? "/friends?tab=requests"
          : n.type === "BUDDY_ACCEPTED" ? "/friends"
          : n.eventId ? `/events/${n.eventId}`
          : n.type === "ACHIEVEMENT" || n.type === "DIGEST" ? "/dashboard" : "/friends",
      }));
      setNotifications(
        [...buddies, ...announcements].sort((x, y) => new Date(y.createdAt || 0) - new Date(x.createdAt || 0)),
      );
    } catch {
      setNotifications([]);
    } finally {
      setNotifLoading(false);
    }
  }, [user]);

  // Eagerly load notifications on mount so badge count shows
  useEffect(() => {
    if (user) loadNotifications();
  }, [user, loadNotifications]);

  const toggleNotifPanel = () => {
    const opening = !notifOpen;
    setNotifOpen(opening);
    setDropdownOpen(false);
    if (opening) {
      loadNotifications().then(() => markBuddyNotificationsRead().catch(() => {}));
    } else {
      // Friend notifications seen in the panel stop counting towards the badge.
      setNotifications((list) => list.map((n) => (n.key.startsWith("b-") ? { ...n, unread: false } : n)));
    }
  };

  const badgeCount = notifications.filter((n) => n.unread).length;

  const handleLogout = () => {
    setDropdownOpen(false);
    forceLogout();
  };

  return (
    <nav className={`home-navbar ${user ? "" : "home-navbar--guest"}`}>
      <div className="navbar-left">
        <div className="navbar-brand" onClick={() => navigate("/home")}>
          <svg className="brand-icon" fill="currentColor" viewBox="0 0 20 20">
            <path d="M10.394 2.08a1 1 0 00-.788 0l-7 3a1 1 0 000 1.84L5.25 8.051a.999.999 0 01.356-.257l4-1.714a1 1 0 11.788 1.838L7.667 9.088l1.94.831a1 1 0 00.787 0l7-3a1 1 0 000-1.838l-7-3zM3.31 9.397L5 10.12v4.102a8.969 8.969 0 00-1.05-.174 1 1 0 01-.89-.89 11.115 11.115 0 01.25-3.762zM9.3 16.573A9.026 9.026 0 007 14.935v-3.957l1.818.78a3 3 0 002.364 0l5.508-2.361a11.026 11.026 0 01.25 3.762 1 1 0 01-.89.89 8.968 8.968 0 00-5.35 2.524 1 1 0 01-1.4 0zM6 18a1 1 0 001-1v-2.065a8.935 8.935 0 00-2-.712V17a1 1 0 001 1z" />
          </svg>
          <span className="brand-text">CampusEvents</span>
        </div>
        <div className="navbar-links">
          <Link
            to="/home"
            className={`nav-link ${location.pathname === "/home" ? "active" : ""}`}
          >
            Home
          </Link>
          <Link
            to="/events"
            className={`nav-link ${location.pathname === "/events" ? "active" : ""}`}
          >
            Events
          </Link>
          {user && (
            <>
              <Link
                to="/dashboard"
                className={`nav-link ${location.pathname === "/dashboard" ? "active" : ""}`}
              >
                Dashboard
              </Link>
              <Link
                to="/friends"
                className={`nav-link ${location.pathname === "/friends" ? "active" : ""}`}
              >
                Friends
              </Link>
              <Link
                to="/profile"
                className={`nav-link ${location.pathname === "/profile" ? "active" : ""}`}
              >
                Profile
              </Link>
            </>
          )}
        </div>
      </div>

      <div className="navbar-right">
        <button className="icon-btn" title="Search">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            />
          </svg>
        </button>
        <div className="notif-wrap" ref={notifRef}>
          <button
            className="icon-btn notification-btn"
            title="Notifications"
            onClick={toggleNotifPanel}
          >
            <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
              />
            </svg>
            {badgeCount > 0 && (
              <span className="notification-badge">
                {badgeCount > 99 ? "99+" : badgeCount}
              </span>
            )}
          </button>

          {notifOpen && (
            <div className="notif-panel">
              <div className="notif-panel__header">
                <h3 className="notif-panel__title">Notifications</h3>
                {notifications.length > 0 && (
                  <span className="notif-panel__count">
                    {notifications.length}
                  </span>
                )}
              </div>

              {/* QR Scanner removed — per-event scan buttons are in the dashboard */}

              <div className="notif-panel__divider" />

              <div className="notif-panel__list">
                {notifLoading ? (
                  <div className="notif-panel__loading">
                    <div className="notif-panel__spinner" />
                    <span>Loading…</span>
                  </div>
                ) : notifications.length === 0 ? (
                  <div className="notif-panel__empty">
                    <svg
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.5"
                    >
                      <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9" />
                      <path d="M13.73 21a2 2 0 01-3.46 0" />
                    </svg>
                    <p>No notifications yet</p>
                  </div>
                ) : (
                  notifications.map((n) => (
                    <div
                      key={n.key}
                      className={`notif-item${n.to ? " notif-item--link" : ""}${n.unread && n.to ? " notif-item--unread" : ""}`}
                      {...(n.to && {
                        role: "link",
                        tabIndex: 0,
                        onClick: () => { setNotifOpen(false); navigate(n.to); },
                        onKeyDown: (e) => { if (e.key === "Enter") { setNotifOpen(false); navigate(n.to); } },
                      })}
                    >
                      <div className="notif-item__dot" />
                      <div className="notif-item__body">
                        <p className="notif-item__title">{n.title}</p>
                        <p className="notif-item__msg">{n.message}</p>
                        {n.createdAt && (
                          <span className="notif-item__time">
                            {timeAgo(n.createdAt)}
                          </span>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>
        <button className="icon-btn" title="Settings" onClick={() => navigate(user ? "/settings" : "/login")}>
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
            />
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
            />
          </svg>
        </button>
        {user ? (
          <div className="nav-avatar-wrap" ref={dropdownRef}>
            <div
              className="user-avatar"
              onClick={() => {
                setDropdownOpen((o) => !o);
                setNotifOpen(false);
              }}
              title="Account"
            >
              {user.picture ? (
                <img
                  src={user.picture}
                  alt={user.name}
                  referrerPolicy="no-referrer"
                />
              ) : (
                <span>{getInitials(user.name || user.email)}</span>
              )}
            </div>

            {dropdownOpen && (
              <div className="nav-dropdown">
                <div className="nav-dropdown__info">
                  <span className="nav-dropdown__name">
                    {user.name || user.email}
                  </span>
                  {user.email && (
                    <span className="nav-dropdown__email">{user.email}</span>
                  )}
                </div>
                <div className="nav-dropdown__divider" />
                <button
                  className="nav-dropdown__item"
                  onClick={() => {
                    setDropdownOpen(false);
                    navigate("/dashboard");
                  }}
                >
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
                  Dashboard
                </button>
                <button
                  className="nav-dropdown__item"
                  onClick={() => {
                    setDropdownOpen(false);
                    navigate("/profile");
                  }}
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" />
                    <circle cx="12" cy="7" r="4" />
                  </svg>
                  Profile
                </button>
                <button
                  className="nav-dropdown__item"
                  onClick={() => {
                    setDropdownOpen(false);
                    navigate("/tickets");
                  }}
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
                    <path d="M14 14h3v3h-3zM20 14v.01M14 20h.01M17 20h3v-3" />
                  </svg>
                  My tickets
                </button>
                <div className="nav-dropdown__divider" />
                <button
                  className="nav-dropdown__item nav-dropdown__item--logout"
                  onClick={handleLogout}
                >
                  <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                  >
                    <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                    <polyline points="16 17 21 12 16 7" />
                    <line x1="21" y1="12" x2="9" y2="12" />
                  </svg>
                  Logout
                </button>
              </div>
            )}
          </div>
        ) : (
          // Logged out: real entry points instead of a placeholder "JD" avatar
          <>
            <Link to="/login" className="nav-link">Login</Link>
            <Link to="/signup" className="nav-link">Sign Up</Link>
          </>
        )}
      </div>
    </nav>
  );
}
