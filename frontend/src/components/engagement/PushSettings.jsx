import { useEffect, useState } from "react";
import { apiErrorMessage } from "../../api/apiClient";
import { currentSubscription, disablePush, enablePush, getPushKey, needsHomeScreenInstall, pushSupported, sendTestPush } from "../../api/pushService";

/** Settings: phone notifications for this device (reminders, seats, friend requests, picks). */
export default function PushSettings() {
  const [state, setState] = useState("loading"); // loading | off-server | unsupported | ios-install | off | on
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    if (!pushSupported()) { setState(needsHomeScreenInstall() ? "ios-install" : "unsupported"); return; }
    Promise.all([getPushKey().catch(() => ({ enabled: false })), currentSubscription().catch(() => null)])
      .then(([key, sub]) => setState(!key.enabled ? "off-server" : sub ? "on" : "off"));
  }, []);

  const run = async (fn, next, done) => {
    setBusy(true); setError(""); setMessage("");
    try { await fn(); if (next) setState(next); if (done) setMessage(done); } catch (e) { setError(e.message && !e.response ? e.message : apiErrorMessage(e)); } finally { setBusy(false); }
  };
  const test = () => run(async () => {
    const { sent } = await sendTestPush();
    if (!sent) throw new Error("Couldn't send a test right now. Try turning notifications off and on.");
  }, null, "Test sent. It should arrive in a few seconds.");

  return (
    <section className="social-form" id="push" aria-labelledby="push-title">
      <h2 className="social-h2" id="push-title">Phone notifications</h2>
      <p className="social-muted">
        Get event reminders, a freed-up seat, friend requests and new events from clubs you follow on this device.
        Suggestions are limited to a few a week and never sent between 10 pm and 8 am.
      </p>
      {state === "loading" && <p className="social-muted">Checking…</p>}
      {state === "off-server" && <p className="social-note">Phone notifications are coming soon.</p>}
      {state === "unsupported" && <p className="social-note">This browser doesn't support notifications. Try Chrome, Edge, Firefox or Safari.</p>}
      {state === "ios-install" && (
        <p className="social-note">On iPhone: tap Share, then <b>Add to Home Screen</b>, open CampusEvents from your Home Screen and turn this on there.</p>
      )}
      {state === "off" && (
        <button type="button" className="pc-btn pc-btn--primary" disabled={busy} onClick={() => run(enablePush, "on", "Notifications are on for this device.")}>
          {busy ? "Turning on…" : "Turn on phone notifications"}
        </button>
      )}
      {state === "on" && (
        <div className="cal-actions">
          <span className="push-on" role="status">On for this device</span>
          <button type="button" className="pc-btn pc-btn--ghost" disabled={busy} onClick={test}>Send a test</button>
          <button type="button" className="pc-btn pc-btn--ghost" disabled={busy} onClick={() => run(disablePush, "off", "Notifications are off for this device.")}>Turn off</button>
        </div>
      )}
      {message && <p className="social-saved" role="status">{message}</p>}
      {error && <p className="social-error" role="alert">{error}</p>}
    </section>
  );
}
