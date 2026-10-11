import { useState } from "react";
import { fetchFeedbackSummary, aiErrorMessage } from "../../api/aiService";
import SparkleIcon from "./SparkleIcon";
import "../../styles/Ai.css";

const SENTIMENT = {
  POSITIVE: { label: "Mostly positive", cls: "ai-badge--positive" },
  MIXED: { label: "Mixed", cls: "ai-badge--mixed" },
  NEGATIVE: { label: "Mostly negative", cls: "ai-badge--negative" },
};

/**
 * Summarises an event's feedback: response count and average (always), plus AI sentiment,
 * highlights and improvements when the server has AI configured.
 */
export default function FeedbackInsights({ eventId }) {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      setSummary(await fetchFeedbackSummary(eventId));
    } catch (err) {
      setError(aiErrorMessage(err, "Couldn't summarize feedback. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  const insights = summary?.insights;
  const sentiment = insights && SENTIMENT[insights.sentiment];

  return (
    <section className="ai-panel ai-insights">
      <div className="ai-panel__header" style={{ marginBottom: summary || error ? "1rem" : 0 }}>
        <div>
          <h3 className="ai-panel__title">
            <SparkleIcon /> Feedback insights
          </h3>
          <p className="ai-panel__subtitle">What attendees liked and what to improve, summarized by AI.</p>
        </div>
        <button type="button" className="ai-btn ai-btn--primary" onClick={load} disabled={loading}>
          {loading ? (
            <>
              <span className="ai-spinner" /> Summarizing…
            </>
          ) : summary ? (
            "Refresh"
          ) : (
            "Summarize feedback"
          )}
        </button>
      </div>

      {error && <div className="ai-error">{error}</div>}

      {summary && (
        <>
          <div className="ai-insights__stats">
            <div>
              <div className="ai-stat__value">{summary.responses}</div>
              <div className="ai-stat__label">Responses</div>
            </div>
            {summary.responses > 0 && (
              <div>
                <div className="ai-stat__value">{summary.averageStars.toFixed(1)} / 5</div>
                <div className="ai-stat__label">Average rating</div>
              </div>
            )}
            {sentiment && (
              <div style={{ alignSelf: "center" }}>
                <span className={`ai-badge ${sentiment.cls}`}>{sentiment.label}</span>
              </div>
            )}
          </div>

          {insights && (
            <>
              <p className="ai-insights__summary">{insights.summary}</p>
              <div className="ai-insights__cols">
                {insights.highlights?.length > 0 && (
                  <div className="ai-insights__col ai-insights__col--good">
                    <h4>What worked</h4>
                    <ul>
                      {insights.highlights.map((h, i) => (
                        <li key={i}>{h}</li>
                      ))}
                    </ul>
                  </div>
                )}
                {insights.improvements?.length > 0 && (
                  <div className="ai-insights__col ai-insights__col--improve">
                    <h4>What to improve</h4>
                    <ul>
                      {insights.improvements.map((h, i) => (
                        <li key={i}>{h}</li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </>
          )}

          {summary.note && (
            <p className="ai-note" style={{ marginTop: "0.75rem" }}>
              {summary.note}
            </p>
          )}
        </>
      )}
    </section>
  );
}
