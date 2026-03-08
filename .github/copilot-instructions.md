# LLM Coding Instructions - Fixpoint Backend

This file is machine-oriented guidance for code generation and edits in `Fixpoint-Backend`.
Human onboarding and operational documentation are in `docs/`.

## 1. Repository Identity

- Stack: Spring Boot 3.5.x, Java 21, PostgreSQL, Flyway.
- API style: REST under `/api/v1/**`.
- Profiles: `dev`, `qa`, `prod`.
- CI trigger: runs on commits to `main` (`.github/workflows/ci.yml`).

## 2. Non-Negotiable Constraints

- Do not introduce a backend `mock` profile.
- Keep schema evolution in Flyway migrations only.
  - Do not rely on Hibernate DDL auto-generation.
- Keep sensitive values out of repository.
  - Only `.env.example` is tracked.
  - `.env.dev`, `.env.qa`, `.env.prod` stay local.
- Keep auth model intact unless explicitly redesigning security:
  - short-lived JWT access token
  - HttpOnly refresh cookie
  - refresh sessions persisted server-side with hashed token
- Keep global API error payload shape:
  - `timestamp`, `status`, `error`, `message`

## 3. Architecture and Package Layout

- Domain package strategy:
  - `com.fixpoint.business.<domain>.{controller,dto,entity,repository,service}`
- Auth/security packages:
  - `com.fixpoint.auth.*`
  - `com.fixpoint.config.*`
- Expose DTOs from controllers; do not return JPA entities directly.
- Validate request DTOs with `jakarta.validation`.

## 4. Data and Migration Rules

- Migration locations:
  - `db/migration/common`
  - `db/migration/dev`
  - `db/migration/qa`
  - `db/migration/prod`
- Profile mapping:
  - `dev = common + dev`
  - `qa = common + qa`
  - `prod = common + prod`
- Production bootstrap user is placeholder-driven (`AUTH_BOOTSTRAP_ADMIN_*`).
- Development bootstrap user exists only in dev migrations.

## 5. Security and Auth Rules

- Security is stateless (`SessionCreationPolicy.STATELESS`).
- Public auth endpoints:
  - `POST /api/v1/auth/login`
  - `POST /api/v1/auth/refresh`
  - `POST /api/v1/auth/logout`
  - `POST /api/v1/auth/register` (controller active only in `dev` profile)
- Keep cookie and CORS behavior consistent with frontend integration.
- Preserve username normalization (lowercase, trimmed) unless explicitly changed.

## 6. Environment and Config Rules

- Required operational vars include:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`
- Keep `EnvironmentVariablesValidator` aligned when adding/changing env vars.
- If new runtime vars are added:
  - update `application*.properties`
  - update `.env.example`
  - update `docs/ENVIRONMENT_VARIABLES.md`

## 7. Change Policy for LLMs

- Prefer minimal edits and preserve existing behavior.
- Keep business rules explicit in services; avoid hidden side effects.
- Add or update tests for behavioral changes:
  - unit tests for services
  - integration tests when endpoint contracts change
- If API contracts change, ensure frontend compatibility is addressed.

## 8. Test and Verification

- Default checks:
  - `./mvnw test`
  - `./mvnw -DskipTests package`
- For auth and migration changes, verify:
  - application startup
  - Flyway migration execution
  - auth login/refresh/logout flow

## 9. Files LLMs Should Read First

- `README.md`
- `docs/DEVELOPMENT_SETUP.md`
- `docs/ENVIRONMENT_VARIABLES.md`
- `docs/AUTHENTICATION.md`
- `src/main/java/com/fixpoint/config/SecurityConfig.java`
- `src/main/java/com/fixpoint/auth/service/AuthService.java`
- `src/main/java/com/fixpoint/config/EnvironmentVariablesValidator.java`
