import { useEffect, useRef, useState } from "react";
import api, { getAccessToken } from "../../api/apiClient";
import "../../styles/Engagement.css";

/**
 * Share an event: the phone's share sheet where available, otherwise WhatsApp and Copy link.
 * The link shows a rich preview (title, date, cover) in WhatsApp and other apps.
 */
export default function ShareButton({ event }) {
  const [open, setOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const wrap = useRef(null);
  const id = event.id ?? event.eventId;
  const url = `${window.location.origin}/events/${id}`;
  const when = event.eventDate ? new Date(event.eventDate).toLocaleDateString(undefined, { day: "numeric", month: "short" }) : "";
  const text = `${event.name}${when ? ` (${when})` : ""}${event.venue ? ` at ${event.venue}` : ""}. Join me?`;

  useEffect(() => {
    if (!open) return undefined;
    const close = (e) => { if (wrap.current && !wrap.current.contains(e.target)) setOpen(false); };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, [open]);

  const count = () => { if (getAccessToken()) api.post(`/events/${id}/shared`).catch(() => {}); };

  const share = async () => {
    if (navigator.share) {
      try {
        await navigator.share({ title: event.name, text, url });
        count();
      } catch { /* closed the share sheet */ }
      return;
    }
    setOpen((o) => !o);
  };

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(url);
    } catch {
      const input = document.createElement("input");
      input.value = url;
      document.body.appendChild(input);
      input.select();
      document.execCommand("copy");
      input.remove();
    }
    setCopied(true);
    count();
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="share" ref={wrap}>
      <button type="button" className="save-btn save-btn--full share__btn" aria-expanded={open} onClick={share}>
        <svg viewBox="0 0 24 24" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" />
          <path d="M8.6 13.5l6.8 4M15.4 6.5l-6.8 4" />
        </svg>
        <span>Share</span>
      </button>
      {open && (
        <div className="share__menu" role="menu">
          <a role="menuitem" href={`https://wa.me/?text=${encodeURIComponent(`${text} ${url}`)}`} target="_blank" rel="noopener noreferrer" onClick={() => { count(); setOpen(false); }}>
            Share on WhatsApp
          </a>
          <button type="button" role="menuitem" onClick={copy}>{copied ? "Link copied" : "Copy link"}</button>
        </div>
      )}
    </div>
  );
}
