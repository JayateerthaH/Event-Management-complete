import { useEffect, useRef, useState } from "react";

/**
 * Renders its children only once they come near the screen, so widgets further down a page don't all call
 * the API the moment the page opens. Without IntersectionObserver it renders at once.
 */
export default function LazyMount({ children, minHeight = 120, margin = "300px" }) {
  const ref = useRef(null);
  const [show, setShow] = useState(() => typeof IntersectionObserver === "undefined");
  useEffect(() => {
    if (show || !ref.current) return;
    const io = new IntersectionObserver(([entry]) => { if (entry.isIntersecting) setShow(true); }, { rootMargin: margin });
    io.observe(ref.current);
    return () => io.disconnect();
  }, [show, margin]);
  return show ? children : <div ref={ref} style={{ minHeight }} aria-hidden="true" />;
}
