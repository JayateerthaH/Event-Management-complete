import { useEffect, useState } from "react";
import { getSocialProof } from "../../api/socialService";

/** Event page: "23 interested · Organizer rated 4.6★ (40 reviews)". Shown only when there's something to say. */
export default function SocialProof({ eventId }) {
  const [p, setP] = useState(null);

  useEffect(() => {
    getSocialProof(eventId).then(setP).catch(() => setP(null));
  }, [eventId]);

  if (!p) return null;
  const parts = [];
  if (p.interested > 0) parts.push(`${p.interested} interested`);
  if (p.organizerRating != null && p.organizerRatings >= 3) parts.push(`Organizer rated ${p.organizerRating.toFixed(1)}★ (${p.organizerRatings} reviews)`);
  if (parts.length === 0) return null;
  return <p className="social-proof">{parts.join(" · ")}</p>;
}
