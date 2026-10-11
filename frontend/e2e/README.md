# End-to-end tests (Playwright)

Real user journeys through the browser against the running stack: visitor browsing, signup, organizer
onboarding, event creation and admin approval, registration, QR attendance via a fake camera, completion,
feedback, certificate download, announcements, session refresh, profile, logout.

## Prerequisites

1. Postgres running (Docker container `postgres_db`), backend on `:8080`, frontend dev server on `:5173`.
2. An admin account. The suite creates its own organizer/student users, but an admin can only be made in the
   database. Use a **copy** of the database for testing, e.g.:

   ```bash
   docker exec postgres_db createdb -U admin eventdb_e2e
   docker exec postgres_db pg_dump -U admin eventdb | docker exec -i postgres_db psql -U admin -d eventdb_e2e -q
   # start the backend against it (from backend/):  DB_URL='jdbc:postgresql://localhost:5432/eventdb_e2e?currentSchema=public' ./mvnw spring-boot:run
   # sign up e2e.admin@example.com / Passw0rd! in the app, then:
   docker exec postgres_db psql -U admin -d eventdb_e2e -c "update auth.app_user set role='{ADMIN}' where username='e2e.admin@example.com'; insert into auth.admin(id,name) select user_id,'E2E Admin' from auth.app_user where username='e2e.admin@example.com';"
   ```

3. `npx playwright install chromium` (once).

## Run

```bash
E2E_DB_NAME=eventdb_e2e npx playwright test
```

| Variable | Default | Purpose |
|---|---|---|
| `E2E_BASE_URL` | `http://localhost:5173` | frontend |
| `E2E_API_URL` | `http://localhost:8080` | backend |
| `E2E_ADMIN_EMAIL` / `E2E_ADMIN_PASSWORD` | `e2e.admin@example.com` / `Passw0rd!` | admin account |
| `E2E_DB_CONTAINER` / `E2E_DB_NAME` / `E2E_DB_USER` | `postgres_db` / `eventdb` / `admin` | where the QR code is read from (it is normally only emailed) |

Each run creates uniquely named users and an event, so it can be repeated.
