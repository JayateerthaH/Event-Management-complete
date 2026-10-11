import { useState } from 'react';
import AddScheduleModal from './AddScheduleModal';
import '../../styles/EventDetails.css';

export default function ScheduleTab({ eventId, schedule, loading, isOrganizer, onSessionAdded }) {
  const [showModal, setShowModal] = useState(false);

  // schedule is { id, time[], schedule[], smallDescription[] } — flatten to rows
  const sessions = (() => {
    if (!schedule) return [];
    const times  = schedule.time || [];
    const titles = schedule.schedule || [];
    const descs  = schedule.smallDescription || [];
    const len = Math.max(times.length, titles.length, descs.length);
    const arr = [];
    for (let i = 0; i < len; i++) {
      arr.push({ time: times[i] || '--', title: titles[i] || `Session ${i + 1}`, desc: descs[i] || '' });
    }
    return arr;
  })();

  if (loading) {
    return (
      <div className="ed-schedule">
        {[1, 2, 3].map(i => (
          <div key={i} className="ed-sched-skeleton">
            <div className="ed-skel-line ed-skel-line--sm" />
            <div className="ed-skel-line ed-skel-line--lg" />
            <div className="ed-skel-line ed-skel-line--md" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="ed-schedule">
      <div className="ed-section__header">
        <h2 className="ed-section__title">Event Schedule</h2>
        {isOrganizer && (
          <button className="ed-btn ed-btn--primary" onClick={() => setShowModal(true)}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            Add Session
          </button>
        )}
      </div>

      {sessions.length === 0 ? (
        <div className="ed-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          <p>No schedule available yet.</p>
        </div>
      ) : (
        <div className="ed-timeline">
          {sessions.map((s, i) => (
            <div key={i} className="ed-timeline__item">
              <div className="ed-timeline__marker" />
              <div className="ed-timeline__card">
                <div className="ed-timeline__time">{s.time}</div>
                <h3 className="ed-timeline__title">{s.title}</h3>
                {s.desc && <p className="ed-timeline__desc">{s.desc}</p>}
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <AddScheduleModal
          eventId={eventId}
          schedule={schedule}
          onClose={() => setShowModal(false)}
          onSuccess={() => { setShowModal(false); onSessionAdded?.(); }}
        />
      )}
    </div>
  );
}
