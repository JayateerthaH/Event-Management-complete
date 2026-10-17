import { useEffect } from "react";
import { useLocation } from "react-router-dom";

/**
 * Start every page at the top. The router keeps the previous page's scroll position, so on a phone
 * (where you scroll down to reach the Login button) the dashboard used to open half-way down.
 * Links to an anchor (#section) keep their own scrolling.
 */
export default function ScrollToTop() {
  const { pathname, hash } = useLocation();
  useEffect(() => {
    if (!hash) window.scrollTo(0, 0);
  }, [pathname, hash]);
  return null;
}
