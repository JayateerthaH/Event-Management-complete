import { useState } from "react";
import { resendQr } from "../services/registrationService";
import "../styles/ResendQrButton.css";

/**
 * Re-emails the student's attendance QR code for an event and shows the outcome inline.
 * `className` lets each page apply its own button style.
 */
export default function ResendQrButton({ eventId, className = "" }) {
  const [sending, setSending] = useState(false);
  const [message, setMessage] = useState(null); // { type: 'success' | 'error', text }

  const handleClick = async () => {
    if (sending) return;
    setSending(true);
    setMessage(null);
    try {
      setMessage({ type: "success", text: await resendQr(eventId) });
    } catch (err) {
      const text =
        err?.response?.data?.error ||
        err?.response?.data?.message ||
        err?.message ||
        "Could not resend the QR code. Please try again.";
      setMessage({ type: "error", text });
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="resend-qr">
      <button type="button" className={className} onClick={handleClick} disabled={sending}>
        {sending ? "Sending…" : "Resend QR code"}
      </button>
      {message && (
        <p className={`resend-qr__msg resend-qr__msg--${message.type}`} role="status">
          {message.text}
        </p>
      )}
    </div>
  );
}
