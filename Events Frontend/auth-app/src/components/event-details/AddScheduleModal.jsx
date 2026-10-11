import { useState } from 'react';
import { addSchedule } from '../../api/eventDetailsService';
import '../../styles/EventDetails.css';

/**
 * Backend model — EventSchedule:
 *   time: List<String>, schedule: List<String>, smallDescription: List<String>
 * These are parallel arrays; each index represents one session.
 * POST replaces the whole EventSchedule, so we append to existing arrays and send full object.
 */
export default function AddScheduleModal({ eventId, schedule, onClose, onSuccess }) {
  const [form, setForm] = useState({ time: '', title: '', description: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.title.trim()) { setError('Session title is required'); return; }
    setSubmitting(true);
    setError(null);
    try {
      const existingTimes = schedule?.time || [];
      const existingTitles = schedule?.schedule || [];
      const existingDescs = schedule?.smallDescription || [];

      await addSchedule(eventId, {
        time:             [...existingTimes, form.time.trim() || '--'],
        schedule:         [...existingTitles, form.title.trim()],
        smallDescription: [...existingDescs, form.description.trim() || ''],
      });
      onSuccess?.();
    } catch (err) {
      console.error('[AddSchedule] error:', err?.response?.status, err?.response?.data, err);
      const data = err?.response?.data;
      const msg = typeof data === 'string' ? data : data?.message || data?.error || err?.message || 'Failed to add session';
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="ed-modal-backdrop" onClick={onClose}>
      <div className="ed-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ed-modal__header">
          <h3>Add Schedule Session</h3>
          <button className="ed-modal__close" onClick={onClose}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <form className="ed-modal__body" onSubmit={handleSubmit}>
          {error && <div className="ed-modal__error">{error}</div>}

          <label className="ed-form__label">
            Time *
            <input name="time" value={form.time} onChange={handleChange} className="ed-form__input" placeholder="e.g. 10:00 AM - 11:00 AM" required />
          </label>

          <label className="ed-form__label">
            Session Title *
            <input name="title" value={form.title} onChange={handleChange} className="ed-form__input" placeholder="e.g. Opening Keynote" required />
          </label>

          <label className="ed-form__label">
            Description
            <textarea name="description" value={form.description} onChange={handleChange} className="ed-form__textarea" rows="3" placeholder="Brief description of this session..." />
          </label>

          <div className="ed-modal__actions">
            <button type="button" className="ed-btn ed-btn--ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="ed-btn ed-btn--primary" disabled={submitting}>
              {submitting ? 'Adding...' : 'Add Session'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
