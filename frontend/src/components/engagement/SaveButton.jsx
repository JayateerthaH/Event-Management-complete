import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { apiErrorMessage } from "../../api/apiClient";
import { loadSavedIds, setSaved, useSavedIds } from "../../api/savedService";
import "../../styles/Engagement.css";

/** Bookmark toggle: "Save" / "Saved". Logged-out visitors are sent to login. */
export default function SaveButton({ eventId, variant = "icon", className = "" }) {
  const { user } = useAuth();
  const navigate = useNavigate();
  const savedIds = useSavedIds();
  const [error, setError] = useState("");
  const saved = savedIds.has(Number(eventId));

  useEffect(() => { if (user) loadSavedIds(); }, [user]);

  const toggle = async (e) => {
    e.stopPropagation();
    e.preventDefault();
    if (!user) { navigate("/login"); return; }
    setError("");
    try {
      await setSaved(eventId, !saved);
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  };

  const label = saved ? "Saved" : "Save";
  return (
    <button
      type="button"
      className={`save-btn save-btn--${variant}${saved ? " is-saved" : ""} ${className}`}
      aria-pressed={saved}
      aria-label={saved ? "Remove from saved events" : "Save this event"}
      title={error || (saved ? "Saved. Click to remove" : "Save for later")}
      onClick={toggle}
    >
      <svg viewBox="0 0 24 24" aria-hidden="true" fill={saved ? "currentColor" : "none"} stroke="currentColor" strokeWidth="2" strokeLinejoin="round">
        <path d="M6 3h12a1 1 0 0 1 1 1v17l-7-4.5L5 21V4a1 1 0 0 1 1-1z" />
      </svg>
      {variant === "full" && <span>{label}</span>}
    </button>
  );
}
