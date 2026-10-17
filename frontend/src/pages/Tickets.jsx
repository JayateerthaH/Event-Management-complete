import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import QRCode from "qrcode";
import Navbar from "../components/Navbar";
import InstallAppButton from "../components/engagement/InstallAppButton";
import api, { getAccessToken, getUserData } from "../api/apiClient";
import "../styles/Social.css";
import "../styles/Engagement.css";

const KEY = "ce_tickets_v1";
const fmt = (d) => new Date(d).toLocaleString(undefined, { weekday: "short", day: "numeric", month: "short", hour: "numeric", minute: "2-digit" });

const readCache = (userId) => {
  try {
    const c = JSON.parse(localStorage.getItem(KEY) || "null");
    return c && String(c.userId) === String(userId) ? c : null;
  } catch {
    return null;
  }
};

/**
 * QR tickets that work without signal: kept on this device after the first load, QR drawn on the phone.
 * Deliberately not behind the login redirect, so it opens offline.
 */
export default function Tickets() {
  const user = getUserData();
  const userId = user?.userId ?? user?.id;
  const cached = userId ? readCache(userId) : null;
  const [tickets, setTickets] = useState(cached?.tickets ?? null);
  const [savedAt, setSavedAt] = useState(cached?.savedAt ?? null);
  const [offline, setOffline] = useState(!navigator.onLine);
  const [qr, setQr] = useState({});

  useEffect(() => {
    if (!userId || !getAccessToken()) return;
    api.get("/attendance/my-tickets")
      .then(({ data }) => {
        const list = Array.isArray(data) ? data : [];
        const at = new Date().toISOString();
        setTickets(list);
        setSavedAt(at);
        setOffline(false);
        try { localStorage.setItem(KEY, JSON.stringify({ userId, savedAt: at, tickets: list })); } catch { /* storage full or blocked */ }
      })
      .catch(() => setOffline(true));
  }, [userId]);

  useEffect(() => {
    if (!tickets) return;
    let cancelled = false;
    Promise.all(tickets.map((t) => QRCode.toDataURL(String(t.code), { width: 280, margin: 2 }).then((url) => [t.code, url])))
      .then((pairs) => { if (!cancelled) setQr(Object.fromEntries(pairs)); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [tickets]);

  return (
    <div className="social-page">
      <Navbar />
      <main className="social-wrap">
        <header className="social-head tickets-head">
          <div>
            <h1>My tickets</h1>
            <p>Show the QR code at the entrance. Tickets stay on this phone, so they work without internet.</p>
          </div>
          <InstallAppButton />
        </header>

        {offline && tickets && (
          <p className="tickets-offline" role="status">
            You're offline. Showing tickets saved on this phone{savedAt ? ` (updated ${fmt(savedAt)})` : ""}.
          </p>
        )}

        {!userId ? (
          <div className="social-empty"><h2>Log in to see your tickets</h2><Link className="pc-btn pc-btn--primary" to="/login">Log in</Link></div>
        ) : tickets === null ? (
          <p className="social-muted">{offline ? "No tickets saved on this phone yet. Open this page once while online." : "Loading…"}</p>
        ) : tickets.length === 0 ? (
          <div className="social-empty">
            <h2>No upcoming tickets</h2>
            <p>Register for an event and its QR ticket appears here.</p>
            <Link className="pc-btn pc-btn--primary" to="/events">Browse events</Link>
          </div>
        ) : (
          <ul className="tickets">
            {tickets.map((t) => (
              <li key={`${t.eventId}-${t.code}`} id={`ticket-${t.eventId}`} className="ticket-card">
                <div className="ticket-card__info">
                  <h2>{t.eventName}</h2>
                  <p>{fmt(t.eventDate)}{t.venue ? ` · ${t.venue}` : ""}</p>
                  {t.attended && <span className="pc__tag">Checked in</span>}
                </div>
                <div className="ticket-card__qr">
                  {qr[t.code] ? <img src={qr[t.code]} alt={`QR ticket for ${t.eventName}`} width="220" height="220" /> : <span className="social-muted">Drawing QR…</span>}
                </div>
                <p className="ticket-card__hint">Turn up your screen brightness if the scanner can't read it.</p>
              </li>
            ))}
          </ul>
        )}
      </main>
    </div>
  );
}
