# Backend User Provisioning

This document describes supported ways to create application users per environment.

## Summary by Profile

- `dev`:
  - preferred: `POST /api/v1/auth/register` (dev-only endpoint)
  - optional: dev bootstrap migration (`src/main/resources/db/migration/dev/V101__bootstrap_dev_users.sql`)
- `qa`:
  - manual SQL or QA-only migration when needed
- `prod`:
  - bootstrap via Flyway placeholders and env vars (`AUTH_BOOTSTRAP_ADMIN_*`)
  - no public signup endpoint

## Dev: Register Endpoint (Recommended for Daily Work)

Endpoint:

- `POST /api/v1/auth/register`

Behavior:

- available only when `SPRING_PROFILES_ACTIVE=dev`
- creates a new enabled user with role `TECH`
- normalizes username to lowercase

## Dev: Bootstrap Admin User

File:

- `src/main/resources/db/migration/dev/V101__bootstrap_dev_users.sql`

Current seed user:

- username: `admin`
- password: `admin123456`

This migration is development-only and should never be reused in production.

## Prod: Bootstrap User via Environment Variables

File:

- `src/main/resources/db/migration/prod/V301__bootstrap_prod_users.sql`

Required env vars:

- `AUTH_BOOTSTRAP_ADMIN_USERNAME`
- `AUTH_BOOTSTRAP_ADMIN_PASSWORD_HASH`
- `AUTH_BOOTSTRAP_ADMIN_ROLE` (`ADMIN` or `TECH`)

The migration inserts only when:

- username is not empty
- password hash is not empty
- role is valid
- username does not already exist

## How to Obtain a BCrypt Hash

Recommended workflow:

1. In `dev`, call `POST /api/v1/auth/register` with the target password.
2. Read the generated `password_hash` from table `app_users`.
3. Reuse that hash value for `AUTH_BOOTSTRAP_ADMIN_PASSWORD_HASH` in prod deployment.

Notes:

- BCrypt hashes are salted, so the same plain password can produce different hashes.
- Different hashes of the same password are still valid for verification.
- Store only the hash, never the plain password.

## Operational Recommendation

- Keep registration endpoint enabled only in `dev`.
- In `qa` and `prod`, use controlled provisioning (migration, SQL script, or admin process).
