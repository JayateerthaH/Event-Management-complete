import { useEffect, useState } from "react";
import api, { apiErrorMessage } from "../../api/apiClient";

const LABELS = {
  active: "Active days", login: "Logins", signup: "Signups", event_registered: "Registrations",
  attended: "Check-ins", event_saved: "Events saved", interests_picked: "Interests picked",
  buddy_opt_in: "Buddy opt-ins", buddy_request: "Buddy requests", buddy_accepted: "Buddies accepted",
  event_duplicated: "Events duplicated",
};
const pct = (v) => (v == null ? "—" : `${Math.round(v * 100)}%`);

/** Admin Analytics: weekly/monthly active users, week-1 retention and what people do. */
export default function EngagementPanel() {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get("/admin/engagement").then((r) => setData(r.data)).catch((e) => setError(apiErrorMessage(e)));
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
