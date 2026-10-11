import { useState } from 'react';
import AddFaqModal from './AddFaqModal';
import '../../styles/EventDetails.css';

export default function FaqTab({ eventId, faqs, loading, isOrganizer, onFaqAdded }) {
  const [showModal, setShowModal] = useState(false);
  const [openIndex, setOpenIndex] = useState(null);

  const toggle = (i) => setOpenIndex(openIndex === i ? null : i);

  if (loading) {
    return (
      <div className="ed-faqs">
        {[1, 2, 3].map(i => (
          <div key={i} className="ed-sched-skeleton">
            <div className="ed-skel-line ed-skel-line--lg" />
            <div className="ed-skel-line ed-skel-line--md" />
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="ed-faqs">
      <div className="ed-section__header">
        <h2 className="ed-section__title">Frequently Asked Questions</h2>
        {isOrganizer && (
          <button className="ed-btn ed-btn--primary" onClick={() => setShowModal(true)}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
            Add FAQ
          </button>
        )}
      </div>

      {faqs.length === 0 ? (
        <div className="ed-empty">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><circle cx="12" cy="12" r="10"/><path d="M9 9a3 3 0 115.12 2.13c-.58.49-1.12 1.08-1.12 1.87v1"/><circle cx="12" cy="17" r="0.5"/></svg>
          <p>No FAQs available yet.</p>
        </div>
      ) : (
        <div className="ed-accordion">
          {faqs.map((faq, i) => (
            <div key={faq.id || i} className={`ed-accordion__item ${openIndex === i ? 'ed-accordion__item--open' : ''}`}>
              <button className="ed-accordion__header" onClick={() => toggle(i)}>
                <span>{faq.questions}</span>
                <svg className="ed-accordion__chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="6 9 12 15 18 9"/>
                </svg>
              </button>
              <div className="ed-accordion__body">
                <p>{faq.answers || '—'}</p>
              </div>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <AddFaqModal
          eventId={eventId}
          onClose={() => setShowModal(false)}
          onSuccess={() => { setShowModal(false); onFaqAdded?.(); }}
        />
      )}
    </div>
  );
}
