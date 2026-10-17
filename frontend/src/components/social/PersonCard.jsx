import { useState } from "react";
import { apiErrorMessage } from "../../api/apiClient";
import { sayHi, acceptRequest, blockUser, reportUser } from "../../api/socialService";

/**
 * Another student: name, department/year, interests (shared ones highlighted) and the one action that fits
 * the relationship. Every card has Block and Report in its "More" menu.
 */
export default function PersonCard({ person, eventId, onChange, extraActions }) {
  const [p, setP] = useState(person);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const [reporting, setReporting] = useState(false);
  const [reason, setReason] = useState("");
  const [gone, setGone] = useState("");

  const run = async (fn, after) => {
    setBusy(true);
    setError("");
    try {
      const result = await fn();
      after?.(result);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const shared = new Set(p.sharedInterests || []);
  const meta = [p.department, p.yearOfStudy && `Year ${String(p.yearOfStudy).replace(/^year\s*/i, "")}`]
    .filter(Boolean)
    .join(" · ");

  if (gone) return <div className="pc pc--gone" role="status">{gone}</div>;

  return (
    <article className="pc">
      <div className="pc__avatar" aria-hidden="true">{(p.name || "?").charAt(0).toUpperCase()}</div>
      <div className="pc__body">
        <div className="pc__head">
          <h3 className="pc__name">{p.name}</h3>
          {p.relation === "FRIENDS" && <span className="pc__tag">Friend</span>}
        </div>
        {meta && <p className="pc__meta">{meta}</p>}
        {p.interests?.length > 0 && (
          <ul className="pc__interests" aria-label="Interests">
            {p.interests.slice(0, 8).map((i) => (
              <li key={i} className={shared.has(i) ? "is-shared" : ""} title={shared.has(i) ? "You share this interest" : undefined}>
                {i}
              </li>
            ))}
          </ul>
        )}
        {shared.size > 0 && (
          <p className="pc__shared">{shared.size} shared interest{shared.size === 1 ? "" : "s"}</p>
        )}
        {p.relation === "FRIENDS" && p.contact && (
          <p className="pc__contact">Contact: <span>{p.contact}</span></p>
        )}
        {error && <p className="pc__error" role="alert">{error}</p>}

        {reporting && (
          <form
            className="pc__report"
            onSubmit={(e) => {
              e.preventDefault();
              run(() => reportUser(p.userId, reason), () => {
                setGone(`Reported and blocked ${p.name}. An admin will review it.`);
                onChange?.();
              });
            }}
          >
            <label htmlFor={`report-${p.userId}`}>What happened?</label>
            <textarea
              id={`report-${p.userId}`}
              value={reason}
              maxLength={500}
              rows={3}
              required
              onChange={(e) => setReason(e.target.value)}
              placeholder="For example: sent unwanted messages after the event"
            />
            <div className="pc__actions">
              <button type="submit" className="pc-btn pc-btn--danger" disabled={busy || !reason.trim()}>
                Report and block
              </button>
              <button type="button" className="pc-btn pc-btn--ghost" onClick={() => setReporting(false)}>
                Cancel
              </button>
            </div>
          </form>
        )}

        {!reporting && (
          <div className="pc__actions">
            {p.relation === "NONE" && (
              <button type="button" className="pc-btn pc-btn--primary" disabled={busy}
                onClick={() => run(() => sayHi(p.userId, eventId), (np) => { setP(np); onChange?.(); })}>
                Say hi
              </button>
            )}
            {p.relation === "REQUESTED" && <span className="pc__status">Requested</span>}
            {p.relation === "INCOMING" && (
              <button type="button" className="pc-btn pc-btn--primary" disabled={busy}
                onClick={() => run(() => acceptRequest(p.requestId), (np) => { setP(np); onChange?.(); })}>
                Accept request
              </button>
            )}
            {extraActions}
            <div className="pc__more">
              <button type="button" className="pc-btn pc-btn--ghost" aria-expanded={menuOpen}
                onClick={() => setMenuOpen((o) => !o)}>
                More
              </button>
              {menuOpen && (
                <div className="pc__menu" role="menu">
                  <button type="button" role="menuitem" disabled={busy}
                    onClick={() => run(() => blockUser(p.userId), () => { setGone(`Blocked ${p.name}. You won't see each other.`); onChange?.(); })}>
                    Block
                  </button>
                  <button type="button" role="menuitem" onClick={() => { setMenuOpen(false); setReporting(true); }}>
                    Report
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </article>
  );
}
