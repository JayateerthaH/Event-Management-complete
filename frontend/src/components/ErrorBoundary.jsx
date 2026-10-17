import { Component } from "react";

/**
 * Catches a crash anywhere in the app and shows what went wrong instead of a blank page, with a way to
 * recover. Saved data from an older session or a browser extension that changes the page can cause one.
 */
export default class ErrorBoundary extends Component {
  state = { error: null };

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error("[App crash]", error, info?.componentStack);
  }

  resetAndLogin = () => {
    try {
      localStorage.clear();
      sessionStorage.clear();
    } catch {
      /* storage blocked: reloading is still worth a try */
    }
    window.location.assign("/login");
  };

  render() {
    const { error } = this.state;
    if (!error) return this.props.children;

    const button = {
      padding: "0.6rem 1.1rem",
      borderRadius: 8,
      border: "1px solid rgba(139,92,246,0.5)",
      background: "rgba(139,92,246,0.15)",
      color: "#e2e8f0",
      cursor: "pointer",
      fontSize: "0.9rem",
    };
    return (
      <div
        role="alert"
        style={{
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          padding: 16,
          background: "#0b0b1a",
          color: "#e2e8f0",
          fontFamily: "system-ui, sans-serif",
        }}
      >
        <div style={{ maxWidth: 520, width: "100%" }}>
          <h1 style={{ fontSize: "1.4rem", margin: "0 0 0.5rem" }}>Something went wrong on this page</h1>
          <p style={{ color: "#94a3b8", margin: "0 0 1rem", lineHeight: 1.5 }}>
            Try reloading. If it keeps happening, reset this site&apos;s saved data and log in again. If it
            only happens in your normal browser window but not in a private one, a browser extension is
            likely interfering: turn extensions off for this site.
          </p>
          <pre
            style={{
              whiteSpace: "pre-wrap",
              wordBreak: "break-word",
              background: "rgba(148,163,184,0.08)",
              padding: "0.75rem",
              borderRadius: 8,
              fontSize: "0.8rem",
              color: "#fca5a5",
              margin: "0 0 1rem",
            }}
          >
            {String(error?.message || error)}
          </pre>
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <button type="button" style={button} onClick={() => window.location.reload()}>
              Reload
            </button>
            <button type="button" style={button} onClick={this.resetAndLogin}>
              Reset &amp; log in again
            </button>
          </div>
        </div>
      </div>
    );
  }
}
