import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyInterestNames } from "../../api/interestService";

const KEY = "ce_interest_nudge_dismissed";

/** Dashboard reminder for students with no interests yet: suggestions get much better with a few. */
export default function InterestNudge() {
  const [show, setShow] = useState(false);

  useEffect(() => {
    let dismissed = false;
    try { dismissed = localStorage.getItem(KEY) === "1"; } catch { /* storage blocked */ }
    if (dismissed) return;
    getMyInterestNames().then((names) => setShow(names.length === 0)).catch(() => {});
  }, []);

  if (!show) return null;
  const dismiss = () => {
    try { localStorage.setItem(KEY, "1"); } catch { /* storage blocked */ }
    setShow(false);
  };
  return (
    <div className="interest-nudge" role="note">
      <p><b>Get better suggestions.</b> Pick a few topics you like and we'll recommend events that fit.</p>
      <div className="interest-nudge__actions">
        <Link to="/welcome" className="interest-nudge__go">Pick interests</Link>
        <button type="button" onClick={dismiss} className="interest-nudge__close">Not now</button>
      </div>
    </div>
  );
}
