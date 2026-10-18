import { useState } from "react";
import { Link } from "react-router-dom";
import { aiErrorMessage, planMyWeek } from "../../api/aiService";
import SparkleIcon from "./SparkleIcon";
import "../../styles/Ai.css";

const when = (d) => new Date(d).toLocaleString(undefined, { weekday: "short", day: "numeric", month: "short", hour: "numeric", minute: "2-digit" });

/** "Plan my week": what the student is going to in the next 7 days, plus up to 3 picks with reasons. */
export default function PlanMyWeek() {
  const [note, setNote] = useState("");
  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const run = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try { setPlan(await planMyWeek(note.trim())); } catch (err) { setError(aiErrorMessage(err, "Couldn't plan your week right now.")); } finally { setLoading(false); }
  };

  const row = (item, kind) => (
    <li key={`${kind}-${item.event.id}`} className={`plan-week__item plan-week__item--${kind}`}>
      <time>{when(item.event.eventDate)}</time>
      <div>
        <Link to={`/events/${item.event.id}`}>{item.event.name}</Link>
        <small>{item.why}{item.clash && <b className="plan-week__clash"> Clashes with one of your events.</b>}</small>
      </div>
    </li>
  );

  return (
    <section className="ai-panel plan-week" aria-labelledby="plan-week-title">
      <div className="ai-panel__header">
        <div>
          <h2 className="ai-panel__title" id="plan-week-title"><SparkleIcon /> Plan my week</h2>
          <p className="ai-panel__subtitle">Your next 7 days, and what's worth adding.</p>
        </div>
      </div>
      <form className="plan-week__form" onSubmit={run}>
        <label className="sr-only" htmlFor="plan-week-note">Anything to keep in mind?</label>
        <input id="plan-week-note" className="plan-week__input" maxLength={200} value={note} onChange={(e) => setNote(e.target.value)}
          placeholder="Optional: e.g. free after 4 pm, into coding" />
        <button type="submit" className="ai-btn ai-btn--primary" disabled={loading}>
          {loading ? <span className="ai-spinner" /> : plan ? "Plan again" : "Plan my week"}
        </button>
      </form>
      {error && <div className="ai-error">{error}</div>}
      {plan && (
        <div className="plan-week__result" role="status">
          <p className="plan-week__summary">{plan.summary}</p>
          {plan.going.length > 0 && (<><h3>You're going</h3><ol className="plan-week__list">{plan.going.map((i) => row(i, "going"))}</ol></>)}
          {plan.picks.length > 0 && (<><h3>{plan.ai ? "Suggested for you" : "Good fits"}</h3><ol className="plan-week__list">{plan.picks.map((i) => row(i, "pick"))}</ol></>)}
          {plan.going.length === 0 && plan.picks.length === 0 && <p className="ai-note"><Link to="/events">Browse all events</Link></p>}
        </div>
      )}
    </section>
  );
}
