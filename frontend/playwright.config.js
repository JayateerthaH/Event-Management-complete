import { defineConfig, devices } from "@playwright/test";

/**
 * End-to-end tests against a running stack (see e2e/README.md):
 *   backend on E2E_API_URL (default http://localhost:8080), frontend dev server on E2E_BASE_URL (default :5173).
 * E2E_MOBILE=1 adds phone runs: an Android phone (Chromium) and an iPhone (WebKit), e.g.
 *   E2E_MOBILE=1 npx playwright test --project=android --project=iphone
 */
const phones = process.env.E2E_MOBILE
  ? [
      { name: "android", use: { ...devices["Pixel 7"] } },
      // Playwright's WebKit can't run the offline ticket check, and the service worker taking control a few
      // seconds after load races the journeys' next page.goto there (aborted or crashed navigations).
      { name: "iphone", use: { ...devices["iPhone 13"], serviceWorkers: "block" } },
    ]
  : [];

export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  fullyParallel: false,
  workers: 1, // the journeys build on each other (organizer → event → registration → attendance ...)
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://localhost:5173",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "desktop", use: { viewport: { width: 1366, height: 900 } } }, ...phones],
});
