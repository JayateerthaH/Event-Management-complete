import { useState } from "react";
import useAiStatus from "../../hooks/useAiStatus";
import { generateEventDescription, aiErrorMessage } from "../../api/aiService";
import SparkleIcon from "./SparkleIcon";
import "../../styles/Ai.css";

/**
 * Drafts an event description + FAQs from the create form's fields and a few key points.
 * The organizer reviews the draft and chooses what to use; nothing is applied automatically.
 *
 * @param form            the create-event form ({ name, category, eventDate, venue, costOfRegistration })
 * @param onUseDescription called with the drafted description text
 * @param onFaqsChange     called with the FAQs to add after the event is created ([] = none)
 */
export default function DescriptionHelper({ form, onUseDescription, onFaqsChange }) {
  const { llmEnabled, loaded } = useAiStatus();
  const [open, setOpen] = useState(false);
  const [keyPoints, setKeyPoints] = useState("");
  const [draft, setDraft] = useState(null);
  const [addFaqs, setAddFaqs] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [used, setUsed] = useState(false);

  if (!loaded || !llmEnabled) return null;

  const generate = async () => {
    if (!form.name?.trim()) {
      setError("Enter the event name first — the draft is based on it.");
      return;
    }
    setLoading(true);
    setError(null);
    setUsed(false);
    try {
      const result = await generateEventDescription({
        title: form.name.trim(),
        category: form.category || null,
        eventDate: form.eventDate || null,
        venue: form.venue?.trim() || null,
        cost: form.costOfRegistration?.trim() || null,
        keyPoints: keyPoints.split("\n").map((p) => p.trim()).filter(Boolean),
      });
      setDraft(result);
      setAddFaqs(false);
      onFaqsChange?.([]);
    } catch (err) {
      setError(aiErrorMessage(err, "Couldn't draft a description. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  const toggleFaqs = (checked) => {
    setAddFaqs(checked);
    onFaqsChange?.(checked ? draft?.faqs ?? [] : []);
  };

  if (!open) {
    return (
      <button type="button" className="ai-btn ai-btn--ghost" style={{ marginTop: "0.5rem", alignSelf: "flex-start" }} onClick={() => setOpen(true)}>
        <SparkleIcon /> Write with AI
      </button>
    );
  }

  return (
    <div className="ai-helper ai-stack">
      <div className="ai-row" style={{ justifyContent: "space-between" }}>
        <strong className="ai-panel__title" style={{ fontSize: "0.88rem" }}>
          <SparkleIcon /> AI writing assistant
        </strong>
        <button type="button" className="ai-btn ai-btn--text" onClick={() => setOpen(false)}>
          Close
        </button>
      </div>

      <div>
        <label className="ai-label" htmlFor="ai-key-points">
          Key points (one per line, optional)
        </label>
        <textarea
          id="ai-key-points"
          className="ai-input"
          rows={3}
          value={keyPoints}
          onChange={(e) => setKeyPoints(e.target.value)}
          placeholder={"Hands-on session with Arduino kits\nTeams of 2\nCertificates for all participants"}
        />
        <p className="ai-note" style={{ marginTop: "0.35rem" }}>
          Uses the name, category, date, venue and cost above. The AI won't invent details you don't give it.
        </p>
      </div>

      {error && <div className="ai-error">{error}</div>}

      <div className="ai-row">
        <button type="button" className="ai-btn ai-btn--primary" onClick={generate} disabled={loading}>
          {loading ? (
            <>
              <span className="ai-spinner" /> Drafting…
            </>
          ) : draft ? (
            "Regenerate"
          ) : (
            "Draft description & FAQs"
          )}
        </button>
      </div>

      {draft && (
        <>
          <div>
            <span className="ai-label">Description</span>
            <div className="ai-draft">{draft.description}</div>
            <div className="ai-row" style={{ marginTop: "0.5rem" }}>
              <button
                type="button"
                className="ai-btn ai-btn--ghost"
                onClick={() => {
                  onUseDescription(draft.description);
                  setUsed(true);
                }}
              >
                {used ? "✓ Added to form" : "Use this description"}
              </button>
            </div>
          </div>

          {draft.faqs?.length > 0 && (
            <div>
              <span className="ai-label">Suggested FAQs</span>
              <div className="ai-draft">
                <ul className="ai-faqs">
                  {draft.faqs.map((f, i) => (
                    <li key={i}>
                      <strong>Q: {f.question}</strong>
                      <br />
                      A: {f.answer}
                    </li>
                  ))}
                </ul>
              </div>
              <label className="ai-check" style={{ marginTop: "0.5rem" }}>
                <input type="checkbox" checked={addFaqs} onChange={(e) => toggleFaqs(e.target.checked)} />
                Add these {draft.faqs.length} FAQs to the event when I create it
              </label>
            </div>
          )}
        </>
      )}
    </div>
  );
}
