/* CampusEvents service worker: keeps the app shell and the tickets page working offline.
 * API calls are never cached here; the tickets page keeps its own copy of the student's tickets. */
const VERSION = "ce-v1";
const SHELL = ["/", "/index.html", "/manifest.webmanifest", "/icons/icon-192.png", "/icons/icon-512.png"];

self.addEventListener("install", (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(VERSION);
    await cache.addAll(SHELL);
    // Also cache the entry script and styles named in index.html (their names change every build)
    try {
      const html = await (await fetch("/index.html", { cache: "no-store" })).text();
      const assets = [...html.matchAll(/(?:src|href)="(\/assets\/[^"]+)"/g)].map((m) => m[1]);
      await cache.addAll(assets);
      await cache.put("/index.html", new Response(html, { headers: { "Content-Type": "text/html" } }));
    } catch { /* offline during install: the shell above is enough to start */ }
    self.skipWaiting();
  })());
});

self.addEventListener("activate", (event) => {
  event.waitUntil((async () => {
    for (const key of await caches.keys()) if (key !== VERSION) await caches.delete(key);
    await self.clients.claim();
  })());
});

// The page lists the built assets it already loaded before this worker took control, so they're cached too
self.addEventListener("message", (event) => {
  const urls = (event.data && event.data.type === "cache-assets" && Array.isArray(event.data.urls)) ? event.data.urls : [];
  const same = urls.filter((u) => { try { const x = new URL(u); return x.origin === self.location.origin && x.pathname.startsWith("/assets/"); } catch { return false; } });
  if (same.length) event.waitUntil(caches.open(VERSION).then((c) => c.addAll(same)).catch(() => {}));
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin || url.pathname.startsWith("/api/")) return;

  // Pages: network first so updates arrive; offline, the cached app shell (React Router does the rest)
  if (req.mode === "navigate") {
    event.respondWith((async () => {
      try {
        const res = await fetch(req);
        if (res.ok) (await caches.open(VERSION)).put("/index.html", res.clone());
        return res;
      } catch {
        return (await caches.match("/index.html")) || Response.error();
      }
    })());
    return;
  }

  // Built assets are content-hashed: cache first, and keep what we fetch (lazy pages, QR library)
  if (url.pathname.startsWith("/assets/") || url.pathname.startsWith("/icons/")) {
    event.respondWith((async () => {
      const hit = await caches.match(req);
      if (hit) return hit;
      const res = await fetch(req);
      if (res.ok) (await caches.open(VERSION)).put(req, res.clone());
      return res;
    })());
  }
});

// Phone notifications (Web Push): show what the server sent; tapping opens the right page in the app
self.addEventListener("push", (event) => {
  let data = {};
  try { data = event.data ? event.data.json() : {}; } catch { data = { body: event.data && event.data.text() }; }
  event.waitUntil(self.registration.showNotification(data.title || "CampusEvents", {
    body: data.body || "",
    icon: "/icons/icon-192.png",
    badge: "/icons/icon-192.png",
    tag: data.tag || undefined,
    data: { url: data.url || "/dashboard" },
  }));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const target = new URL((event.notification.data && event.notification.data.url) || "/dashboard", self.location.origin).href;
  event.waitUntil((async () => {
    const windows = await self.clients.matchAll({ type: "window", includeUncontrolled: true });
    for (const w of windows) {
      if (new URL(w.url).origin === self.location.origin && "focus" in w) {
        await w.focus();
        if ("navigate" in w) return w.navigate(target);
        return undefined;
      }
    }
    return self.clients.openWindow(target);
  })());
});
