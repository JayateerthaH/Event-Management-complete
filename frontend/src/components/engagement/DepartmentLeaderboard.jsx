import { useEffect, useState } from "react";
import { getDepartmentLeaderboard } from "../../api/socialService";
import "../../styles/Engagement.css";

/** Student dashboard: departments ranked by check-ins this semester (totals only, no individuals). */
export default function DepartmentLeaderboard() {
  const [board, setBoard] = useState(null);

  useEffect(() => {
    getDepartmentLeaderboard().then(setBoard).catch(() => setBoard(null));
  }, []);

  if (!board || board.top.length === 0) return null;
  const max = Math.max(...board.top.map((r) => r.checkIns), 1);
  const showMine = board.mine && !board.top.some((r) => r.mine);
  return (
    <section className="sd-card dept-board" aria-labelledby="dept-board-title">
      <div className="sd-card__header">
        <h2 className="sd-card__title" id="dept-board-title">Department leaderboard</h2>
        <span className="achievements__count">{board.semester} · check-ins</span>
      </div>
      <ol className="dept-board__list">
        {[...board.top, ...(showMine ? [board.mine] : [])].map((r) => (
          <li key={r.department} className={r.mine ? "is-mine" : ""}>
            <span className="dept-board__rank">{r.rank}</span>
            <span className="dept-board__name">{r.department}{r.mine && <small> · your department</small>}</span>
            <span className="dept-board__bar" aria-hidden="true"><span style={{ width: `${(r.checkIns / max) * 100}%` }} /></span>
            <b>{r.checkIns}</b>
          </li>
        ))}
      </ol>
      <p className="achievements__note">
        {board.mine ? (board.mine.rank === 1 ? "Your department is leading. Keep it up!" : `Every event you attend moves ${board.mine.department} up.`)
          : "Add your department in your profile to see where it ranks."}
      </p>
    </section>
  );
}
