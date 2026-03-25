# Backend Project Map

Quick code navigation map for `Fixpoint-Backend`.

## Entry points and config

- `src/main/java/com/fixpoint/FixpointApplication.java`
- `src/main/resources/application.properties`
- `src/main/resources/application-dev.properties`
- `src/main/resources/application-qa.properties`
- `src/main/resources/application-prod.properties`

## Security and auth

- `src/main/java/com/fixpoint/config/SecurityConfig.java`
- `src/main/java/com/fixpoint/config/CorsConfig.java`
- `src/main/java/com/fixpoint/config/EnvironmentVariablesValidator.java`
- `src/main/java/com/fixpoint/auth/**`

## Business domains

- `src/main/java/com/fixpoint/business/dashboard/**`
- `src/main/java/com/fixpoint/business/attachments/**`
- `src/main/java/com/fixpoint/business/clients/**`
- `src/main/java/com/fixpoint/business/inventory/**`
- `src/main/java/com/fixpoint/business/tickets/**`
- `src/main/java/com/fixpoint/business/ticketlogs/**`
- `src/main/java/com/fixpoint/business/ticketparts/**`

## Persistence and migrations

- `src/main/resources/db/migration/common`
- `src/main/resources/db/migration/dev`
- `src/main/resources/db/migration/qa`
- `src/main/resources/db/migration/prod`

## Tests

- `src/test/java/com/fixpoint/**`
