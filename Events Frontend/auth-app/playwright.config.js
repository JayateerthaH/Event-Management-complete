import { defineConfig } from "@playwright/test";

/**
 * End-to-end tests against a running stack (see e2e/README.md):
 *   backend on E2E_API_URL (default http://localhost:8080), frontend dev server on E2E_BASE_URL (default :5173).
 */
export default defineConfig({
  testDir: "./e2e",
  timeout: 60_000,
  fullyParallel: false,
  workers: 1, // the journeys build on each other (organizer → event → registration → attendance ...)
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL || "http://localhost:5173",
    viewport: { width: 1366, height: 900 },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
});
