import { useState } from "react";
import useAiStatus from "../../hooks/useAiStatus";
import { draftAnnouncement, aiErrorMessage } from "../../api/aiService";
import SparkleIcon from "./SparkleIcon";
import "../../styles/Ai.css";

const TONES = [
  { value: "FRIENDLY", label: "Friendly" },
  { value: "FORMAL", label: "Formal" },
  { value: "URGENT", label: "Urgent" },
];

/** Turns rough notes into an announcement title + message; the organizer reviews it before sending. */
export default function AnnouncementHelper({ eventId, onUse }) {
  const { llmEnabled, loaded } = useAiStatus();
  const [notes, setNotes] = useState("");
  const [tone, setTone] = useState("FRIENDLY");
  const [draft, setDraft] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!loaded || !llmEnabled) return null;

  const generate = async () => {
    if (!eventId) {
      setError("Select an event first.");
      return;
    }
    if (!notes.trim()) {
      setError("Write a few words about what you want to announce.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setDraft(await draftAnnouncement(eventId, { notes: notes.trim(), tone }));
    } catch (err) {
      setError(aiErrorMessage(err, "Couldn't draft the announcement. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="ai-helper ai-stack">
      <strong className="ai-panel__title" style={{ fontSize: "0.88rem" }}>
        <SparkleIcon /> Draft with AI
      </strong>
      <div>
        <label className="ai-label" htmlFor="ai-ann-notes">
          What do you want to tell attendees?
        </label>
        <textarea
          id="ai-ann-notes"
          className="ai-input"
          rows={2}
          maxLength={1000}
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="e.g. moved to Seminar Hall B, starts 30 min later, bring college ID"
        />
      </div>
      <div className="ai-row">
        <select className="ai-input" style={{ width: "auto" }} value={tone} onChange={(e) => setTone(e.target.value)} aria-label="Tone">
          {TONES.map((t) => (
            <option key={t.value} value={t.value}>
              {t.label}
            </option>
          ))}
        </select>
        <button type="button" className="ai-btn ai-btn--primary" onClick={generate} disabled={loading}>
          {loading ? (
            <>
              <span className="ai-spinner" /> Drafting…
            </>
          ) : draft ? (
            "Regenerate"
          ) : (
            "Draft announcement"
          )}
        </button>
      </div>

      {error && <div className="ai-error">{error}</div>}

      {draft && (
        <div>
          <div className="ai-draft">
            <p className="ai-draft__title">{draft.title}</p>
            {draft.message}
          </div>
          <div className="ai-row" style={{ marginTop: "0.5rem" }}>
            <button type="button" className="ai-btn ai-btn--ghost" onClick={() => onUse(draft)}>
              Use this draft
            </button>
            <span className="ai-note">You can edit it below before sending.</span>
          </div>
        </div>
      )}
    </div>
  );
}
