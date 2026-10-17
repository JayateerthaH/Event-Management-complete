import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/apiClient";

/** Settings: subscribe to "my events" in Google/Apple/Outlook calendars; the link can be reset. */
export default function CalendarSubscription() {
  const [url, setUrl] = useState("");
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);

  useEffect(() => {
    api.get("/events/calendar-feed").then((r) => setUrl(r.data.url)).catch((e) => setError(apiErrorMessage(e)));
  }, []);

  const webcal = url.replace(/^https?:\/\//, "webcal://");
  const copy = async () => {
    try { await navigator.clipboard.writeText(url); } catch {
      const el = document.getElementById("cal-url");
      el?.select();
      document.execCommand("copy");
    }
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  const reset = async () => {
    try {
      const { data } = await api.post("/events/calendar-feed/reset");
      setUrl(data.url);
      setConfirmReset(false);
    } catch (e) {
      setError(apiErrorMessage(e));
    }
  };

  return (
    <section className="social-form" aria-labelledby="cal-title">
      <h2 className="social-h2" id="cal-title">Calendar</h2>
      <p className="social-muted">
        Subscribe once and the events you register for or save show up in your phone's calendar, updated automatically.
      </p>
      {error && <p className="social-error" role="alert">{error}</p>}
      {url && (
        <>
          <div className="cal-actions">
            <a className="pc-btn pc-btn--primary" href={`https://calendar.google.com/calendar/r?cid=${encodeURIComponent(webcal)}`} target="_blank" rel="noopener noreferrer">
              Add to Google Calendar
            </a>
            <a className="pc-btn pc-btn--ghost" href={webcal}>Apple / Outlook calendar</a>
          </div>
          <label className="social-label" htmlFor="cal-url">Or copy your calendar link</label>
          <div className="find-bar">
            <input id="cal-url" className="social-input" readOnly value={url} onFocus={(e) => e.target.select()} />
            <button type="button" className="pc-btn pc-btn--ghost" onClick={copy}>{copied ? "Copied" : "Copy"}</button>
          </div>
          <p className="social-note">Anyone with this link can see your event list. If you shared it by mistake, reset it.</p>
          {!confirmReset ? (
            <button type="button" className="seat__link cal-reset" onClick={() => setConfirmReset(true)}>Reset link</button>
          ) : (
            <div className="pc__actions">
              <span className="social-muted">Calendars using the old link stop updating.</span>
              <button type="button" className="pc-btn pc-btn--danger" onClick={reset}>Reset link</button>
              <button type="button" className="pc-btn pc-btn--ghost" onClick={() => setConfirmReset(false)}>Cancel</button>
            </div>
          )}
        </>
      )}
    </section>
  );
}
