# CampusEvents — Event Management Platform

A full-stack campus event platform. Students discover and register for events and get a QR code for
attendance; organizers create and run events (QR check-in, announcements, feedback, certificates);
admins approve organizers and events and manage the platform.

| Layer | Stack |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security (JWT + refresh-token cookie), Spring Data JPA / Hibernate, Quartz |
| Database | PostgreSQL (one schema per module) |
| Frontend | React 19, Vite 7, React Router 7, axios, Recharts, html5-qrcode |
| Integrations | Brevo (email), ZXing (QR codes), OpenHTMLtoPDF (certificates), Claude via the Anthropic Java SDK (optional AI features) |

## Features

**Visitors** — browse the home page, search/filter approved events, and view event details without an account.

**Students** — sign up / log in; register for events (a QR code is emailed); dashboard with upcoming and past
events, participation analytics, announcements, pending feedback; download certificates after attending and
giving feedback; edit profile, interests, preferences and social links; request their QR again.

**Organizers** (approved by an admin) — create events (visible after admin approval); add details, schedule,
FAQs, speakers, highlights and gallery images; add collaborators; scan attendee QR codes with the camera;
see attendees; mark events complete (updates dashboards once); send announcements; request and read feedback;
upload an HTML certificate template.

**AI assistance** — students get personal event recommendations (with the reasons shown), "You might also like"
suggestions on every event page, plain-English search ("free workshops this week") and an assistant that answers
questions about an event using only what its page says. Organizers can draft an event description and FAQs from a
few key points, turn rough notes into an announcement, and get a summary of attendee feedback (what worked, what
to improve). See [AI features](#ai-features).

**Admins** — overview of users, events, organizers and revenue; accept organizer/admin requests; approve or
reject events; manage profile catalogs (badges, interests, notification types, social platforms, preferences).

## Architecture

```
React SPA (localhost:5173) ──HTTP/JSON + JWT──▶ Spring Boot API (localhost:8080) ──JPA──▶ PostgreSQL
```

The backend is a modular monolith; modules talk to each other through small Java interfaces
(`EventApi`, `DashboardApi`, `AttendanceApi`, `NotificationApi`):

| Module | Package | Responsibility | DB schema |
|---|---|---|---|
| Auth | `authmodule` | signup/login, JWT + refresh tokens, roles, admin onboarding | `auth` |
| Events | `eventmodule` | events, details, registration, feedback, certificates, images, reminders | `events` |
| Participation | `participationmodule` | QR codes and attendance scanning | `attendance` |
| Analytics | `analyticsmodule` | student / organizer dashboards and stats | `analytics` |
| Profile | `profilemodule` | student profiles and admin-managed catalogs | `profile` |
| Notification | `notificationmodule` | email via Brevo | — |
| AI | `aimodule` | recommendations, similar events, smart search, event assistant, drafting, feedback insights (`AiApi`) | — |
| Scheduler | `schedulermodule` | scheduled emails (Quartz, JDBC job store) | `public.qrtz_*` |

Errors are returned as JSON: `{ "error": "...", "statusCode": 404, "timeStamp": "..." }`.

## Project structure

```
.
├── backend/                          # Spring Boot API
│   ├── pom.xml, mvnw                 # build (Maven wrapper)
│   ├── .env.example                  # backend configuration template
│   ├── src/main/java/com/event/      # backend modules (see table above)
│   ├── src/main/resources/           # application.yaml + config/*.yml per module
│   └── src/test/java/                # backend tests
└── frontend/                         # React app
    ├── src/                          # pages, components, API services
    ├── e2e/                          # Playwright end-to-end journeys
    └── .env.example                  # frontend configuration template
```

## Getting started

### Prerequisites

- Java 21 (the Maven wrapper downloads Maven)
- Node.js 20.19+ or 22.12+ (required by Vite 7)
- Docker (for PostgreSQL), or any PostgreSQL 15+

### 1. Database

```bash
docker run -d --name postgres_db -p 5432:5432 \
  -e POSTGRES_USER=admin -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=eventdb postgres:15
```

The schemas and tables are created automatically on the first backend start (Hibernate `ddl-auto: update`
plus an idempotent Quartz script); existing data is never dropped.

### 2. Backend

```bash
cd backend
cp .env.example .env      # then fill in the values (see Configuration)
./mvnw spring-boot:run    # http://localhost:8080
```

### 3. Frontend

```bash
cd frontend
cp .env.example .env      # VITE_API_URL=http://localhost:8080
npm install
npm run dev               # http://localhost:5173
```

### 4. First admin

Admins can only be granted by another admin, so the first one is created in the database. Sign up in the app,
then run:

```bash
docker exec postgres_db psql -U admin -d eventdb -c "
  update auth.app_user set role = '{ADMIN}' where username = 'you@example.com';
  insert into auth.admin (id, name) select user_id, 'Admin' from auth.app_user where username = 'you@example.com';"
```

Organizers sign up choosing the *Organizer* role, then an admin accepts them under **Dashboard → Accept Organizer**.

## Configuration

Backend settings are read from environment variables or from a git-ignored `backend/.env` file
(loaded from the directory you start the backend in).
Never commit real values.

| Variable | Required | Description |
|---|---|---|
| `JWT_SECRET` | yes | HMAC key for signing JWTs, ≥ 32 characters (`openssl rand -hex 32`) |
| `INTERNAL_SECRET` | yes | any random string |
| `BREVO_API_KEY`, `PERSONAL_EMAIL` | for email | Brevo API key and sender address. Without them emails fail and are logged; the app keeps working |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | no | Google OAuth client (must match the frontend's `VITE_GOOGLE_CLIENT_ID`) |
| `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET` | no | GitHub OAuth (server-side redirect flow) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | no | default `jdbc:postgresql://localhost:5432/eventdb`, `admin` / `admin` |
| `CORS_ALLOWED_ORIGINS` | no | frontend origin(s), comma-separated; default `http://localhost:5173` |
| `ANTHROPIC_API_KEY` | for AI | Claude API key. Without it recommendations and similar events still work, search falls back to keywords, feedback insights show counts only, and the other AI tools are hidden in the UI |
| `AI_MODEL`, `AI_TIMEOUT_SECONDS` | no | default `claude-opus-5-5`, `30` |
| `ANTHROPIC_BASE_URL` | no | proxy or local stub of the Messages API (empty = Anthropic) |

Frontend (`frontend/.env`): `VITE_API_URL` (backend URL) and `VITE_GOOGLE_CLIENT_ID`.

## AI features

All under `/ai`, login required unless noted. LLM-backed endpoints are rate limited per user and cached briefly;
emails and phone numbers are removed from anything sent to the model, and the model only sees public event data.

| Endpoint | Who | LLM | Where in the UI |
|---|---|---|---|
| `GET /ai/recommendations` | any user | no — transparent scoring (interests, department, past registrations, popularity, date) | Student dashboard → *Recommended for you* |
| `GET /ai/events/{id}/similar` | **public** | no — same category, shared topics, department, venue | Event page → *You might also like* |
| `GET /ai/search?q=` | any user | turns the query into the regular event filters; keyword fallback | Events page → smart search |
| `POST /ai/events/{id}/ask` | any user | answers only from the event page; says when the page doesn't cover it | Event page → *Ask about this event* |
| `POST /ai/events/generate-description` | organizer, admin | description + FAQs draft (FAQs can be saved with the new event) | Create Event → *Write with AI* |
| `POST /ai/events/{id}/draft-announcement` | event managers | title + message in a chosen tone | Announcements → *Draft with AI* |
| `GET /ai/events/{id}/feedback-summary` | event managers | sentiment, highlights, improvements (counts/average computed locally) | Feedback → *Feedback insights* |
| `GET /ai/status` | any user | — | lets the UI hide LLM tools when no key is set |

## Testing

```bash
# Backend: unit + integration tests (EventsApplicationTests needs the database running)
cd backend && ./mvnw test

# Frontend: lint and production build
cd frontend && npm run lint && npm run build

# End-to-end: 12 browser journeys against the running stack (setup in e2e/README.md)
npx playwright install chromium   # once
E2E_DB_NAME=eventdb_e2e npm run e2e
```

The end-to-end suite covers visitor browsing, signup, organizer onboarding, event creation and approval,
registration, QR attendance through a (fake) camera, completion, feedback, certificate download,
announcements, session refresh, profile editing and logout. Run it against a copy of the database.

## Deployment

**Backend** — `backend/Dockerfile` builds a production image (Java 21, non-root, `prod` profile):

```bash
docker build -t campusevents-api backend
docker run -p 8080:8080 -v campusevents-data:/data --env-file backend/.env campusevents-api
```

Most hosts (Render, Railway, Fly.io, a VM) can build this Dockerfile directly. Set `PORT` if the host assigns one.
**Attach a persistent disk at `/data`** — uploaded event images and certificate templates are stored there and
are lost on every redeploy otherwise.

**Frontend** — a static site: `cd frontend && npm ci && npm run build`, publish `frontend/dist`.
Set `VITE_API_URL` to the backend's public URL **at build time**. SPA fallbacks are included for Netlify/Render
(`public/_redirects`) and Vercel (`vercel.json`); on other hosts, rewrite unknown paths to `/index.html`.

**Production settings** (backend environment):

| Variable | Value |
|---|---|
| `JWT_SECRET`, `INTERNAL_SECRET` | new random values (not the development ones) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | your managed PostgreSQL |
| `CORS_ALLOWED_ORIGINS` | the frontend's public URL, e.g. `https://campusevents.example.com` |
| `FRONTEND_URL` | the same URL (used in email links) |
| `COOKIE_SECURE=true`, `COOKIE_SAME_SITE=None` | when frontend and backend are on **different domains**; otherwise users are logged out when their session refreshes. Requires HTTPS |
| `BREVO_API_KEY`, `PERSONAL_EMAIL` | email (QR codes, reminders); `PERSONAL_EMAIL` must be a sender verified in Brevo |
| `ANTHROPIC_API_KEY` | optional, enables the Claude-backed AI features |

Optional: `SHOW_SQL=true` logs SQL; `BREVO_URL` points email at a test server.

## Troubleshooting

- **Backend hangs right after the Spring banner** — if the project lives in an iCloud-synced folder
  (e.g. `~/Desktop`), iCloud creates duplicate files such as `Foo 2.class` in `backend/target/`. Delete it and
  move the project to a non-synced folder.
- **`Port 8080 already in use`** — find the process with `lsof -i :8080`.
- **QR / reminder emails not arriving** — check `BREVO_API_KEY`; failures are logged with the recipient and event.
- **Frontend shows network errors** — make sure `VITE_API_URL` points at the backend and the backend's
  `CORS_ALLOWED_ORIGINS` includes the frontend origin.

## Known limitations

- Google sign-in is not implemented yet (the button is hidden in the UI); GitHub OAuth is optional and untested.
- There is no password-reset flow.
- Emails require a Brevo account.
