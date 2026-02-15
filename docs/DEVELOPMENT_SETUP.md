# Backend Development Setup

This document is intended for someone coming back to the project after a long break.

## 1. Prerequisites

- Java 21
- PostgreSQL (local or cloud)
- Git
- Optional: IntelliJ IDEA

## 2. Clone and Prepare Environment

1. Create your local environment file:

```bash
cp .env.example .env.dev
```

2. Edit `.env.dev` with your real values.

3. Important:
- `.env.dev` is local-only and ignored by git.
- Never commit credentials.
- Keep `.env.example` updated whenever a new runtime variable is introduced.

## 3. Start the API

### Option A: IntelliJ

- Use shared run configuration: `Backend - Dev`
- It loads `$PROJECT_DIR$/.env.dev`

### Option B: CLI

```bash
set -a
source .env.dev
set +a
./mvnw spring-boot:run
```

## 4. First Startup Checks

On startup, verify:

- Environment validation passed
- Flyway migrations executed successfully
- Application started on the expected port (`8080` by default)

If startup fails, check:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET` length (minimum 32 characters)
- Cookie settings if you changed SameSite/Secure behavior

For detailed error patterns and concrete fixes, see `docs/TROUBLESHOOTING.md`.

## 5. Profile Notes

- `dev`: local development profile
- `qa`: QA infrastructure profile
- `prod`: production profile

The backend no longer has a `mock` profile. Mock mode exists only in the frontend.

## 6. Migrations

Migration folders:

- `src/main/resources/db/migration/common`
- `src/main/resources/db/migration/dev`
- `src/main/resources/db/migration/qa`
- `src/main/resources/db/migration/prod`

Profile mapping:

- `dev`: `common + dev`
- `qa`: `common + qa`
- `prod`: `common + prod`

## 7. Project Map (Where to Look)

- Auth API and flow:
  - `src/main/java/com/fixpoint/auth`
- Business modules (tickets, clients, inventory, attachments):
  - `src/main/java/com/fixpoint/business`
- Security and platform configuration:
  - `src/main/java/com/fixpoint/config`
- Runtime properties and migrations:
  - `src/main/resources/application*.properties`
  - `src/main/resources/db/migration/**`

## 8. Run Tests

```bash
./mvnw test
```

## 9. Local Network Access (LAN)

If frontend and backend run on different LAN hosts:

- Set backend `CORS_ALLOWED_ORIGINS` to include frontend origin (for example `http://192.168.1.50:4200`)
- Keep cookie settings coherent for HTTP local network:
  - `AUTH_REFRESH_COOKIE_SAME_SITE=Lax`
  - `AUTH_REFRESH_COOKIE_SECURE=false`

## 10. Related Docs

- `docs/ENVIRONMENT_VARIABLES.md` for complete variable reference
- `docs/AUTHENTICATION.md` for JWT/refresh flow details
- `docs/USER_PROVISIONING.md` for dev/prod user creation strategy
- `docs/TROUBLESHOOTING.md` for startup and integration failures
