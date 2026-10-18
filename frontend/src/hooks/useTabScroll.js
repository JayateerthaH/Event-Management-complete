import { useEffect, useRef } from "react";

/**
 * Dashboards on phones: after switching tabs, show the new tab from its top (the tab row stays pinned)
 * and slide the chosen tab into view in the swipeable row. Skipped on the first render.
 */
export default function useTabScroll(activeTab, navSelector) {
  const first = useRef(true);
  useEffect(() => {
    if (first.current) { first.current = false; return; }
    const main = document.querySelector("main");
    if (main) {
      const top = main.getBoundingClientRect().top + window.scrollY - 60;
      if (window.scrollY > top) window.scrollTo({ top: Math.max(0, top) });
    }
    document.querySelector(`${navSelector} [class*="--active"]`)?.scrollIntoView({ block: "nearest", inline: "center" });
  }, [activeTab, navSelector]);
}
