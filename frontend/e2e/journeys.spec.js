import { test, expect, chromium } from "@playwright/test";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { dashboardLoaded, API, ADMIN, PASSWORD, login, openTab, trackErrors, readQrCode, gotoSettled } from "./support/helpers.js";
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
      // New accounts are offered the interest picker first; it can be skipped
      await page.waitForURL("**/welcome");
      await page.getByRole("button", { name: "Skip for now" }).click();
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
      // Pick an interest (event categories are offered) so the first suggestions fit
      await page.waitForURL("**/welcome");
      const chip = page.locator(".welcome-chips button").first();
      const topic = (await chip.innerText()).trim();
      await chip.click();
      await page.getByRole("button", { name: /Continue with 1 topic/ }).click();
      await page.waitForURL("**/dashboard");
      const saved = await page.evaluate(async (api) => {
        const res = await fetch(`${api}/management/interests`, { headers: { Authorization: `Bearer ${localStorage.getItem("jwt_token")}` } });
        return (await res.json()).map((i) => i.interestName);
      }, API);
      expect(saved).toEqual([topic]);
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

  test("sharing: link previews carry the event's title and image; the Share menu offers WhatsApp", async ({ browser, request }) => {
    // What WhatsApp/Telegram crawlers get (vercel.json routes them here)
    const preview = await (await request.get(`${API}/public/share/events/${eventId}`)).text();
    expect(preview).toContain(`<meta property="og:title" content="${eventName}">`);
    expect(preview).toMatch(/<meta property="og:image" content="https?:\/\/[^"]+">/);
    expect(preview).toContain(`/events/${eventId}"`);

    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await gotoSettled(page, `/events/${eventId}`);
      const share = page.locator(".ed-sidebar").getByRole("button", { name: "Share" });
      await expect(share).toBeVisible();
      if (test.info().project.name === "desktop") {
        await page.evaluate(() => { delete Navigator.prototype.share; }); // desktop: no share sheet
        await share.click();
        const wa = page.getByRole("menuitem", { name: "Share on WhatsApp" });
        await expect(wa).toHaveAttribute("href", new RegExp(`^https://wa\\.me/\\?text=.*%2Fevents%2F${eventId}$`));
      }
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

  test("student saves an event, finds it on the dashboard, and removes it", async ({ browser, request }) => {
    // Another upcoming event (from an earlier run) than the one the student registered for
    const res = await request.post(`${API}/events/filter`, { data: { page: 0, size: 10, search: "E2E Tech Talk" } });
    const other = (await res.json()).content.find((e) => e.id !== eventId && new Date(e.eventDate) > new Date());
    expect(other, "an upcoming test event to save").toBeTruthy();

    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await gotoSettled(page, `/events/${other.id}`);
      const sidebar = page.locator(".ed-sidebar");
      await sidebar.getByRole("button", { name: "Save this event" }).click();
      await expect(sidebar.getByRole("button", { name: "Remove from saved events" })).toBeVisible();

      await page.goto("/dashboard");
      const saved = page.locator(".saved-events");
      await expect(saved.getByRole("link", { name: other.name })).toBeVisible();
      await saved.locator("li", { hasText: other.name }).getByRole("button", { name: "Remove from saved events" }).click();
      await expect(saved.getByRole("link", { name: other.name })).toHaveCount(0);
      await page.reload();
      await expect(page.locator(".saved-events").getByRole("link", { name: other.name })).toHaveCount(0);
    });
  });

  test("event buddies: two attendees opt in, say hi, accept, and see each other as friends", async ({ browser, request }) => {
    // A second attendee, created and registered through the API
    const buddyEmail = `buddy.${run}@example.com`;
    await request.post(`${API}/auth/signup`, { data: { username: buddyEmail, password: PASSWORD, roles: ["STUDENT"] } });
    const login = await request.post(`${API}/auth/login`, { data: { username: buddyEmail, password: PASSWORD } });
    const token = (await login.json()).jwt;
    const reg = await request.post(`${API}/event/${eventId}/register`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { name: "Bud Dy", email: buddyEmail, phone: "9876500000", department: "CSE", yearOfStudy: "2" },
    });
    expect(reg.status()).toBe(201);

    const setName = async (page, name) => {
      await gotoSettled(page, "/friends?tab=settings");
      await page.locator("#set-name").fill(name);
      await page.getByRole("button", { name: "Save settings" }).click();
      await expect(page.getByRole("status").filter({ hasText: "Saved" })).toBeVisible();
    };

    await asUser(browser, buddyEmail, PASSWORD, async (page) => {
      await setName(page, `Buddy ${run}`);
      await page.goto(`/events/${eventId}`);
      await page.getByRole("button", { name: "I'm open to meeting people" }).click();
      await expect(page.getByText("Nobody else has opted in yet")).toBeVisible();
    });

    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await page.goto(`/events/${eventId}`);
      // A name is required before opting in
      await expect(page.getByRole("button", { name: "I'm open to meeting people" })).toBeDisabled();
      await setName(page, `Stu ${run}`);
      await page.goto(`/events/${eventId}`);
      await page.getByRole("button", { name: "I'm open to meeting people" }).click();
      const card = page.locator(".pc").filter({ hasText: `Buddy ${run}` });
      await expect(card).toBeVisible();
      await card.getByRole("button", { name: "Say hi" }).click();
      await expect(card.getByText("Requested")).toBeVisible();
    });

    await asUser(browser, buddyEmail, PASSWORD, async (page) => {
      await page.locator("button[title=Notifications]").click();
      await page.getByText(`Stu ${run} wants to connect`).click();
      await expect(page).toHaveURL(/\/friends\?tab=requests/);
      await page.locator(".pc").filter({ hasText: `Stu ${run}` }).getByRole("button", { name: "Accept request" }).click();
      await expect(page.locator(".pc").filter({ hasText: `Stu ${run}` })).toHaveCount(0); // accepted: leaves the list
      await page.getByRole("tab", { name: "Friends" }).click();
      await expect(page.locator(".pc").filter({ hasText: `Stu ${run}` }).getByText("Friend", { exact: true })).toBeVisible();
      // The event page lists friends who are going
      await page.goto(`/events/${eventId}`);
      await expect(page.locator(".eb__chips").getByText(`Stu ${run}`)).toBeVisible();
    });

    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await page.locator("button[title=Notifications]").click();
      await expect(page.getByText(`Buddy ${run} accepted your request`)).toBeVisible();
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

  test("organizer duplicates the completed event to a new date; the copy awaits approval", async ({ browser }) => {
    const copyName = `${eventName} (next term)`;
    await asUser(browser, organizerEmail, PASSWORD, async (page) => {
      await openTab(page, "Manage Events");
      const row = page.locator("tr", { hasText: eventName }).filter({ hasText: "Completed" }).first();
      await row.getByRole("button", { name: `Duplicate ${eventName}` }).click();
      const dialog = page.getByRole("dialog", { name: "Duplicate event" });
      await dialog.getByLabel("Name").fill(copyName);
      await dialog.getByLabel(/New date and time/).fill("2027-01-20T10:00");
      await dialog.getByRole("button", { name: "Duplicate", exact: true }).click();
      await expect(dialog.getByText(/waiting for admin approval/)).toBeVisible();
      await dialog.getByRole("button", { name: "Done" }).click();
      const copyRow = page.locator("tr", { hasText: copyName });
      await expect(copyRow).toBeVisible();
      await expect(copyRow).not.toContainText("Completed");
    });
  });

  test("student follows the organizer, hears about their next approved event, and can turn that off", async ({ browser }) => {
    const copyName = `${eventName} (next term)`; // created by the duplicate journey, awaiting approval
    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await gotoSettled(page, `/events/${eventId}`);
      const org = page.locator(".follow-org");
      await org.getByRole("button", { name: "Follow" }).click();
      await expect(org.getByRole("button", { name: "Following" })).toBeVisible();
      await expect(org).toContainText("1 follower");
      await page.goto("/friends?tab=following");
      await expect(page.locator(".following-list li").first()).toBeVisible();
    });

    await asUser(browser, ADMIN.email, ADMIN.password, async (page) => {
      await openTab(page, "Events");
      await page.getByPlaceholder(/Search by title/).fill(copyName);
      const row = page.locator("tr", { hasText: copyName });
      await row.locator('button[title="Approve Event"]').click();
      await expect(row.locator('button[title="Approve Event"]')).toHaveCount(0);
    });

    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      await page.locator("button[title=Notifications]").click();
      await page.getByText(`: ${copyName}`).click();
      await expect(page).toHaveURL(/\/events\/\d+$/);

      // Notification settings: turn off new-event alerts; it sticks
      await page.locator("button[title=Settings]").click();
      await expect(page).toHaveURL(/\/settings$/);
      const box = page.getByLabel(/New events from organizers you follow/);
      await expect(box).toBeChecked();
      await box.uncheck();
      await expect(page.getByRole("status").filter({ hasText: "Saved" })).toBeVisible();
      await page.reload();
      await expect(page.getByLabel(/New events from organizers you follow/)).not.toBeChecked();
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

  test("attending, feedback and a certificate earn badges, announced in the bell", async ({ browser }) => {
    await asUser(browser, studentEmail, PASSWORD, async (page) => {
      const badges = page.locator(".achievements");
      await expect(badges).toBeVisible();
      for (const name of ["First step", "Certified"]) {
        await expect(badges.locator(".badge-tile.is-earned", { hasText: name })).toBeVisible();
      }
      await expect(badges.locator(".badge-tile:not(.is-earned)", { hasText: "Regular" }).getByText("1/5")).toBeVisible();
      await page.locator("button[title=Notifications]").click();
      await expect(page.getByText('You earned the "First step" badge').first()).toBeVisible();
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

  test("admin sees engagement: active users and what people did", async ({ browser }) => {
    test.setTimeout(150_000); // the digest run walks every student in the test database
    await asUser(browser, ADMIN.email, ADMIN.password, async (page) => {
      await openTab(page, "Analytics");
      const panel = page.locator(".engagement");
      await expect(panel.getByText("Active today")).toBeVisible();
      const activeToday = panel.locator(".engagement__tiles > div", { hasText: "Active today" }).locator(".engagement__value");
      await expect(activeToday).not.toHaveText("0");
      for (const label of ["Signups", "Registrations", "Check-ins"]) {
        await expect(panel.locator(".engagement__counts li", { hasText: label })).toBeVisible();
      }
      // Weekly digest can be sent on demand (never twice in a week per student)
      await panel.getByRole("button", { name: "Send this week's digest now" }).click();
      await expect(panel.getByRole("status")).toContainText(/Sent \d+ digest/, { timeout: 90_000 });
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
