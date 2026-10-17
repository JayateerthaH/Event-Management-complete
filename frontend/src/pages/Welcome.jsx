import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiErrorMessage } from "../api/apiClient";
import { getInterestOptions, getMyInterestNames, saveInterestNames } from "../api/interestService";
import "../styles/Welcome.css";

/** First step after signup: pick interests so the first feed already fits the student. */
export default function Welcome() {
  const navigate = useNavigate();
  const [options, setOptions] = useState(null);
  const [picked, setPicked] = useState([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([getInterestOptions().catch(() => []), getMyInterestNames().catch(() => [])]).then(([opts, mine]) => {
      setOptions(opts);
      setPicked(mine.filter((m) => opts.some((o) => o.toLowerCase() === m.toLowerCase())));
    });
  }, []);

  const toggle = (name) =>
    setPicked((p) => (p.includes(name) ? p.filter((x) => x !== name) : p.length >= 15 ? p : [...p, name]));

  const save = async () => {
    setBusy(true);
    setError("");
    try {
      await saveInterestNames(picked);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err));
      setBusy(false);
    }
  };

  return (
    <div className="welcome-page">
      <main className="welcome-card">
        <p className="welcome-step">Step 1 of 1</p>
        <h1>What are you into?</h1>
        <p className="welcome-lede">Pick a few topics and we'll suggest events that fit you. You can change them later on your profile.</p>

        {options === null ? (
          <p className="welcome-muted">Loading topics…</p>
        ) : options.length === 0 ? (
          <p className="welcome-muted">No topics have been set up yet. You can add interests on your profile later.</p>
        ) : (
          <div className="welcome-chips" role="group" aria-label="Interests">
            {options.map((o) => (
              <button key={o} type="button" aria-pressed={picked.includes(o)}
                className={picked.includes(o) ? "is-on" : ""} onClick={() => toggle(o)}>
                {picked.includes(o) && <span aria-hidden="true">✓ </span>}{o}
              </button>
            ))}
          </div>
        )}

        {error && <p className="welcome-error" role="alert">{error}</p>}
        <div className="welcome-actions">
          <button type="button" className="welcome-primary" disabled={busy || picked.length === 0} onClick={save}>
            {picked.length === 0 ? "Pick at least one" : `Continue with ${picked.length} topic${picked.length === 1 ? "" : "s"}`}
          </button>
          <button type="button" className="welcome-skip" onClick={() => navigate("/dashboard", { replace: true })}>
            Skip for now
          </button>
        </div>
      </main>
    </div>
  );
}
