import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import { getPublicPortfolio } from "../api/socialService";
import { BADGE_ICONS } from "../utils/badgeIcons";
import "../styles/Social.css";
import "../styles/Portfolio.css";

const day = (d) => new Date(d).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });

/** A student's public portfolio (/p/:slug): open without logging in, printable as a PDF. */
export default function Portfolio() {
  const { slug } = useParams();
  const [p, setP] = useState(null);
  const [missing, setMissing] = useState(false);

  useEffect(() => {
    getPublicPortfolio(slug)
      .then((v) => { setP(v); document.title = `${v.name} · Campus portfolio`; })
      .catch(() => setMissing(true));
    return () => { document.title = "CampusEvents"; };
  }, [slug]);

  return (
    <div className="social-page portfolio-page">
      <Navbar />
      <main className="social-wrap portfolio">
        {missing ? (
          <div className="social-empty">
            <h1>This portfolio isn't available</h1>
            <p>It's private, or the link was reset.</p>
            <Link className="pc-btn pc-btn--primary" to="/events">Browse events</Link>
          </div>
        ) : !p ? (
          <p className="social-muted">Loading…</p>
        ) : (
          <>
            <header className="portfolio__head">
              <p className="portfolio__kicker">Campus portfolio</p>
              <h1>{p.name}</h1>
              {(p.department || p.yearOfStudy) && (
                <p className="portfolio__meta">{[p.department, p.yearOfStudy && `Year ${p.yearOfStudy}`].filter(Boolean).join(" · ")}</p>
              )}
              {p.bio && <p className="portfolio__bio">{p.bio}</p>}
              <button type="button" className="pc-btn pc-btn--ghost portfolio__print" onClick={() => window.print()}>Save as PDF</button>
            </header>

            <dl className="portfolio__stats">
              <div><dt>Events attended</dt><dd>{p.stats.events}</dd></div>
              <div><dt>Certificates</dt><dd>{p.stats.certificates}</dd></div>
              <div><dt>Badges</dt><dd>{p.stats.badges}</dd></div>
              <div><dt>Categories</dt><dd>{p.stats.categories}</dd></div>
            </dl>

            {(p.interests.length > 0 || p.categories.length > 0) && (
              <section className="portfolio__section">
                <h2>Interests</h2>
                <ul className="portfolio__chips">
                  {[...new Set([...p.interests, ...p.categories])].map((i) => <li key={i}>{i}</li>)}
                </ul>
              </section>
            )}

            {p.badges.length > 0 && (
              <section className="portfolio__section">
                <h2>Badges</h2>
                <ul className="portfolio__badges">
                  {p.badges.map((b) => (
                    <li key={b.key}>
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                        <path d={BADGE_ICONS[b.key] || BADGE_ICONS.veteran} />
                      </svg>
                      <span><b>{b.name}</b><small>{b.description}{b.earnedAt ? ` · ${day(b.earnedAt)}` : ""}</small></span>
                    </li>
                  ))}
                </ul>
              </section>
            )}

            <section className="portfolio__section">
              <h2>Events attended</h2>
              {p.events.length === 0 ? (
                <p className="social-muted">No check-ins yet.</p>
              ) : (
                <ol className="portfolio__events">
                  {p.events.map((e) => (
                    <li key={e.eventId}>
                      <time dateTime={e.date}>{e.date ? day(e.date) : ""}</time>
                      <div>
                        <Link to={`/events/${e.eventId}`}>{e.name}</Link>
                        <small>{[e.category, e.organizedBy && `by ${e.organizedBy}`, e.venue].filter(Boolean).join(" · ")}</small>
                      </div>
                      {e.certificate && <span className="pc__tag">Certificate</span>}
                    </li>
                  ))}
                </ol>
              )}
              <p className="social-note">Attendance is verified by QR check-in at each event on CampusEvents.</p>
            </section>
          </>
        )}
      </main>
    </div>
  );
}
