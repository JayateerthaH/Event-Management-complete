import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Navbar from "../components/Navbar";
import { apiErrorMessage } from "../api/apiClient";
import { getNotificationSettings, updateNotificationSettings } from "../api/socialService";
import "../styles/Social.css";

const OPTIONS = [
  { key: "friendRequests", label: "Friend requests", hint: "When someone says hi or accepts your request" },
  { key: "friendGoing", label: "Friends going to events", hint: "When a friend registers for an event" },
  { key: "newEvents", label: "New events from organizers you follow" },
  { key: "badges", label: "Badges", hint: "When you earn a new badge" },
  { key: "weeklyDigest", label: "Weekly digest", hint: "A short summary of the week's events for you, every Monday" },
];

/** Settings: which notifications to get. Announcements from events you registered for always arrive. */
export default function Settings() {
  const [prefs, setPrefs] = useState(null);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState("");

  useEffect(() => { getNotificationSettings().then(setPrefs).catch((e) => setError(apiErrorMessage(e))); }, []);

  const toggle = async (key, value) => {
    setError("");
    setSaved("");
    const before = prefs;
    setPrefs({ ...prefs, [key]: value });
    try {
      setPrefs(await updateNotificationSettings({ [key]: value }));
      setSaved("Saved");
    } catch (err) {
      setPrefs(before);
      setError(apiErrorMessage(err));
    }
  };

  return (
    <div className="social-page">
      <Navbar />
      <main className="social-wrap">
        <header className="social-head">
          <h1>Settings</h1>
          <p>Choose which notifications you get in the bell.</p>
        </header>
        <section className="social-form" aria-labelledby="notif-title">
          <h2 className="social-h2" id="notif-title">Notifications</h2>
          {!prefs ? (
            error ? <p className="social-error" role="alert">{error}</p> : <p className="social-muted">Loading…</p>
          ) : (
            <>
              {OPTIONS.map((o) => (
                <label key={o.key} className="social-check" htmlFor={`pref-${o.key}`}>
                  <input id={`pref-${o.key}`} type="checkbox" checked={prefs[o.key]} onChange={(e) => toggle(o.key, e.target.checked)} />
                  <span><b>{o.label}</b>{o.hint && <><br /><small className="social-muted">{o.hint}</small></>}</span>
                </label>
              ))}
              <label className="social-check" htmlFor="pref-digestEmail">
                <input id="pref-digestEmail" type="checkbox" checked={prefs.digestEmail} disabled={!prefs.weeklyDigest}
                  onChange={(e) => toggle("digestEmail", e.target.checked)} />
                <span><b>Also email me the weekly digest</b></span>
              </label>
              <p className="social-note">Announcements from organizers of events you registered for always arrive.</p>
              {error && <p className="social-error" role="alert">{error}</p>}
              {saved && <span className="social-saved" role="status">{saved}</span>}
            </>
          )}
        </section>
        <section className="social-form">
          <h2 className="social-h2">Privacy</h2>
          <p className="social-muted">
            Who can find you, what friends see and who you've blocked: <Link to="/friends?tab=settings" className="social-link">Friends settings</Link>.
          </p>
        </section>
      </main>
    </div>
  );
}
