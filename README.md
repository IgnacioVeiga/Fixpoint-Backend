# Fixpoint Backend

## Perfiles de entorno

- `dev`: desarrollo local con PostgreSQL.
- `qa`: pruebas contra infraestructura de QA.
- `prod`: producción.
- `mock`: ejecución demo sin PostgreSQL (H2 en memoria).

Archivos asociados:

- `src/main/resources/application-dev.properties`
- `src/main/resources/application-qa.properties`
- `src/main/resources/application-prod.properties`
- `src/main/resources/application-mock.properties`

## Variables de entorno

Base común (`application.properties`):

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `FILE_UPLOAD_DIR`
- `APP_TIMEZONE`
- `JWT_SECRET`
- `JWT_EXPIRATION_SECONDS`
- `AUTH_BOOTSTRAP_ADMIN_USERNAME` (prod only)
- `AUTH_BOOTSTRAP_ADMIN_PASSWORD_HASH` (prod only)
- `AUTH_BOOTSTRAP_ADMIN_ROLE` (prod only, `ADMIN`/`TECH`)

Plantillas por entorno para IntelliJ:

- `.env.dev`
- `.env.qa`
- `.env.prod`
- `.env.mock`

## IntelliJ IDEA

Hay run configurations compartidas en `.run/`:

- `Backend - Dev`
- `Backend - QA`
- `Backend - Prod`
- `Backend - Mock`

Cada una levanta `com.fixpoint.FixpointApplication` usando su archivo `.env.*`.

## Authentication flow

- `POST /api/auth/login`: available in all environments.
- `POST /api/auth/register`: available only in `dev` profile.
- Production user provisioning is handled by Flyway SQL (`src/main/resources/db/migration/prod/V0_3__Bootstrap_prod_users.sql`).

## Flyway migration layout

- `src/main/resources/db/migration/common`: shared schema/data migrations for every environment.
- `src/main/resources/db/migration/dev`: dev-only migrations.
- `src/main/resources/db/migration/qa`: QA-only migrations.
- `src/main/resources/db/migration/prod`: prod-only migrations.

Active locations by profile:

- `dev`: `common + dev`
- `qa`: `common + qa`
- `prod`: `common + prod`
- `mock`: Flyway disabled

Bootstrap behavior for empty databases:

- Flyway is configured to create schema `public` automatically (`spring.flyway.create-schemas=true`).
- Base schema/tables are created from `common` migrations without assuming pre-existing schemas.

## Dev Profile with Cloud PostgreSQL

Minimum required environment variables for `dev`:

- `DB_URL` (example: `jdbc:postgresql://db-host:5432/fixpoint?sslmode=require`)
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (minimum 32 chars)

If any of these are missing, startup now fails fast with a clear validation message.

## Comandos

```bash
./mvnw test
./mvnw -DskipTests package
```

## Unit tests included

- `TicketPartServiceTest`: stock validation and DTO mapping.
- `InventoryServiceImplTest`: delete guard when inventory is linked to ticket parts.
- `GlobalExceptionHandlerTest`: HTTP status and response payload mapping.
- `ApiIntegrationTest`: MockMvc integration tests for tickets, inventory, ticket parts, and global error mapping.

## CI

GitHub Actions workflow: `.github/workflows/ci.yml` runs tests and package build only on commits to `main`.

## Branch protection

Use `BRANCH_PROTECTION_CHECKLIST.md` before enabling/adjusting rules for `main`.
