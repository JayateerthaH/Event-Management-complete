import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { aiSearch, aiErrorMessage } from "../../api/aiService";
import AiEventTile from "./AiEventTile";
import SparkleIcon from "./SparkleIcon";
import "../../styles/Ai.css";

const EXAMPLES = ["Free workshops this week", "Hackathons for CSE students", "Cultural events this month"];

const DATE_LABELS = { TODAY: "Today", TOMORROW: "Tomorrow", THIS_WEEK: "This week", THIS_MONTH: "This month" };
const COST_LABELS = { FREE: "Free", PAID: "Paid" };

/** The filters the AI derived from the query, as readable chips. */
const filterChips = (f) =>
  !f
    ? []
    : [
        f.keywords && `“${f.keywords}”`,
        f.category,
        f.department && `Dept: ${f.department}`,
        f.venue && `At: ${f.venue}`,
        DATE_LABELS[f.dateRange],
        COST_LABELS[f.costType],
      ].filter(Boolean);

/**
 * Natural-language event search. The backend turns the query into the regular event filters
 * (or falls back to keyword search), so results are the same events the filters would find.
 * `onActiveChange(true)` tells the page to hide its regular grid while AI results are shown.
 */
export default function SmartSearch({ onActiveChange }) {
  const { user } = useAuth();
  const [query, setQuery] = useState("");
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const run = async (q) => {
    const text = q.trim();
    if (!text || loading) return;
    setQuery(text);
    setLoading(true);
    setError(null);
    try {
      const data = await aiSearch(text);
      setResult(data);
      onActiveChange?.(true);
    } catch (err) {
      setError(aiErrorMessage(err, "Search failed. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  const clear = () => {
    setResult(null);
    setError(null);
    setQuery("");
    onActiveChange?.(false);
  };

  if (!user) {
    return (
      <div className="ai-search">
        <p className="ai-note" style={{ textAlign: "center" }}>
          <SparkleIcon className="ai-sparkle" /> <Link to="/login">Log in</Link> to search in plain English, like
          “free workshops this week”.
        </p>
      </div>
    );
  }

  const chips = filterChips(result?.filters);

  return (
    <div className="ai-search">
      <form
        className="ai-search__form"
        onSubmit={(e) => {
          e.preventDefault();
          run(query);
        }}
        role="search"
      >
        <div className="ai-search__input-wrap">
          <SparkleIcon />
          <input
            className="ai-input ai-search__input"
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Describe what you're looking for, e.g. “free coding workshops this week”"
            maxLength={300}
            aria-label="Smart search"
          />
        </div>
        <button className="ai-btn ai-btn--primary" type="submit" disabled={loading || !query.trim()}>
          {loading ? (
            <>
              <span className="ai-spinner" /> Searching…
            </>
          ) : (
            "Smart search"
          )}
        </button>
      </form>

      {!result && !loading && (
        <div className="ai-chips ai-search__examples">
          {EXAMPLES.map((ex) => (
            <button key={ex} type="button" className="ai-chip" onClick={() => run(ex)}>
              {ex}
            </button>
          ))}
        </div>
      )}

      {error && (
        <div className="ai-error" style={{ marginTop: "0.75rem" }}>
          {error}
        </div>
      )}

      {result && (
        <section className="ai-panel" style={{ marginTop: "1.25rem" }} aria-live="polite">
          <div className="ai-search__results-head">
            <div>
              <h2 className="ai-search__results-title">
                {result.results.length} result{result.results.length === 1 ? "" : "s"} for “{result.query}”
              </h2>
              <div className="ai-chips">
                <span className={`ai-badge ${result.aiUsed ? "" : "ai-badge--muted"}`}>
                  {result.aiUsed ? "AI-interpreted" : "Keyword search"}
                </span>
                {result.aiUsed &&
                  chips.map((c) => (
                    <span key={c} className="ai-chip ai-chip--accent">
                      {c}
                    </span>
                  ))}
              </div>
              {result.note && (
                <p className="ai-note" style={{ marginTop: "0.5rem" }}>
                  {result.note}
                </p>
              )}
            </div>
            <button type="button" className="ai-btn ai-btn--ghost" onClick={clear}>
              Back to all events
            </button>
          </div>

          {result.results.length === 0 ? (
            <p className="ai-note">No events match that. Try fewer details or a different wording.</p>
          ) : (
            <div className="ai-tiles">
              {result.results.map((ev) => (
                <AiEventTile key={ev.id} event={ev} />
              ))}
            </div>
          )}
        </section>
      )}
    </div>
  );
}
