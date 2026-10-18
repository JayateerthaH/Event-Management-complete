import { useEffect, useState } from "react";
import { getDepartmentGoing } from "../../api/socialService";
import "../../styles/Social.css";

/** "12 from CSE are going": a count only (never names), shown from 3 people upward. */
export default function DepartmentGoing({ eventId }) {
  const [going, setGoing] = useState(null);

  useEffect(() => {
    getDepartmentGoing(eventId).then((g) => setGoing(g || null)).catch(() => setGoing(null));
  }, [eventId]);

  if (!going) return null;
  return (
    <p className="dept-going">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M17 20h5v-2a3 3 0 00-5.4-1.8M9 20H4v-2a3 3 0 015.4-1.8M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
      <span><b>{going.count}</b> from {going.department} {going.count === 1 ? "is" : "are"} going</span>
    </p>
  );
}
