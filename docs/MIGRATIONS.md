# Backend Migrations

Flyway strategy for `Fixpoint-Backend`.

## Folder layout

- `src/main/resources/db/migration/common`
- `src/main/resources/db/migration/dev`
- `src/main/resources/db/migration/qa`
- `src/main/resources/db/migration/prod`

## Profile mapping

- `dev` = `common + dev`
- `qa` = `common + qa`
- `prod` = `common + prod`

## Rules

1. Keep schema evolution in Flyway only.
2. Do not edit already applied versioned migrations in shared environments.
3. Keep production bootstrap deterministic and placeholder-driven.
4. Keep secrets out of migration SQL files.

## Current baseline

- `common/V1__init_database.sql`
- `common/V2__add_authentication.sql`
- `common/V3__add_refresh_sessions.sql`
- `dev/V101__bootstrap_dev_users.sql`
- `prod/V301__bootstrap_prod_users.sql`

## Related docs

- `docs/DEVELOPMENT_SETUP.md`
- `docs/ENVIRONMENT_VARIABLES.md`
- `docs/USER_PROVISIONING.md`