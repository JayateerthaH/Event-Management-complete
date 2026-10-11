import { useState } from 'react';
import { submitFeedback } from '../services/feedbackService';

/**
 * FeedbackForm — interactive 5-star rating + text feedback
 * Props:
 *   eventId    – event to submit feedback for
 *   eventTitle – display title
 *   onClose    – close the form / navigate away
 *   onSuccess  – callback after successful submission
 */
export default function FeedbackForm({ eventId, eventTitle, onClose, onSuccess }) {
  const [stars, setStars]       = useState(0);
  const [hovered, setHovered]   = useState(0);
  const [text, setText]         = useState('');
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState('');
  const [success, setSuccess]   = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (stars < 1 || stars > 5) {
      setError('Please select a rating between 1 and 5 stars.');
      return;
    }
    if (text.length > 500) {
      setError('Feedback must be 500 characters or less.');
      return;
    }

    setLoading(true);
    try {
      await submitFeedback(eventId, {
        noOfStars: stars,
        feedBack: text.trim(),
      });
      setSuccess(true);
      onSuccess?.();
    } catch (err) {
      const status = err?.response?.status;
      const msg = typeof err?.response?.data === 'string'
        ? err.response.data
        : err?.response?.data?.message || err?.response?.data?.error;

      if (status === 409 || (msg && msg.toLowerCase().includes('already'))) {
        setError('You have already submitted feedback for this event.');
      } else if (status === 400) {
        setError(msg || 'Invalid feedback. Please check your input.');
      } else {
        setError(msg || 'Failed to submit feedback. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  if (success) {
    return (
      <div className="fb-overlay" onClick={onClose}>
        <div className="fb-modal" onClick={(e) => e.stopPropagation()}>
          <div className="fb-success-card">
            <div className="fb-success-card__icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M9 11l3 3L22 4" />
                <path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11" />
              </svg>
            </div>
            <h3 className="fb-success-card__title">Thank You!</h3>
            <p className="fb-success-card__text">
              Your feedback for <strong>{eventTitle}</strong> has been submitted successfully.
            </p>
            <button className="fb-btn fb-btn--primary" onClick={onClose}>Close</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fb-overlay" onClick={onClose}>
      <div className="fb-modal" onClick={(e) => e.stopPropagation()}>
        <div className="fb-modal__header">
          <h3 className="fb-modal__title">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" style={{ width: 20, height: 20 }}>
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
            </svg>
            Rate Event
          </h3>
          <button className="fb-modal__close" onClick={onClose}>&times;</button>
        </div>

        <form onSubmit={handleSubmit} className="fb-modal__body">
          {eventTitle && <p className="fb-event-title">{eventTitle}</p>}

          {error && <div className="fb-alert fb-alert--error">{error}</div>}

          {/* Star Rating */}
          <div className="fb-stars">
            <label className="fb-field__label">Your Rating *</label>
            <div className="fb-stars__row">
              {[1, 2, 3, 4, 5].map((s) => (
                <button
                  key={s}
                  type="button"
                  className={`fb-star ${s <= (hovered || stars) ? 'fb-star--active' : ''}`}
                  onClick={() => setStars(s)}
                  onMouseEnter={() => setHovered(s)}
                  onMouseLeave={() => setHovered(0)}
                  disabled={loading}
                  aria-label={`${s} star${s > 1 ? 's' : ''}`}
                >
                  <svg viewBox="0 0 24 24" fill={s <= (hovered || stars) ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth="1.5">
                    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                  </svg>
                </button>
              ))}
              <span className="fb-stars__label">
                {stars > 0 ? `${stars} / 5` : 'Select rating'}
              </span>
            </div>
          </div>

          {/* Text Feedback */}
          <div className="fb-field">
            <label className="fb-field__label">
              Your Feedback <span className="fb-field__opt">(optional, max 500 chars)</span>
            </label>
            <textarea
              className="fb-field__input fb-field__textarea"
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Tell us about your experience..."
              rows={4}
              maxLength={500}
              disabled={loading}
            />
            <span className="fb-char-count">{text.length}/500</span>
          </div>

          <div className="fb-actions">
            <button type="button" className="fb-btn fb-btn--ghost" onClick={onClose} disabled={loading}>
              Cancel
            </button>
            <button type="submit" className="fb-btn fb-btn--primary" disabled={loading || stars === 0}>
              {loading ? (
                <><span className="fb-spinner" /> Submitting...</>
              ) : (
                'Submit Feedback'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
