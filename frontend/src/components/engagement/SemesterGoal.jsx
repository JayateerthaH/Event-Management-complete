import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiErrorMessage } from "../../api/apiClient";
import { clearSemesterGoal, getSemesterGoal, setSemesterGoal } from "../../api/socialService";
import "../../styles/Engagement.css";

const CHOICES = [3, 5, 8, 12];
const plural = (n, word) => `${n} ${word}${n === 1 ? "" : "s"}`;
// Weeks until the semester's last day ends in India, worked out when the data arrives
const withWeeksLeft = (g) => ({ ...g, weeksLeft: Math.max(0, Math.ceil((new Date(`${g.end}T23:59:59+05:30`) - Date.now()) / (7 * 86400000))) });

/** Student dashboard: "attend N events this semester". Only check-ins scanned at the event count. */
export default function SemesterGoal() {
  const [goal, setGoal] = useState(null);
  const [editing, setEditing] = useState(false);
  const [custom, setCustom] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    getSemesterGoal().then((g) => setGoal(withWeeksLeft(g))).catch(() => setGoal(null));
  }, []);

  const save = async (call) => {
    setError("");
    try {
      setGoal(withWeeksLeft(await call()));
      setEditing(false);
      setCustom("");
    } catch (e) {
      setError(apiErrorMessage(e));
    }
  };

  if (!goal) return null;
  const picking = editing || goal.target == null;

  return (
    <section className="sd-card goal" aria-labelledby="goal-title">
      <div className="sd-card__header">
        <h2 className="sd-card__title" id="goal-title">Semester goal</h2>
        <span className="achievements__count">{goal.semester} · {plural(goal.weeksLeft, "week")} left</span>
      </div>

      {picking ? (
        <div className="goal__pick">
          <p className="goal__text">
            How many events do you want to attend this semester?
            {goal.lastSemesterAttended > 0 && ` Last semester you attended ${goal.lastSemesterAttended}.`}
          </p>
          <div className="goal__choices" role="group" aria-label="Goal">
            {CHOICES.map((n) => (
              <button key={n} type="button" className={`goal__choice${goal.target === n ? " is-current" : ""}`}
                onClick={() => save(() => setSemesterGoal(n))}>{plural(n, "event")}</button>
            ))}
            <form className="goal__custom" onSubmit={(e) => { e.preventDefault(); if (custom) save(() => setSemesterGoal(Number(custom))); }}>
              <label className="sr-only" htmlFor="goal-custom">Other number of events</label>
              <input id="goal-custom" type="number" min="1" max="50" inputMode="numeric" placeholder="Other"
                value={custom} onChange={(e) => setCustom(e.target.value)} />
              <button type="submit" className="goal__choice" disabled={!custom}>Set</button>
            </form>
          </div>
          {editing && <button type="button" className="goal__link" onClick={() => setEditing(false)}>Cancel</button>}
        </div>
      ) : (
        <div className="goal__progress">
          <p className="goal__count"><b>{Math.min(goal.attended, goal.target)}</b> of {plural(goal.target, "event")} attended</p>
          <div className="goal__bar" role="progressbar" aria-label="Semester goal" aria-valuemin={0}
            aria-valuemax={goal.target} aria-valuenow={Math.min(goal.attended, goal.target)}>
            <span style={{ width: `${Math.min(100, (goal.attended / goal.target) * 100)}%` }} />
            {goal.registeredUpcoming > 0 && goal.attended < goal.target && (
              <span className="goal__bar-planned"
                style={{ width: `${Math.min(100 - (goal.attended / goal.target) * 100, (goal.registeredUpcoming / goal.target) * 100)}%` }} />
            )}
          </div>
          <p className="goal__text">
            {goal.reached ? (
              <>Goal reached{goal.attended > goal.target ? `, and ${goal.attended - goal.target} more` : ""}. Nice work.</>
            ) : goal.attended + goal.registeredUpcoming >= goal.target ? (
              <>You're registered for {plural(goal.registeredUpcoming, "more event")} this semester. Attend them and you've made it.</>
            ) : (
              <>
                {goal.registeredUpcoming > 0 && `You're registered for ${goal.registeredUpcoming} more. `}
                Find {plural(goal.target - goal.attended - goal.registeredUpcoming, "more event")}: <Link to="/events" className="social-link">browse events</Link>.
              </>
            )}
          </p>
          <div className="goal__actions">
            <button type="button" className="goal__link" onClick={() => setEditing(true)}>{goal.reached ? "Raise the goal" : "Change goal"}</button>
            <button type="button" className="goal__link" onClick={() => save(clearSemesterGoal)}>Remove goal</button>
          </div>
        </div>
      )}
      {error && <p className="social-error" role="alert">{error}</p>}
      <p className="achievements__note">Only check-ins scanned at the event count.</p>
    </section>
  );
}
