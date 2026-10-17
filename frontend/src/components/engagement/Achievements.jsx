import { useEffect, useState } from "react";
import { getAchievements } from "../../api/socialService";
import "../../styles/Engagement.css";

const ICONS = {
  first_event: "M5 13l4 4L19 7",
  planner: "M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z",
  regular: "M17 20h5v-2a3 3 0 00-5.4-1.8M9 20H4v-2a3 3 0 015.4-1.8M15 7a3 3 0 11-6 0 3 3 0 016 0z",
  explorer: "M12 21a9 9 0 100-18 9 9 0 000 18zm3.5-12.5l-2 5-5 2 2-5 5-2z",
  voice: "M8 10h8M8 14h5m-9 6l3-3h10a2 2 0 002-2V6a2 2 0 00-2-2H5a2 2 0 00-2 2v14z",
  certified: "M12 15a5 5 0 100-10 5 5 0 000 10zm-3 0l-1 6 4-2 4 2-1-6",
  connector: "M13.8 10.2a4 4 0 010 5.6l-2 2a4 4 0 01-5.6-5.6l1-1m4.6-1a4 4 0 015.6-5.6 4 4 0 010 5.6l-1 1",
  veteran: "M12 3l2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.4 6.8 19.1l1-5.8L3.5 9.2l5.9-.9L12 3z",
};

/** Student dashboard: badges earned automatically, with progress toward the rest. */
export default function Achievements({ onSummary }) {
  const [summary, setSummary] = useState(null);

  useEffect(() => {
    getAchievements()
      .then((s) => { setSummary(s); onSummary?.(s); })
      .catch(() => setSummary(null));
  }, [onSummary]);

  if (!summary) return null;
  return (
    <section className="sd-card achievements" aria-labelledby="ach-title">
      <div className="sd-card__header">
        <h2 className="sd-card__title" id="ach-title">Badges</h2>
        <span className="achievements__count">{summary.earned} of {summary.total} earned</span>
      </div>
      <ul className="achievements__grid">
        {summary.achievements.map((a) => (
          <li key={a.key} className={`badge-tile${a.earned ? " is-earned" : ""}`}>
            <span className="badge-tile__icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                <path d={ICONS[a.key] || ICONS.veteran} />
              </svg>
            </span>
            <span className="badge-tile__name">{a.name}</span>
            <span className="badge-tile__desc">{a.description}</span>
            {a.earned ? (
              <span className="badge-tile__status">
                Earned{a.earnedAt ? ` ${new Date(a.earnedAt).toLocaleDateString(undefined, { day: "numeric", month: "short" })}` : ""}
              </span>
            ) : (
              <span className="badge-tile__progress" aria-label={`${a.progress} of ${a.target}`}>
                <span className="badge-tile__bar"><span style={{ width: `${(a.progress / a.target) * 100}%` }} /></span>
                <span className="badge-tile__num">{a.progress}/{a.target}</span>
              </span>
            )}
          </li>
        ))}
      </ul>
      <p className="achievements__note">Only check-ins scanned at the event count as attended.</p>
    </section>
  );
}
