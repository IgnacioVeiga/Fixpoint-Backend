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
- `dev/V101__bootstrap_dev_users.sql`
- `prod/V301__bootstrap_prod_users.sql`

## Baseline notes

- `V1` already creates the current attachment schema:
  - `fileType`: technical file category such as `image`, `document`, `spreadsheet`, `archive`
  - `fileFormat`: explicit stored extension such as `pdf`, `png`, `xlsx`
  - `fileSizeBytes`: stored file size used by dashboard storage metrics
  - `tag`: optional business label such as `Presupuesto`, `Escaneo`, `Factura`
- `V1` also includes authentication tables (`app_users`, `refresh_sessions`) so fresh environments start from the current model directly.

## Related docs

- `docs/DEVELOPMENT_SETUP.md`
- `docs/ENVIRONMENT_VARIABLES.md`
- `docs/USER_PROVISIONING.md`
