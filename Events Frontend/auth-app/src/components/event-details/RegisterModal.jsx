import { useState, useRef } from 'react';
import { registerForEvent } from '../../services/registrationService';
import { useAuth } from '../../context/AuthContext';

const DEPARTMENTS = [
  'Computer Science', 'Information Technology', 'Electronics & Communication',
  'Mechanical Engineering', 'Civil Engineering', 'Electrical Engineering',
  'Chemical Engineering', 'Biotechnology', 'Business Administration',
  'Arts & Humanities', 'Science', 'Other',
];

const YEARS = ['1st Year', '2nd Year', '3rd Year', '4th Year', 'Alumni', 'Other'];

const EMPTY = { name: '', email: '', phone: '', department: '', yearOfStudy: '' };

export default function RegisterModal({ event, onClose, onSuccess }) {
  const { user } = useAuth();

  const [form, setForm] = useState(() => ({
    ...EMPTY,
    name: user?.firstName
      ? `${user.firstName}${user.lastName ? ' ' + user.lastName : ''}`.trim()
      : (user?.name ?? ''),
    email: user?.email ?? '',
  }));

  const [agreed, setAgreed]               = useState(false);
  const [loading, setLoading]             = useState(false);
  const [errorMessage, setErrorMessage]   = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const closingRef = useRef(false);

  const eventId   = event?.id ?? event?.eventId;
  const eventName = event?.name ?? event?.title ?? event?.eventTitle ?? 'this event';

  const set = (field) => (e) => setForm((f) => ({ ...f, [field]: e.target.value }));

  const validate = () => {
    if (!form.name.trim())                    return 'Full name is required.';
    if (!form.email.trim())                   return 'Email is required.';
    if (!/\S+@\S+\.\S+/.test(form.email))    return 'Please enter a valid email address.';
    if (!form.phone.trim())                   return 'Phone number is required.';
    if (!form.department)                     return 'Please select a department.';
    if (!form.yearOfStudy)                    return 'Please select your year of study.';
    if (!agreed)                              return 'You must agree to the terms and conditions.';
    return '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMessage('');
    setSuccessMessage('');

    const err = validate();
    if (err) {
      setErrorMessage(err);
      return;
    }

    setLoading(true);

    try {
      const res = await registerForEvent(eventId, form);

      // Extract success message from backend response
      const resData = res?.data;
      const message =
        resData?.message ??
        resData?.msg ??
        resData?.status ??
        (typeof resData === 'string' ? resData : null) ??
        'Registration successful!';

      setSuccessMessage(message);
      setErrorMessage('');
      setForm(EMPTY);
      setAgreed(false);

      // Auto-close after 1.5 seconds
      closingRef.current = true;
      setTimeout(() => {
        onSuccess?.();
        onClose();
      }, 1500);
    } catch (ex) {
      // Extract error message from backend response
      const backendErr =
        ex?.response?.data?.error ??
        ex?.response?.data?.message ??
        ex?.response?.data?.msg ??
        (typeof ex?.response?.data === 'string' ? ex.response.data : null) ??
        ex?.message ??
        'Registration failed. Please try again.';

      setErrorMessage(backendErr);
      setSuccessMessage('');
      // Do NOT close modal on error
    } finally {
      setLoading(false);
    }
  };

  // Prevent closing modal during auto-close countdown or while loading
  const handleBackdropClick = () => {
    if (closingRef.current || loading) return;
    onClose();
  };

  /* ── Success screen ── */
  if (successMessage) {
    return (
      <div className="ed-modal-backdrop" onClick={handleBackdropClick}>
        <div className="ed-modal reg-modal" onClick={(e) => e.stopPropagation()}>
          <div className="reg-success">
            <div className="reg-success__icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" />
                <polyline points="9 12 11 14 15 9" />
              </svg>
            </div>
            <h3>You&apos;re Registered!</h3>
            <p className="reg-success__message">{successMessage}</p>
            <p className="reg-success__sub">
              You have successfully registered for <strong>{eventName}</strong>.
              <br />Check your email for confirmation details.
            </p>
            <button
              className="reg-success__btn"
              onClick={() => { onSuccess?.(); onClose(); }}
            >
              Done
            </button>
          </div>
        </div>
      </div>
    );
  }

  /* ── Form ── */
  return (
    <div className="ed-modal-backdrop" onClick={handleBackdropClick}>
      <div className="ed-modal reg-modal" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <div className="ed-modal__header">
          <h3>Register for <span className="reg-modal__event-name">{eventName}</span></h3>
          <button className="ed-modal__close" onClick={onClose} aria-label="Close" disabled={loading}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        {/* Body */}
        <form className="ed-modal__body reg-form" onSubmit={handleSubmit} noValidate>
          {/* Error message */}
          {errorMessage && (
            <div className="reg-form__error">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="12" cy="12" r="10" /><line x1="15" y1="9" x2="9" y2="15" /><line x1="9" y1="9" x2="15" y2="15" />
              </svg>
              {errorMessage}
            </div>
          )}

          {/* Full Name */}
          <label className="ed-form__label">
            Full Name <span className="reg-form__req">*</span>
            <input
              className="ed-form__input"
              type="text"
              placeholder="Enter your full name"
              value={form.name}
              onChange={set('name')}
              autoComplete="name"
              disabled={loading}
            />
          </label>

          {/* Email */}
          <label className="ed-form__label">
            Email <span className="reg-form__req">*</span>
            <input
              className="ed-form__input"
              type="email"
              placeholder="your.email@example.com"
              value={form.email}
              onChange={set('email')}
              autoComplete="email"
              disabled={loading}
            />
          </label>

          {/* Phone */}
          <label className="ed-form__label">
            Phone Number <span className="reg-form__req">*</span>
            <input
              className="ed-form__input"
              type="tel"
              placeholder="+91 98765 43210"
              value={form.phone}
              onChange={set('phone')}
              autoComplete="tel"
              disabled={loading}
            />
          </label>

          {/* Department */}
          <label className="ed-form__label">
            Department <span className="reg-form__req">*</span>
            <div className="reg-form__select-wrap">
              <select
                className="ed-form__input reg-form__select"
                value={form.department}
                onChange={set('department')}
                disabled={loading}
              >
                <option value="" disabled>Select Department</option>
                {DEPARTMENTS.map((d) => <option key={d} value={d}>{d}</option>)}
              </select>
              <svg className="reg-form__select-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </div>
          </label>

          {/* Year of Study */}
          <label className="ed-form__label">
            Year of Study <span className="reg-form__req">*</span>
            <div className="reg-form__select-wrap">
              <select
                className="ed-form__input reg-form__select"
                value={form.yearOfStudy}
                onChange={set('yearOfStudy')}
                disabled={loading}
              >
                <option value="" disabled>Select Year</option>
                {YEARS.map((y) => <option key={y} value={y}>{y}</option>)}
              </select>
              <svg className="reg-form__select-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="6 9 12 15 18 9" />
              </svg>
            </div>
          </label>

          {/* Terms */}
          <label className="reg-form__terms">
            <input
              type="checkbox"
              checked={agreed}
              onChange={(e) => setAgreed(e.target.checked)}
              disabled={loading}
            />
            <span>I agree to the terms and conditions</span>
          </label>

          {/* Submit */}
          <button
            type="submit"
            className="reg-form__submit"
            disabled={loading}
          >
            {loading ? (
              <>
                <span className="reg-form__spinner" />
                Registering&hellip;
              </>
            ) : (
              <>
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
                Complete Registration
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}
