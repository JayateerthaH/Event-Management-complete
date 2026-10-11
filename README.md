# CampusEvents — Event Management Platform

A full-stack campus event platform. Students discover and register for events and get a QR code for
attendance; organizers create and run events (QR check-in, announcements, feedback, certificates);
admins approve organizers and events and manage the platform.

| Layer | Stack |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security (JWT + refresh-token cookie), Spring Data JPA / Hibernate, Quartz |
| Database | PostgreSQL (one schema per module) |
| Frontend | React 19, Vite 7, React Router 7, axios, Recharts, html5-qrcode |
| Integrations | Brevo (email), ZXing (QR codes), OpenHTMLtoPDF (certificates) |

## Features

**Visitors** — browse the home page, search/filter approved events, and view event details without an account.

**Students** — sign up / log in; register for events (a QR code is emailed); dashboard with upcoming and past
events, participation analytics, announcements, pending feedback; download certificates after attending and
giving feedback; edit profile, interests, preferences and social links; request their QR again.

**Organizers** (approved by an admin) — create events (visible after admin approval); add details, schedule,
FAQs, speakers, highlights and gallery images; add collaborators; scan attendee QR codes with the camera;
see attendees; mark events complete (updates dashboards once); send announcements; request and read feedback;
upload an HTML certificate template.

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

Frontend (`frontend/.env`): `VITE_API_URL` (backend URL) and `VITE_GOOGLE_CLIENT_ID`.

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
