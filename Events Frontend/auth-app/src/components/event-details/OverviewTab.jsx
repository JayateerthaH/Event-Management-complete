import React from "react";

// ─── Icon map for highlights that may come from the backend ───────────────────
const HIGHLIGHT_ICONS = {
  prize:       <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2z"/></svg>,
  certificate: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><path d="M4 7V4h16v3"/><path d="M9 20h6"/><path d="M12 4v16"/><path d="M2 11h4l1.5 4L12 7l4 8 1.5-4h4"/></svg>,
  networking:  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>,
  job:         <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 3h-8l-2 4h12l-2-4z"/><line x1="12" y1="11" x2="12" y2="17"/><line x1="9" y1="14" x2="15" y2="14"/></svg>,
  default:     <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"/><path d="M8 12l2 2 4-4"/></svg>,
};

const pickIcon = (keyword = '') => {
  const k = keyword.toLowerCase();
  if (k.includes('prize') || k.includes('award') || k.includes('reward')) return HIGHLIGHT_ICONS.prize;
  if (k.includes('certif'))  return HIGHLIGHT_ICONS.certificate;
  if (k.includes('network') || k.includes('connect')) return HIGHLIGHT_ICONS.networking;
  if (k.includes('job') || k.includes('career') || k.includes('recruit')) return HIGHLIGHT_ICONS.job;
  return HIGHLIGHT_ICONS.default;
};

export default function OverviewTab({ /* event */ about, details, highlights, whatToBring }) {

  // ── About text ──────────────────────────────────────────────────────────────
  const aboutText = about?.about || (typeof about === 'string' ? about : null);

  const aboutParagraphs = aboutText
    ? aboutText.split(/\n\n|\n/).filter(Boolean)
    : null;

  // ── Key Highlights — from /highlights endpoint [{name, smallDescription}] ──
  const highlightsList = highlights || [];

  // ── Event Details — from /details endpoint (EventDetails) ──────────────────
  const detailRows = [
    { label: 'Organized By:', value: details?.organizedBy },
    { label: 'Departments:',  value: Array.isArray(details?.departments) ? details.departments.join(', ') : details?.departments },
    { label: 'Duration:',     value: details?.durationInHours ? `${details.durationInHours} Hours` : null },
    { label: 'Language:',     value: details?.language },
    { label: 'Level:',        value: details?.level },
    { label: 'Contact Email:',value: details?.contactEmail },
    { label: 'Contact Phone:',value: details?.contactPhone },
  ].filter(d => d.value);

  // ── What to Bring — from /whattobring endpoint [{ item }] ─────────────────
  const wtbList = whatToBring || [];

  return (
    <div className="ed-overview">
      {/* ── About This Event ── */}
      <section className="ed-section">
        <h2 className="ed-section__title">About This Event</h2>
        {aboutParagraphs ? (
          aboutParagraphs.map((p, i) => (
            <p key={i} className="ed-section__text">{p}</p>
          ))
        ) : (
          <p className="ed-section__text ed-section__text--muted">
            No description available yet.
          </p>
        )}
      </section>

      {/* ── Key Highlights ── */}
      <section className="ed-section">
        <h2 className="ed-section__title">Key Highlights</h2>
        {highlightsList.length > 0 ? (
          <div className="ed-highlights">
            {highlightsList.map((h, i) => (
              <div key={h.id || i} className="ed-highlight-card">
                <div className="ed-highlight-card__icon">
                  {pickIcon(h.name || '')}
                </div>
                <h3 className="ed-highlight-card__title">{h.name}</h3>
                <p className="ed-highlight-card__desc">{h.smallDescription || ''}</p>
              </div>
            ))}
          </div>
        ) : (
          <p className="ed-section__text ed-section__text--muted">
            No highlights added yet.
          </p>
        )}
      </section>

      {/* ── Event Details ── */}
      <section className="ed-section">
        <h2 className="ed-section__title">Event Details</h2>
        {detailRows.length > 0 ? (
          <div className="ed-details-table">
            {detailRows.map((d, i) => (
              <div key={i} className="ed-details-row">
                <span className="ed-details-row__label">{d.label}</span>
                <span className="ed-details-row__value">{d.value}</span>
              </div>
            ))}
          </div>
        ) : (
          <p className="ed-section__text ed-section__text--muted">
            No event details added yet.
          </p>
        )}
      </section>

      {/* ── What to Bring ── */}
      <section className="ed-section">
        <h2 className="ed-section__title">What to Bring</h2>
        {wtbList.length > 0 ? (
          <ul className="ed-checklist">
            {wtbList.map((w, i) => {
              const text = typeof w === 'string' ? w : w?.item || '';
              return (
                <li key={w?.id || i} className="ed-checklist__item">
                  <svg className="ed-checklist__icon" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="#22c55e" strokeWidth="2"/>
                    <path d="M8 12l2.5 2.5L16 9" stroke="#22c55e" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                  </svg>
                  <span>{text}</span>
                </li>
              );
            })}
          </ul>
        ) : (
          <p className="ed-section__text ed-section__text--muted">
            No items listed yet.
          </p>
        )}
      </section>
    </div>
  );
}
