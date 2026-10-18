import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/apiClient";

const LABELS = {
  active: "Active days", login: "Logins", signup: "Signups", event_registered: "Registrations",
  attended: "Check-ins", event_saved: "Events saved", interests_picked: "Interests picked",
  buddy_opt_in: "Buddy opt-ins", buddy_request: "Buddy requests", buddy_accepted: "Buddies accepted",
  event_duplicated: "Events duplicated", event_shared: "Events shared", organizer_followed: "Organizers followed",
  waitlist_joined: "Waitlist joins", seat_released: "Seats released", portfolio_published: "Portfolios published",
  goal_set: "Semester goals set", wrapped_viewed: "Wrapped views", wrapped_shared: "Wrapped shares",
  push_enabled: "Phone notifications on", push_disabled: "Phone notifications off",
};
const mins = (m) => (m == null ? "—" : m < 1 ? "< 1 min" : m < 120 ? `${Math.round(m)} min` : `${Math.round(m / 60)} h`);
const num = (v) => (v == null ? "—" : v.toFixed(v < 10 ? 2 : 0));
const OPT_OUT = { friend_requests: "Friend requests", friend_going: "Friend is going", new_events: "New events", badges: "Badges", weekly_digest: "Weekly digest" };
const pct = (v) => (v == null ? "—" : `${Math.round(v * 100)}%`);

/** Admin Analytics: weekly/monthly active users, week-1 retention and what people do. */
export default function EngagementPanel() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [digest, setDigest] = useState("");
  const [sending, setSending] = useState(false);

  const sendDigest = async () => {
    setSending(true);
    setDigest("");
    try {
      const { data: r } = await api.post("/admin/digest/run");
      setDigest(`Sent ${r.sent} digest${r.sent === 1 ? "" : "s"} (${r.emailed} by email). Skipped: ${r.skippedNothingNew} with nothing this week, `
        + `${r.skippedAlreadySent} already sent this week, ${r.skippedTurnedOff} turned off.`);
    } catch (e) {
      setDigest(apiErrorMessage(e));
    } finally {
      setSending(false);
    }
  };

  const [health, setHealth] = useState(null);

  useEffect(() => {
    api.get("/admin/engagement").then((r) => setData(r.data)).catch((e) => setError(apiErrorMessage(e)));
    api.get("/admin/engagement/health").then((r) => setHealth(r.data)).catch(() => setHealth(null));
  }, []);

  if (error) return <section className="admin-card"><h2 className="admin-card__title">Engagement</h2><p className="admin-empty">{error}</p></section>;
  if (!data) return <section className="admin-card"><h2 className="admin-card__title">Engagement</h2><p className="admin-empty">Loading…</p></section>;

  const series = data.dailyActiveLast30Days || [];
  const max = Math.max(1, ...series.map((d) => d.users));
  const W = 600, H = 120, gap = 2, bw = W / Math.max(series.length, 1) - gap;
  const counts = Object.entries(data.countsLast30Days || {}).sort((a, b) => b[1] - a[1]);

  return (
    <section className="admin-card engagement" aria-labelledby="eng-title">
      <h2 className="admin-card__title" id="eng-title">Engagement</h2>
      <p className="engagement__since">
        {data.trackingSince
          ? `Counting since ${new Date(data.trackingSince).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" })}.`
          : "Nothing counted yet. Numbers appear as people use the app."}
      </p>
      <div className="engagement__tiles">
        <div><span className="engagement__value">{data.dailyActive}</span><span className="engagement__label">Active today</span></div>
        <div><span className="engagement__value">{data.weeklyActive}</span><span className="engagement__label">Active this week</span></div>
        <div><span className="engagement__value">{data.monthlyActive}</span><span className="engagement__label">Active in 30 days</span></div>
        <div>
          <span className="engagement__value">{pct(data.weeklyOverMonthly)}</span>
          <span className="engagement__label">Weekly ÷ monthly (habit)</span>
        </div>
        <div>
          <span className="engagement__value">{pct(data.week1Retention)}</span>
          <span className="engagement__label">
            Back in week 2{data.retentionCohort ? ` (of ${data.retentionCohort} signups)` : " (needs 2+ weeks of signups)"}
          </span>
        </div>
      </div>

      <h3 className="engagement__h3">Daily active users, last 30 days</h3>
      <div className="engagement__chart">
        <svg viewBox={`0 0 ${W} ${H + 18}`} role="img" aria-label={`Daily active users, peak ${max}`} preserveAspectRatio="none">
          <line x1="0" y1={H} x2={W} y2={H} stroke="currentColor" strokeOpacity="0.2" />
          {series.map((d, i) => {
            const h = (d.users / max) * (H - 8);
            return (
              <rect key={d.day} x={i * (bw + gap)} y={H - h} width={bw} height={Math.max(h, d.users ? 2 : 0)} rx="2" fill="#8b5cf6">
                <title>{`${d.day}: ${d.users}`}</title>
              </rect>
            );
          })}
          <text x="0" y={H + 14} fill="currentColor" fillOpacity="0.6" fontSize="11">{series[0]?.day}</text>
          <text x={W} y={H + 14} fill="currentColor" fillOpacity="0.6" fontSize="11" textAnchor="end">today · peak {max}</text>
        </svg>
      </div>

      <div className="engagement__digest">
        <div>
          <h3 className="engagement__h3">Weekly digest</h3>
          <p className="engagement__since">Goes out automatically on Mondays at 9:00. Each student gets it at most once a week.</p>
        </div>
        <button type="button" className="admin-accept-btn" onClick={sendDigest} disabled={sending}>
          {sending ? "Sending…" : "Send this week's digest now"}
        </button>
      </div>
      {digest && <p className="engagement__since" role="status">{digest}</p>}

      {health && (
        <>
          <h3 className="engagement__h3">Retention and health</h3>
          <div className="engagement__tiles">
            {[["Day 1", health.day1], ["Day 7", health.day7], ["Day 30", health.day30]].map(([label, r]) => (
              <div key={label}>
                <span className="engagement__value">{pct(r.rate)}</span>
                <span className="engagement__label">{label} retention{r.cohort ? ` (of ${r.cohort})` : " (not enough signups yet)"}</span>
              </div>
            ))}
            <div><span className="engagement__value">{mins(health.medianMinutesToFirstRegistration)}</span><span className="engagement__label">Signup to first registration (median, target under 2 min)</span></div>
            <div><span className="engagement__value">{pct(health.firstHourRegistrationShare)}</span><span className="engagement__label">Register within an hour of signing up</span></div>
            <div><span className="engagement__value">{pct(health.showUp.rate)}</span><span className="engagement__label">Show-up rate (QR check-ins{health.showUp.cohort ? `, ${health.showUp.cohort} events` : ""}); no-shows {health.showUp.rate == null ? "—" : pct(1 - health.showUp.rate)}</span></div>
            <div><span className="engagement__value">{pct(health.feedbackRate)}</span><span className="engagement__label">Attendees who gave feedback</span></div>
            <div><span className="engagement__value">{pct(health.organizerRepeat.rate)}</span><span className="engagement__label">Organizers with another event within 30 days{health.organizerRepeat.cohort ? ` (of ${health.organizerRepeat.cohort})` : ""}</span></div>
          </div>
          <ul className="engagement__counts">
            <li><span>Follows per active user</span><b>{num(health.followsPerStudent)}</b></li>
            <li><span>Shares per recent event</span><b>{num(health.sharesPerEvent)}</b></li>
            <li><span>Public portfolios</span><b>{health.portfoliosPublished}</b></li>
            <li><span>Semester goals set</span><b>{health.goalsSet}</b></li>
            <li><span>Devices with phone notifications</span><b>{health.pushDevices}</b></li>
            <li><span>Phone notifications turned on / off (30 days)</span><b>{health.pushTurnedOn30} / {health.pushTurnedOff30}</b></li>
            {Object.entries(health.notificationsTurnedOff).map(([k, n]) => (
              <li key={k}><span>Turned off: {OPT_OUT[k] || k}</span><b>{n}</b></li>
            ))}
          </ul>
          <p className="engagement__since">A rising number of people turning notifications off is the warning sign of too many messages.</p>
        </>
      )}

      <h3 className="engagement__h3">What people did, last 30 days</h3>
      {counts.length === 0 ? <p className="admin-empty">No activity yet.</p> : (
        <ul className="engagement__counts">
          {counts.map(([type, n]) => (
            <li key={type}><span>{LABELS[type] || type}</span><b>{n}</b></li>
          ))}
        </ul>
      )}
    </section>
  );
}
