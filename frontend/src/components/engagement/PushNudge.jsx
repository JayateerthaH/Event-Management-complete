import { useEffect, useState } from "react";
import { currentSubscription, enablePush, getPushKey, pushSupported } from "../../api/pushService";
import "../../styles/Engagement.css";

const KEY = "ce_push_nudge_dismissed";

/** Student dashboard: a one-tap prompt for event reminders on this phone, until turned on or dismissed. */
export default function PushNudge() {
  const [show, setShow] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let dismissed = false;
    try { dismissed = localStorage.getItem(KEY) === "1"; } catch { /* storage blocked */ }
    if (dismissed || !pushSupported() || Notification.permission === "denied") return;
    Promise.all([getPushKey(), currentSubscription()])
      .then(([key, sub]) => setShow(key.enabled && !sub))
      .catch(() => {});
  }, []);

  const dismiss = () => {
    try { localStorage.setItem(KEY, "1"); } catch { /* storage blocked */ }
    setShow(false);
  };
  const enable = async () => {
    setError("");
    try { await enablePush(); setShow(false); } catch (e) { setError(e.message || "Couldn't turn on notifications."); }
  };

  if (!show) return null;
  return (
    <section className="push-nudge" aria-label="Phone notifications">
      <p><b>Never miss an event.</b> Get a reminder on this device before events you registered for, and when a seat opens up.</p>
      <div className="push-nudge__actions">
        <button type="button" className="pc-btn pc-btn--primary" onClick={enable}>Turn on reminders</button>
        <button type="button" className="goal__link" onClick={dismiss}>Not now</button>
      </div>
      {error && <p className="social-error" role="alert">{error}</p>}
    </section>
  );
}
