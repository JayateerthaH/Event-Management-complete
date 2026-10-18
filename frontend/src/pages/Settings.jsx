import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import Navbar from "../components/Navbar";
import CalendarSubscription from "../components/engagement/CalendarSubscription";
import PortfolioSettings from "../components/engagement/PortfolioSettings";
import PushSettings from "../components/engagement/PushSettings";
import "../styles/Engagement.css";
import { apiErrorMessage } from "../api/apiClient";
import { getNotificationSettings, updateNotificationSettings } from "../api/socialService";
import "../styles/Social.css";
import "../styles/Settings.css";

const GROUPS = [
  {
    title: "Friends",
    options: [
      { key: "friendRequests", label: "Friend requests", hint: "When someone says hi or accepts your request" },
      { key: "friendGoing", label: "Friends going to events", hint: "When a friend registers for an event" },
    ],
  },
  {
    title: "Events",
    options: [
      { key: "newEvents", label: "New events from organizers you follow" },
      { key: "eventReminders", label: "Reminders for saved events", hint: "The day before, when one is almost full, recaps and feedback asks" },
      { key: "badges", label: "Badges", hint: "When you earn a new badge" },
    ],
  },
  {
    title: "Digest",
    options: [
      { key: "weeklyDigest", label: "Weekly digest", hint: "A short summary of the week's events for you, every Monday" },
      { key: "digestEmail", label: "Also email me the weekly digest", needs: "weeklyDigest" },
    ],
  },
];

const SECTIONS = [
  ["notifications", "Notifications"],
  ["push", "Phone"],
  ["calendar", "Calendar"],
  ["portfolio", "Portfolio"],
  ["privacy", "Privacy"],
];

/** Settings: notifications, phone push, calendar feed, portfolio and privacy. Announcements for registered events always arrive. */
export default function Settings() {
  const [prefs, setPrefs] = useState(null);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);
  const savedTimer = useRef(null);

  useEffect(() => {
    getNotificationSettings().then(setPrefs).catch((e) => setError(apiErrorMessage(e)));
    return () => clearTimeout(savedTimer.current);
  }, []);

  const toggle = async (key, value) => {
    setError("");
    const before = prefs;
    setPrefs({ ...prefs, [key]: value });
    try {
      setPrefs(await updateNotificationSettings({ [key]: value }));
      setSaved(true);
      clearTimeout(savedTimer.current);
      savedTimer.current = setTimeout(() => setSaved(false), 2000);
    } catch (err) {
      setPrefs(before);
      setError(apiErrorMessage(err));
    }
  };

  return (
    <div className="social-page settings-page">
      <Navbar />
      <main className="social-wrap settings-wrap">
        <header className="social-head">
          <h1>Settings</h1>
          <p>Notifications, calendar, portfolio and privacy, all in one place.</p>
        </header>
        <nav className="settings-jump" aria-label="Settings sections">
          {SECTIONS.map(([id, label]) => (
            <a key={id} href={`#${id}`} onClick={(e) => {
              e.preventDefault();
              document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
              history.replaceState(history.state, "", `#${id}`);
            }}>{label}</a>
          ))}
        </nav>

        <section className="social-form" id="notifications" aria-labelledby="notif-title">
          <div className="settings-title">
            <h2 className="social-h2" id="notif-title">Notifications</h2>
            <span className={`settings-saved ${saved ? "is-on" : ""}`} role="status">{saved ? "Saved" : ""}</span>
          </div>
          <p className="social-muted">What shows up in your bell, and on your phone if you turn that on below.</p>
          {!prefs ? (
            error ? <p className="social-error" role="alert">{error}</p> : <div className="settings-skeleton" aria-label="Loading" />
          ) : (
            <>
              {GROUPS.map((g) => (
                <fieldset key={g.title} className="settings-group">
                  <legend>{g.title}</legend>
                  {g.options.map((o) => (
                    <label key={o.key} className={`social-check ${o.needs && !prefs[o.needs] ? "is-disabled" : ""}`} htmlFor={`pref-${o.key}`}>
                      <input id={`pref-${o.key}`} type="checkbox" role="switch" checked={!!prefs[o.key]}
                        disabled={!!o.needs && !prefs[o.needs]} onChange={(e) => toggle(o.key, e.target.checked)} />
                      <span><b>{o.label}</b>{o.hint && <small>{o.hint}</small>}</span>
                    </label>
                  ))}
                </fieldset>
              ))}
              <p className="social-note">Announcements from organizers of events you registered for always arrive.</p>
              {error && <p className="social-error" role="alert">{error}</p>}
            </>
          )}
        </section>
        <PushSettings />
        <CalendarSubscription />
        <PortfolioSettings />
        <section className="social-form" id="privacy" aria-labelledby="privacy-title">
          <h2 className="social-h2" id="privacy-title">Privacy</h2>
          <p className="social-muted">Who can find you, what friends see and who you've blocked.</p>
          <Link to="/friends?tab=settings" className="pc-btn pc-btn--ghost settings-linkbtn">Open privacy settings</Link>
        </section>
      </main>
    </div>
  );
}
