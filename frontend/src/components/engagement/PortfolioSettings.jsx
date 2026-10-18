import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { apiErrorMessage } from "../../api/apiClient";
import { getMyPortfolio, resetPortfolioLink, setPortfolioPublished } from "../../api/socialService";

/** Settings: publish the student portfolio (attendance, certificates, badges) as a link for a CV or LinkedIn. */
export default function PortfolioSettings() {
  const [mine, setMine] = useState(null);
  const [error, setError] = useState("");
  const [copied, setCopied] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);

  useEffect(() => {
    getMyPortfolio().then(setMine).catch((e) => setError(apiErrorMessage(e)));
  }, []);

  // Reached from the dashboard's "portfolio" link (/settings#portfolio): scroll once the section has content
  useEffect(() => {
    if (mine && window.location.hash === "#portfolio") document.getElementById("portfolio")?.scrollIntoView({ block: "start" });
  }, [mine === null]); // eslint-disable-line react-hooks/exhaustive-deps

  const run = async (call) => {
    setError("");
    try { setMine(await call()); } catch (e) { setError(apiErrorMessage(e)); }
  };
  const togglePublished = (published) => {
    const before = mine;
    setMine({ ...mine, published });   // flip at once; the server's answer replaces it
    run(() => setPortfolioPublished(published).catch((e) => { setMine(before); throw e; }));
  };
  const copy = async () => {
    try { await navigator.clipboard.writeText(mine.url); } catch {
      const el = document.getElementById("portfolio-url");
      el?.select();
      document.execCommand("copy");
    }
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  const slug = mine?.url.split("/p/")[1];
  const stats = mine?.portfolio.stats;

  return (
    <section className="social-form" id="portfolio" aria-labelledby="portfolio-title">
      <h2 className="social-h2" id="portfolio-title">Portfolio</h2>
      <p className="social-muted">
        One page with the events you attended (scanned at the venue), your certificates and badges. Add it to your CV or LinkedIn.
      </p>
      {error && <p className="social-error" role="alert">{error}</p>}
      {mine && (
        <>
          <p className="portfolio-summary">
            {stats.events} {stats.events === 1 ? "event" : "events"} · {stats.certificates} {stats.certificates === 1 ? "certificate" : "certificates"} · {stats.badges} {stats.badges === 1 ? "badge" : "badges"}
          </p>
          <label className="social-check" htmlFor="portfolio-published">
            <input id="portfolio-published" type="checkbox" checked={mine.published}
              onChange={(e) => togglePublished(e.target.checked)} />
            <span>
              <b>Make my portfolio public</b><br />
              <small className="social-muted">Shows your name, department, year, About me and interests. Never your email or phone.</small>
            </span>
          </label>
          {mine.published ? (
            <>
              <label className="social-label" htmlFor="portfolio-url">Your portfolio link</label>
              <div className="find-bar">
                <input id="portfolio-url" className="social-input" readOnly value={mine.url} onFocus={(e) => e.target.select()} />
                <button type="button" className="pc-btn pc-btn--ghost" onClick={copy}>{copied ? "Copied" : "Copy"}</button>
              </div>
              <div className="cal-actions">
                <Link className="pc-btn pc-btn--primary" to={`/p/${slug}`}>View portfolio</Link>
                <a className="pc-btn pc-btn--ghost" target="_blank" rel="noopener noreferrer"
                  href={`https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(mine.url)}`}>Share on LinkedIn</a>
              </div>
              {!confirmReset ? (
                <button type="button" className="seat__link cal-reset" onClick={() => setConfirmReset(true)}>Reset link</button>
              ) : (
                <div className="pc__actions">
                  <span className="social-muted">The old link stops working.</span>
                  <button type="button" className="pc-btn pc-btn--danger" onClick={() => run(resetPortfolioLink).then(() => setConfirmReset(false))}>Reset link</button>
                  <button type="button" className="pc-btn pc-btn--ghost" onClick={() => setConfirmReset(false)}>Cancel</button>
                </div>
              )}
            </>
          ) : (
            <Link className="social-link" to="/profile">Update your name, department and About me</Link>
          )}
        </>
      )}
    </section>
  );
}
