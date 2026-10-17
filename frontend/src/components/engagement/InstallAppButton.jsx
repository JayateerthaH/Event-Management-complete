import { useEffect, useState } from "react";

let deferredPrompt = null;
if (typeof window !== "undefined") {
  window.addEventListener("beforeinstallprompt", (e) => {
    e.preventDefault();
    deferredPrompt = e;
    window.dispatchEvent(new Event("ce-installable"));
  });
}

const isStandalone = () =>
  window.matchMedia?.("(display-mode: standalone)").matches || window.navigator.standalone === true;
const isIos = () => /iphone|ipad|ipod/i.test(window.navigator.userAgent);

/** "Install app": the browser's install prompt (Android, desktop Chrome/Edge) or how-to for iPhone. */
export default function InstallAppButton() {
  const [canPrompt, setCanPrompt] = useState(Boolean(deferredPrompt));
  const [showIosHelp, setShowIosHelp] = useState(false);

  useEffect(() => {
    const on = () => setCanPrompt(true);
    window.addEventListener("ce-installable", on);
    return () => window.removeEventListener("ce-installable", on);
  }, []);

  if (isStandalone()) return null;
  if (!canPrompt && !isIos()) return null;

  const install = async () => {
    if (deferredPrompt) {
      deferredPrompt.prompt();
      await deferredPrompt.userChoice.catch(() => {});
      deferredPrompt = null;
      setCanPrompt(false);
    } else {
      setShowIosHelp((s) => !s);
    }
  };

  return (
    <div className="install-app">
      <button type="button" className="pc-btn pc-btn--primary" onClick={install}>Install app</button>
      {showIosHelp && (
        <p className="install-app__help">
          On iPhone: tap the Share button in Safari, then <b>Add to Home Screen</b>.
        </p>
      )}
    </div>
  );
}
