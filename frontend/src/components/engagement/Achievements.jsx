import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getAchievements } from "../../api/socialService";
import "../../styles/Engagement.css";
import { BADGE_ICONS as ICONS } from "../../utils/badgeIcons";

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
      <p className="achievements__note">
        Only check-ins scanned at the event count as attended. Show them off on your{" "}
        <Link to="/settings#portfolio" className="social-link">portfolio</Link>.
      </p>
    </section>
  );
}
