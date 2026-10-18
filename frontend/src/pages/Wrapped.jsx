import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import Navbar from "../components/Navbar";
import { getWrapped, wrappedShared } from "../api/socialService";
import "../styles/Social.css";
import "../styles/Wrapped.css";

const plural = (n, word) => `${n} ${word}${n === 1 ? "" : "s"}`;
const label = (key) => (key.endsWith("-1") ? `Jan–Jun ${key.slice(0, 4)}` : `Jul–Dec ${key.slice(0, 4)}`);

/** Campus Wrapped: the student's semester in numbers (scanned check-ins only), ready to share. */
export default function Wrapped() {
  const [params, setParams] = useSearchParams();
  const semester = params.get("semester");
  const [w, setW] = useState(null);
  const [error, setError] = useState(false);
  const [shared, setShared] = useState("");

  useEffect(() => {
    getWrapped(semester).then((d) => { setW(d); setError(false); }).catch(() => setError(true));
  }, [semester]);

  const shareText = w && `My ${w.semester} on CampusEvents: ${plural(w.events, "event")}`
    + (w.hours ? `, ${plural(w.hours, "hour")}` : "")
    + (w.certificates ? `, ${plural(w.certificates, "certificate")}` : "")
    + (w.topPercent ? `. Top ${w.topPercent}% on campus!` : ".");
  const share = async () => {
    const url = `${window.location.origin}/events`;
    wrappedShared().catch(() => {});
    if (navigator.share) {
      try { await navigator.share({ title: "My Campus Wrapped", text: shareText, url }); return; } catch { /* closed: fall back */ }
    }
    window.open(`https://wa.me/?text=${encodeURIComponent(`${shareText} ${url}`)}`, "_blank", "noopener");
    setShared("Opened WhatsApp");
  };

  return (
    <div className="social-page">
      <Navbar />
      <main className="social-wrap wrapped">
        {error ? (
          <p className="social-error" role="alert">Couldn't load your Wrapped. Try again later.</p>
        ) : !w ? (
          <p className="social-muted">Loading…</p>
        ) : (
          <>
            <header className="wrapped__head">
              <p className="wrapped__kicker">Campus Wrapped</p>
              <h1>Your {w.semester}</h1>
              <nav className="wrapped__semesters" aria-label="Semester">
                {w.semesters.map((k, i) => (
                  <button key={k} type="button" className={k === w.key ? "is-current" : ""} aria-pressed={k === w.key}
                    onClick={() => setParams(i === 0 ? {} : { semester: k })}>
                    {label(k)}
                  </button>
                ))}
              </nav>
            </header>

            {w.events === 0 ? (
              <div className="social-empty">
                <h2>No check-ins this semester yet</h2>
                <p>Events you attend (scanned at the venue) build your Wrapped.</p>
                <Link className="pc-btn pc-btn--primary" to="/events">Browse events</Link>
              </div>
            ) : (
              <>
                <section className="wrapped__grid">
                  <article className="wrapped__card wrapped__card--hero">
                    <span>You showed up to</span>
                    <strong>{w.events}</strong>
                    <span>{w.events === 1 ? "event" : "events"}</span>
                    {w.topPercent && <em>Top {w.topPercent}% on campus</em>}
                  </article>
                  {w.hours > 0 && <article className="wrapped__card"><strong>{w.hours}</strong><span>{w.hours === 1 ? "hour" : "hours"} of events</span></article>}
                  {w.certificates > 0 && <article className="wrapped__card"><strong>{w.certificates}</strong><span>{w.certificates === 1 ? "certificate" : "certificates"}</span></article>}
                  {w.badges > 0 && <article className="wrapped__card"><strong>{w.badges}</strong><span>{w.badges === 1 ? "badge" : "badges"} earned</span></article>}
                  {w.friends > 0 && <article className="wrapped__card"><strong>{w.friends}</strong><span>new {w.friends === 1 ? "friend" : "friends"}</span></article>}
                  {w.busiestMonth && (
                    <article className="wrapped__card"><span>Busiest month</span><strong className="wrapped__word">{w.busiestMonth}</strong><span>{plural(w.busiestMonthEvents, "event")}</span></article>
                  )}
                </section>

                {(w.topCategories.length > 0 || w.topClubs.length > 0) && (
                  <section className="wrapped__lists">
                    {w.topCategories.length > 0 && (
                      <div>
                        <h2>Your top categories</h2>
                        <ol>{w.topCategories.map((c) => <li key={c.name}><span>{c.name}</span><b>{c.count}</b></li>)}</ol>
                      </div>
                    )}
                    {w.topClubs.length > 0 && (
                      <div>
                        <h2>Clubs you followed around</h2>
                        <ol>{w.topClubs.map((c) => <li key={c.name}><span>{c.name}</span><b>{c.count}</b></li>)}</ol>
                      </div>
                    )}
                  </section>
                )}

                {w.firstEvent && (
                  <p className="wrapped__story">
                    It started with <b>{w.firstEvent}</b>{w.lastEvent && w.lastEvent !== w.firstEvent && <> and most recently <b>{w.lastEvent}</b></>}.
                  </p>
                )}

                <div className="wrapped__share">
                  <button type="button" className="pc-btn pc-btn--primary" onClick={share}>Share my Wrapped</button>
                  <Link className="pc-btn pc-btn--ghost" to="/settings#portfolio">My portfolio</Link>
                  {shared && <span className="social-muted" role="status">{shared}</span>}
                </div>
              </>
            )}
          </>
        )}
      </main>
    </div>
  );
}
