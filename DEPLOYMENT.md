# Deployment Guide

## Environments

| Profile | Config file | Used for |
|---|---|---|
| `dev` | `application-dev.yml` | Local development (bare-metal or `docker compose up`) |
| `test` | `application-test.yml` | Automated tests (H2, Flyway disabled) — never used to serve traffic |
| `staging` | `application-staging.yml` | Pre-production, external Oracle instance |
| `prod` | `application-prod.yml` | Production — stricter logging, no Flyway auto-baseline, no stack traces in error responses |

Select one with `SPRING_PROFILES_ACTIVE=<profile>`.

## 1. Local development (no Docker)

```bash
docker compose up -d oracle-xe     # just the database
cd backend && mvn spring-boot:run  # SPRING_PROFILES_ACTIVE defaults to dev
cd frontend && npm start           # ng serve on :4200
```

Requires `JWT_SECRET` set in your shell or a local `.env` sourced beforehand —
see `.env.example`. `JwtService` refuses to start with a blank secret.

## 2. Full local stack via Docker Compose

```bash
cp .env.example .env
# edit .env: set JWT_SECRET (openssl rand -base64 64), Stripe test keys
docker compose up -d --build
```

This brings up three containers on one bridge network (`ecommerce-net`):

- `oracle-xe` — Oracle XE 21c, healthchecked before the backend starts
- `backend` — Spring Boot, waits for `oracle-xe`'s healthcheck, runs Flyway
  migrations on startup, healthchecked via `/actuator/health`
- `frontend` — nginx serving the built Angular app on port 80, reverse-
  proxying `/api/` to the `backend` container by its Docker service name
  (never `localhost` — see the comments in `docker-compose.yml` and
  `application-dev.yml` for why that distinction matters once the backend
  itself is containerized)

Visit `http://localhost` for the storefront, `http://localhost:8080/swagger-ui.html`
for the API docs, `http://localhost:8080/actuator/health` for the backend's
own health check.

Tear down with `docker compose down` (add `-v` to also drop the Oracle data
volume and start clean).

## 3. Building images individually

```bash
docker build -t ecommerce-backend ./backend
docker build -t ecommerce-frontend ./frontend
```

Both Dockerfiles are multi-stage (build stage discarded, only the compiled
artifact ships) and run as a non-root user in the runtime stage.

## 4. Staging / Production

These profiles expect an **external** Oracle instance — no bundled Oracle
container for staging/prod; provision Oracle separately (a managed instance,
Oracle Autonomous Database, or your own cluster) and point these env vars at
it:

```bash
SPRING_PROFILES_ACTIVE=staging   # or prod
DB_URL=jdbc:oracle:thin:@//<host>:1521/<service_name>
DB_USERNAME=...
DB_PASSWORD=...
JWT_SECRET=...                   # openssl rand -base64 64 — different from dev's
STRIPE_SECRET_KEY=sk_live_...    # production keys, not sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STAGING_FRONTEND_URL=https://staging.example.com   # staging profile only
PROD_FRONTEND_URL=https://example.com              # prod profile only
```

Differences the `prod` profile enforces, per `application-prod.yml`
(Phase 2) — worth knowing before you flip to it:

- `flyway.baseline-on-migrate: false` — production schema must already be
  deliberately baselined; Flyway won't silently assume an empty/unmanaged
  schema is safe to adopt.
- `server.error.include-message: never` / `include-binding-errors: never` —
  no exception detail leaks into API responses (`GlobalExceptionHandler`'s
  generic 500 message is what clients see either way, but this is a second,
  framework-level backstop).
- File logging to `/var/log/ecommerce-backend/application.log` — mount a
  volume there in whatever orchestrator you deploy with, or point it at
  stdout instead if your platform expects container logs on stdout (a common
  adjustment — change the `logging.file.name` line if so).

Neither a Kubernetes manifest nor a specific cloud provider's deployment
config (ECS task definition, Cloud Run service, etc.) is included — that
choice depends on where you're actually deploying, which this repo doesn't
assume. The Docker images built above are the portable unit; wire them into
whatever orchestrator applies.

## 5. Database migrations in production

Flyway runs automatically on backend startup (`spring.flyway.enabled: true`
in the base `application.yml`, inherited by every profile). For a
zero-downtime deploy with schema changes, follow standard Flyway practice:
prefer additive, backward-compatible migrations (new nullable columns, new
tables) that the *previous* backend version can still run against, then
migrate destructive changes (column drops, renames) in a later deploy once
the new version is fully rolled out.

## 6. Health checks

- Backend: `GET /actuator/health` — `{"status":"UP"}` when the app and its
  DB connection are healthy (`management.endpoints.web.exposure.include:
  health,info` from `application.yml`).
- Frontend (nginx): a plain `GET /` returning 200 is sufficient — the Docker
  `HEALTHCHECK` in `frontend/Dockerfile` uses this.

## 7. CI pipeline

`.github/workflows/ci.yml` runs on every push/PR to `main`/`develop`:

1. **backend-test** — `mvn clean verify` (runs both unit tests via Surefire
   and the `*IT.java` integration tests via Failsafe — see the Phase 9/10
   note below about why Failsafe specifically had to be added).
2. **frontend-build** — `npm ci && ng build --configuration production`.
   Frontend automated tests are not run here yet — Jasmine/Karma isn't wired
   into `package.json` (see Phase 9's known limitations).
3. **docker-build** — builds both Dockerfiles (no push), gated on the first
   two jobs passing, to catch a broken Dockerfile before it reaches a
   deploy step.

No deploy step is wired in — pushing built images to a registry and
triggering an actual deployment depends on where you host this, which is
your call, not something to hardcode here.
