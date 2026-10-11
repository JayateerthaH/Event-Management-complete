import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import useAiStatus from "../../hooks/useAiStatus";
import { askAboutEvent, aiErrorMessage } from "../../api/aiService";
import SparkleIcon from "./SparkleIcon";
import "../../styles/Ai.css";

const SUGGESTIONS = ["Is it free?", "Who is this event for?", "What should I bring?", "What's on the schedule?"];

/**
 * Answers questions about one event using only what its page publishes (description, schedule, FAQs,
 * speakers, ...). Says so when the page doesn't cover something instead of guessing.
 * Render with key={eventId} so each event starts a fresh conversation.
 */
export default function EventAssistant({ eventId, eventName }) {
  const { user } = useAuth();
  const { llmEnabled, loaded } = useAiStatus();
  const [messages, setMessages] = useState([]); // { role: 'user'|'bot', text, unsure?, error? }
  const [question, setQuestion] = useState("");
  const [asking, setAsking] = useState(false);
  const threadRef = useRef(null);

  useEffect(() => {
    threadRef.current?.scrollTo({ top: threadRef.current.scrollHeight, behavior: "smooth" });
  }, [messages, asking]);

  const ask = async (text) => {
    const q = text.trim();
    if (!q || asking) return;
    setQuestion("");
    setMessages((m) => [...m, { role: "user", text: q }]);
    setAsking(true);
    try {
      const res = await askAboutEvent(eventId, q);
      setMessages((m) => [...m, { role: "bot", text: res.answer, unsure: !res.answeredFromDetails }]);
    } catch (err) {
      setMessages((m) => [
        ...m,
        { role: "bot", text: aiErrorMessage(err, "I couldn't answer that right now."), error: true },
      ]);
    } finally {
      setAsking(false);
    }
  };

  if (user && loaded && !llmEnabled) return null; // AI not configured on the server

  return (
    <section className="ai-panel ai-assistant" aria-label="Ask about this event">
      <div className="ai-panel__header">
        <div>
          <h2 className="ai-panel__title">
            <SparkleIcon /> Ask about this event
          </h2>
          <p className="ai-panel__subtitle">
            Answers come only from this event's page{eventName ? ` for “${eventName}”` : ""}.
          </p>
        </div>
      </div>

      {!user ? (
        <p className="ai-note">
          <Link to="/login">Log in</Link>{" "}
          to ask questions like “Is it free?” or “What should I bring?”.
        </p>
      ) : (
        <>
          {messages.length > 0 && (
            <div className="ai-assistant__thread" ref={threadRef} aria-live="polite">
              {messages.map((m, i) => (
                <div
                  key={i}
                  className={`ai-msg ai-msg--${m.role} ${m.error ? "ai-msg--error" : ""}`}
                >
                  {m.text}
                  {m.unsure && <span className="ai-msg__foot">Not covered on the event page</span>}
                </div>
              ))}
              {asking && (
                <div className="ai-msg ai-msg--bot">
                  <span className="ai-spinner" style={{ borderTopColor: "#c4b5fd" }} /> Thinking…
                </div>
              )}
            </div>
          )}

          {messages.length === 0 && (
            <div className="ai-chips">
              {SUGGESTIONS.map((s) => (
                <button key={s} type="button" className="ai-chip" onClick={() => ask(s)} disabled={asking}>
                  {s}
                </button>
              ))}
            </div>
          )}

          <form
            className="ai-assistant__form"
            onSubmit={(e) => {
              e.preventDefault();
              ask(question);
            }}
          >
            <input
              className="ai-input"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="Ask anything about this event…"
              maxLength={500}
              aria-label="Your question"
            />
            <button className="ai-btn ai-btn--primary" type="submit" disabled={asking || !question.trim()}>
              Ask
            </button>
          </form>
        </>
      )}
    </section>
  );
}
