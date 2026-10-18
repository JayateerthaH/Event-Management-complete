/** Shown while a page's code downloads (pages are loaded on demand, see App.jsx). Fades in late, so quick loads show nothing. */
export default function PageLoader() {
  return (
    <div
      role="status"
      aria-live="polite"
      style={{
        minHeight: "60vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        color: "#94a3b8",
        fontSize: "0.95rem",
        animation: "page-loader-in 0.2s ease-out 0.3s both",
      }}
    >
      Loading…
    </div>
  );
}
