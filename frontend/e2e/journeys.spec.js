import { test, expect, chromium } from "@playwright/test";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { dashboardLoaded, API, ADMIN, PASSWORD, login, openTab, trackErrors, readQrCode } from "./support/helpers.js";
import { writeQrVideo } from "./support/qrVideo.js";

/**
 * Full user journeys across frontend + backend + database, in the order they happen in real life:
 * organizer onboarding → event creation → admin approval → student registration → QR attendance →
 * completion → feedback → certificate. Each step uses its own browser context (one per person).
 */
test.describe.serial("CampusEvents journeys", () => {
  const run = Date.now().toString(36);
  const organizerEmail = `org.${run}@example.com`;
  const studentEmail = `stu.${run}@example.com`;
  const eventName = `E2E Tech Talk ${run}`;
  let eventId;

  async function asUser(browser, email, password, fn) {
    const context = await browser.newContext();
    const page = await context.newPage();
    page.on("dialog", (dialog) => dialog.accept()); // "Mark this event as completed?" etc.
    const errors = trackErrors(page);
    try {
      if (email) await login(page, email, password);
      await fn(page);
    } finally {
      await context.close();
    }
    expect(errors, "no server errors or page crashes").toEqual([]);
  }

  test("visitor can browse home, events and an event page without logging in", async ({ browser }) => {
    await asUser(browser, null, null, async (page) => {
      await page.goto("/home");
      await expect(page.locator(".stat-value").first()).not.toHaveText("...");
      await expect(page.locator(".stat-value").first()).not.toHaveText("0");

      await page.goto("/events");
      await expect(page.locator(".event-card").first()).toBeVisible();
      await expect(page).toHaveURL(/\/events$/); // not bounced to /login

      await page.locator(".event-card").first().click();
      await expect(page).toHaveURL(/\/events\/\d+$/);
      await expect(page.locator("h1").first()).not.toBeEmpty();
      await expect(page.getByText("Add to Calendar")).toBeVisible();
      // Logged-out navbar offers Login/Sign Up rather than account links
      await expect(page.locator("nav.home-navbar").getByRole("link", { name: "Login" })).toBeVisible();
      await expect(page.locator("nav.home-navbar").getByRole("link", { name: "Dashboard" })).toHaveCount(0);
    });
  });

  test("protected pages redirect to login, and a wrong password shows an error", async ({ browser }) => {
    await asUser(browser, null, null, async (page) => {
      await page.goto("/dashboard");
      await expect(page).toHaveURL(/\/login$/);

      // Field icons sit beside the inputs, never on top of the typed text
      for (const field of ["#username", "#password"]) {
        const iconLocator = page.locator(`.input-with-icon:has(${field}) .input-icon`);
        if (!(await iconLocator.isVisible())) continue; // phones hide the field icons (Mobile.css)
        const icon = await iconLocator.boundingBox();
        const input = await page.locator(field).boundingBox();
        expect(icon.x + icon.width).toBeLessThanOrEqual(input.x);
      }

      await page.fill("#username", ADMIN.email);
      await page.fill("#password", "wrong-password");
      await page.click("button[type=submit]");
      await expect(page.locator(".error-message")).toContainText("Invalid email or password");
    });
  });

  test("organizer signs up, admin accepts the request", async ({ browser }) => {
    await asUser(browser, null, null, async (page) => {
      await page.goto("/signup");
      await page.locator(".role-option", { hasText: "Organizer" }).click();
      await page.fill('input[name="username"]', organizerEmail);
      await page.fill('input[name="password"]', PASSWORD);
      await page.fill('input[name="confirmPassword"]', PASSWORD);
      await page.check('input[name="agreeToTerms"]');
      await page.click("button[type=submit]");
      await page.waitForURL("**/dashboard");
      await dashboardLoaded(page);
    });

    await asUser(browser, ADMIN.email, ADMIN.password, async (page) => {
      await openTab(page, "Accept Organizer");
      const row = page.locator("tr", { hasText: organizerEmail });
      await row.getByRole("button").click();
      await expect(row).toHaveCount(0);
    });
  });

  test("organizer creates an event; it stays hidden until an admin approves it", async ({ browser }) => {
    await asUser(browser, organizerEmail, PASSWORD, async (page) => {
      await expect(page.locator("aside")).toContainText("Organizer Panel");
      await openTab(page, "Create Event");
      await page.fill('input[name="name"]', eventName);
      await page.selectOption('select[name="category"]', "Technical");
      await page.fill('textarea[name="description"]', "A talk created by the end-to-end suite.");
      await page.fill('input[name="field"]', "CSE");
      await page.fill('input[name="eventDate"]', "2026-12-15");
      await page.fill('input[name="venue"]', "Auditorium");
      await page.fill('input[name="maxPeople"]', "50");
      await page.fill('input[name="costOfRegistration"]', "0");
      await page.getByRole("button", { name: "+ Create Event" }).click();
      await expect(page.getByText("Event created successfully")).toBeVisible();

      const token = await page.evaluate(() => localStorage.getItem("jwt_token"));
      const res = await page.request.get(`${API}/event/organizer/all`, { headers: { Authorization: `Bearer ${token}` } });
      const created = (await res.json()).find((e) => e.name === eventName);
      expect(created.isApproved).toBe(false);
      eventId = created.id;
    });

    const anon = await (await browser.newContext()).newPage();
    expect((await anon.request.get(`${API}/events/${eventId}`)).status()).toBe(404);
    await anon.context().close();

    await asUser(browser, ADMIN.email, ADMIN.password, async (page) => {
      await openTab(page, "Events");
      await page.getByPlaceholder(/Search by title/).fill(eventName); // list is paginated
      const row = page.locator("tr", { hasText: eventName });
      await row.locator('button[title="Approve Event"]').click();
      await expect(row.locator('button[title="Approve Event"]')).toHaveCount(0);
    });
  });

  test("student signs up and registers for the approved event", async ({ browser }) => {
    await asUser(browser, null, null, async (page) => {
      await page.goto("/signup");
      await page.fill('input[name="username"]', studentEmail);
      await page.fill('input[name="password"]', PASSWORD);
      await page.fill('input[name="confirmPassword"]', PASSWORD);
      await page.check('input[name="agreeToTerms"]');
      await page.click("button[type=submit]");
      await page.waitForURL("**/dashboard");
      await dashboardLoaded(page);

      await page.goto(`/events/${eventId}`);
      await page.getByText("Register Now").first().click();
      const modal = page.locator(".reg-modal");
      await modal.getByPlaceholder("Enter your full name").fill("Stu Dent");
      await modal.getByPlaceholder("your.email@example.com").fill(studentEmail);
      await modal.getByPlaceholder("+91 98765 43210").fill("9876543210");
      await modal.locator("select").nth(0).selectOption({ index: 1 });
      await modal.locator("select").nth(1).selectOption({ index: 1 });
      await modal.locator('input[type="checkbox"]').check();
      await modal.locator('button[type="submit"]').click();
      await expect(page.getByText("You're Registered!")).toBeVisible();

      await page.reload();
      await expect(page.getByText("Already Registered ✓")).toBeVisible();

      // "Add to Calendar" downloads an all-day calendar entry (it used to open the registration form)
      const downloadPromise = page.waitForEvent("download");
      await page.getByRole("button", { name: "Add to Calendar" }).click();
      const ics = fs.readFileSync(await (await downloadPromise).path(), "utf8");
      expect(ics).toContain(`SUMMARY:${eventName}`);
      expect(ics).toContain("DTSTART;VALUE=DATE:20261215");
      await expect(page.locator(".reg-modal")).toHaveCount(0);
    });
  });

  test("student can have their QR code emailed again", async ({ browser }) => {
    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await page.goto(`/events/${eventId}`);
      const button = page.locator(".ed-sidebar").getByRole("button", { name: "Resend QR code" });
      const message = page.locator(".ed-sidebar .resend-qr__msg");
      await button.click();
      // Success needs a working mail provider (Brevo, or a stub via BREVO_URL); otherwise the backend reports 502
      await expect(message).toHaveText(/QR code sent to|Email could not be sent/);
      if ((await message.getAttribute("class")).includes("--success")) {
        await expect(message).toContainText(studentEmail);
        await button.click();
        await expect(message).toContainText("Please wait"); // 60s cooldown protects the email quota
      }
    });
  });

  test("organizer scans the student's QR code with the camera", async () => {
    const qrCode = await readQrCode(studentEmail, eventId);
    const video = path.join(os.tmpdir(), `qr-${run}.y4m`);
    writeQrVideo(qrCode, video);

    const camBrowser = await chromium.launch({
      args: ["--use-fake-ui-for-media-stream", "--use-fake-device-for-media-stream", `--use-file-for-fake-video-capture=${video}`],
    });
    try {
      // Same screen as the project (a phone in the android/iphone runs); the fake camera needs Chromium
      const { baseURL, viewport, isMobile, hasTouch, deviceScaleFactor, userAgent } = test.info().project.use;
      const context = await camBrowser.newContext({ baseURL, viewport, isMobile, hasTouch, deviceScaleFactor, userAgent, permissions: ["camera"] });
      const page = await context.newPage();
      await login(page, organizerEmail);
      await page.goto(`/scan/${eventId}`);
      await page.getByRole("button", { name: "Start Scanner" }).click();
      await expect(page.locator(".qr-result.success")).toContainText("Attendance marked successfully", { timeout: 20_000 });

      // Scanning the same code again is rejected with the backend's message (not a generic one)
      await page.getByRole("button", { name: "Scan Next" }).click();
      await expect(page.locator(".qr-result.error")).toContainText("Attendance already marked", { timeout: 20_000 });
    } finally {
      await camBrowser.close();
      fs.rmSync(video, { force: true });
    }
  });

  test("organizer completes the event, sees the attendee, and requests feedback", async ({ browser }) => {
    await asUser(browser, organizerEmail, PASSWORD, async (page) => {
      await openTab(page, "Manage Events");
      const row = page.locator("tr", { hasText: eventName }).first();
      await row.getByRole("button", { name: "Mark Complete" }).click();
      await expect(page.locator("tr", { hasText: eventName }).first()).toContainText("Completed");

      await openTab(page, "Attendees");
      await expect(page.locator("tr", { hasText: studentEmail })).toBeVisible();

      await openTab(page, "Feedback");
      await page.locator("select").first().selectOption(String(eventId));
      await page.getByRole("button", { name: "Request Feedback" }).click();
      await expect(page.getByText("Feedback request sent")).toBeVisible();

      await openTab(page, "Certificates");
      await page.locator(".od-cert-form select").selectOption(String(eventId));
      await page.locator('.od-cert-form input[type="file"]').setInputFiles({
        name: "certificate.html",
        mimeType: "text/html",
        buffer: Buffer.from("<html><body><h1>Certificate</h1><p>{{name}} — {{course}}</p></body></html>"),
      });
      await page.locator('.od-cert-form button[type="submit"]').click();
      await expect(page.locator(".od-cert-form").locator("..")).toContainText(/uploaded/i);
    });
  });

  test("student gives feedback, then downloads the certificate", async ({ browser }) => {
    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await openTab(page, "Pending Feedback");
      await page.getByRole("button", { name: "Give Feedback" }).first().click();
      await page.getByRole("button", { name: "5 stars" }).click();
      await page.locator(".fb-modal textarea").fill("Great session!");
      await page.locator('.fb-modal button[type="submit"]').click();
      await expect(page.locator(".fb-modal")).toContainText(/thank/i);
      await page.locator(".fb-modal").getByRole("button", { name: "Close" }).click();

      await openTab(page, "Certificates");
      const card = page.locator("text=" + eventName).first();
      await expect(card).toBeVisible();
      const downloadPromise = page.waitForEvent("download");
      await page.getByRole("button", { name: "Download" }).first().click();
      const download = await downloadPromise;
      const file = await download.path();
      expect(fs.readFileSync(file).subarray(0, 5).toString()).toBe("%PDF-");
    });
  });

  test("organizer sends an announcement and the registered student is notified", async ({ browser }) => {
    const title = `Room change ${run}`;
    await asUser(browser, organizerEmail, PASSWORD, async (page) => {
      await openTab(page, "Announcements");
      await page.locator("main select").first().selectOption(String(eventId));
      await page.getByPlaceholder("Announcement title...").fill(title);
      await page.getByPlaceholder("Type your announcement...").fill("Now in Hall B.");
      await page.getByRole("button", { name: "Send" }).click();
      await expect(page.getByText("Announcement sent successfully!")).toBeVisible();
    });
    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await page.locator("button[title=Notifications]").click();
      await expect(page.getByText(title)).toBeVisible();
    });
  });

  test("an expiring session is refreshed with the HttpOnly cookie", async ({ browser }) => {
    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      const result = await page.evaluate(async (api) => {
        const res = await fetch(`${api}/auth/refresh`, { method: "POST", credentials: "include" });
        return { status: res.status, body: await res.json() };
      }, API);
      expect(result.status).toBe(200);
      expect(result.body.accessToken).toBeTruthy();
    });
  });

  test("student edits the profile and it persists after reload", async ({ browser }) => {
    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await page.goto("/profile");
      await page.getByRole("button", { name: /Edit/ }).first().click();
      await page.locator('input[name="name"]').fill("Stu Dent Updated");
      await page.getByRole("button", { name: "Save Changes" }).click();
      await expect(page.getByText("Profile saved successfully!")).toBeVisible();
      await page.reload();
      await expect(page.getByText("Stu Dent Updated").first()).toBeVisible();
    });
  });

  test("logout ends the session and revokes the refresh token", async ({ browser }) => {
    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await page.locator(".user-avatar[title=Account]").click();
      await page.locator(".nav-dropdown__item--logout").click();
      await page.waitForURL("**/login"); // the app only leaves once the session is revoked
      await page.waitForLoadState("load");
      // The route guard redirects straight to /login, so only wait for the navigation to start
      await page.goto("/dashboard", { waitUntil: "commit" });
      await expect(page).toHaveURL(/\/login$/);

      // The browser still holds no usable refresh cookie: the server revoked it
      const refresh = await page.request.post(`${API}/auth/refresh`);
      expect(refresh.status()).toBe(401);
    });
  });

});
