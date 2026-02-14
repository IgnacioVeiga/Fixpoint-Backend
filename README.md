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
