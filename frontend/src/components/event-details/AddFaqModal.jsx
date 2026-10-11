import { useState } from 'react';
import { addFaq } from '../../api/eventDetailsService';
import '../../styles/EventDetails.css';

export default function AddFaqModal({ eventId, onClose, onSuccess }) {
  const [form, setForm] = useState({ questions: '', answers: '' });
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.questions.trim()) { setError('Question is required'); return; }
    if (!form.answers.trim())   { setError('Answer is required');   return; }
    setSubmitting(true);
    setError(null);
    try {
      await addFaq(eventId, {
        questions: form.questions.trim(),
        answers: form.answers.trim(),
      });
      onSuccess?.();
    } catch (err) {
      console.error('[AddFaq] error:', err?.response?.status, err?.response?.data, err);
      const data = err?.response?.data;
      const msg = typeof data === 'string' ? data : data?.message || data?.error || err?.message || 'Failed to add FAQ';
      setError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="ed-modal-backdrop" onClick={onClose}>
      <div className="ed-modal" onClick={(e) => e.stopPropagation()}>
        <div className="ed-modal__header">
          <h3>Add FAQ</h3>
          <button className="ed-modal__close" onClick={onClose}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <form className="ed-modal__body" onSubmit={handleSubmit}>
          {error && <div className="ed-modal__error">{error}</div>}

          <label className="ed-form__label">
            Question *
            <input name="questions" value={form.questions} onChange={handleChange} className="ed-form__input" placeholder="e.g. Is there a registration fee?" required />
          </label>

          <label className="ed-form__label">
            Answer *
            <textarea name="answers" value={form.answers} onChange={handleChange} className="ed-form__textarea" rows="4" placeholder="Provide a detailed answer..." required />
          </label>

          <div className="ed-modal__actions">
            <button type="button" className="ed-btn ed-btn--ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className="ed-btn ed-btn--primary" disabled={submitting}>
              {submitting ? 'Adding...' : 'Add FAQ'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
