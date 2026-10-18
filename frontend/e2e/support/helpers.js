import { expect } from "@playwright/test";
import { execFileSync } from "node:child_process";

export const API = process.env.E2E_API_URL || "http://localhost:8080";
export const PASSWORD = "Passw0rd!";
export const ADMIN = {
  email: process.env.E2E_ADMIN_EMAIL || "e2e.admin@example.com",
  password: process.env.E2E_ADMIN_PASSWORD || PASSWORD,
};

/** Logs in through the real login form and waits for the dashboard. */
export async function login(page, email, password = PASSWORD) {
  await page.goto("/login");
  await page.fill("#username", email);
  await page.fill("#password", password);
  await page.click("button[type=submit]");
  await page.waitForURL("**/dashboard");
  await dashboardLoaded(page);
}

/**
 * Wait until the dashboard has finished its requests, as a person would before moving on. WebKit (iPhone)
 * reports requests cancelled by leaving a page mid-load as errors, which the suite would count.
 */
export async function dashboardLoaded(page) {
  await page.waitForLoadState("networkidle");
}

/** Clicks a dashboard sidebar tab (scoped to the sidebar; "Events" also exists in the navbar). */
export async function openTab(page, label) {
  await page.locator("aside").getByText(label, { exact: true }).first().click();
}

/** Collects failed backend calls and page crashes so each journey can assert there were none. */
export function trackErrors(page) {
  const errors = [];
  page.on("pageerror", (e) => errors.push(`page error: ${e.message}`));
  page.on("response", (r) => {
    if (r.url().startsWith(API) && r.status() >= 500) errors.push(`${r.status()} ${r.url()}`);
  });
  return errors;
}

/**
 * The attendance QR is only delivered by email, so the suite reads the code from the database
 * (the same value the emailed QR image encodes). Configure with E2E_DB_CONTAINER / E2E_DB_NAME.
 */
export async function readQrCode(email, eventId) {
  const container = process.env.E2E_DB_CONTAINER || "postgres_db";
  const db = process.env.E2E_DB_NAME || "eventdb";
  if (!/^[\w.@+-]+$/.test(email) || !/^\d+$/.test(String(eventId))) throw new Error("unexpected input");
  // The QR record is created asynchronously after registration, so poll briefly
  for (let attempt = 0; attempt < 20; attempt++) {
    const out = execFileSync("docker", ["exec", container, "psql", "-U", process.env.E2E_DB_USER || "admin", "-d", db, "-Atc",
      `select random_id from attendance.users where email='${email}' and event_id=${eventId} limit 1`]);
    const code = out.toString().trim();
    if (code) return code;
    await new Promise((r) => setTimeout(r, 500));
  }
  expect.soft(false, "QR code stored for registration within 10s").toBe(true);
  throw new Error("No QR code stored for the registration");
}

/**
 * page.goto that retries once when WebKit reports the navigation was interrupted by the app's own
 * post-login redirect still settling (seen only on the iPhone project, intermittently; WebKit words it
 * either way).
 */
export async function gotoSettled(page, url) {
  try {
    await page.goto(url);
  } catch (e) {
    if (!/interrupted by another navigation|Frame load interrupted/.test(e.message)) throw e;
    await page.waitForLoadState("networkidle");
    await page.goto(url);
  }
}
