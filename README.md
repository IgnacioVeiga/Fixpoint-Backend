# Fixpoint Backend

Spring Boot API for workshop operations (tickets, clients, inventory, attachments) with JWT access tokens and
cookie-based refresh sessions.

## Scope

- Profiles: `dev`, `qa`, `prod`
- Database: PostgreSQL + Flyway
- Authentication:
  - short-lived access token (`Bearer`)
  - HttpOnly refresh cookie with rotation + server-side revocation
- Dashboard summary endpoint for operational metrics and recent files
- Attachment metadata with inferred file category/format and optional tag

## Quick Start

1. Copy `.env.example` to `.env.dev`
2. Fill values for your local/cloud DB and JWT secret
3. Run `Backend - Dev` (IntelliJ) or CLI
4. Confirm logs show:
  - Flyway migration success
  - `Environment validation passed`

Helper scripts:

```bash
./scripts/run.sh dev local
./scripts/run.sh dev docker
./scripts/run.sh dev auto
```

```powershell
.\scripts\run.ps1 dev local
.\scripts\run.ps1 dev docker
.\scripts\run.ps1 dev auto
```

## Start Here (Docs)

- `docs/DEVELOPMENT_SETUP.md` - setup, startup checks, profile behavior
- `docs/ENVIRONMENTS.md` - profile matrix and env conventions
- `docs/AUTH_FLOW.md` - standardized auth/session flow summary
- `docs/MIGRATIONS.md` - Flyway layout and migration rules
- `docs/PROJECT_MAP.md` - source code navigation map
- `docs/ENVIRONMENT_VARIABLES.md` - canonical env var reference by context
- `docs/AUTHENTICATION.md` - login/refresh/logout design and token lifecycle
- `docs/USER_PROVISIONING.md` - how to create users in dev and prod safely
- `docs/TROUBLESHOOTING.md` - common startup/auth/CORS issues and fixes
- `BRANCH_PROTECTION_CHECKLIST.md` - repository governance checklist

## Environment File Policy

- Only `.env.example` is tracked in git
- Real files are local-only and ignored:
  - `.env.dev`
  - `.env.qa`
  - `.env.prod`
- Never commit real credentials or secrets

## IntelliJ Run Configurations

Shared run configs live in `.run/`:

- `Backend - Dev`
- `Backend - QA`
- `Backend - Prod`

Each config reads `.env.<profile>` from project root.

## Useful Commands

```bash
./mvnw test
./mvnw -DskipTests package
```

## CI

- Workflow: `.github/workflows/ci.yml`
- Trigger policy: runs on commits to `main`
