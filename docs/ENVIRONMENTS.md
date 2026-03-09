# Backend Environments

Quick reference for runtime environment handling in `Fixpoint-Backend`.

## Supported profiles

- `dev`
- `qa`
- `prod`

## Canonical variables reference

- `docs/ENVIRONMENT_VARIABLES.md`

## Local env file convention

- `.env.dev`
- `.env.qa`
- `.env.prod`

All files are created from `.env.example`.

## Shared rules

- `SPRING_PROFILES_ACTIVE` selects active runtime profile.
- Keep secrets out of git (`.env.example` only is tracked).
- If runtime variables change, update:
  - `application*.properties`
  - `.env.example`
  - `docs/ENVIRONMENT_VARIABLES.md`
  - `EnvironmentVariablesValidator`

## Related docs

- `docs/DEVELOPMENT_SETUP.md`
- `docs/ENVIRONMENT_VARIABLES.md`
- `docs/TROUBLESHOOTING.md`