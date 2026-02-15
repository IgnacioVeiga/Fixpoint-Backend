# Backend Environment Variables

Canonical runtime variable reference for `Fixpoint-Backend`.

## Required by Profile

- Required in all profiles (`dev`, `qa`, `prod`):
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
  - `JWT_SECRET`
- Required only when profile is `prod` and you want bootstrap user creation by migration:
  - `AUTH_BOOTSTRAP_ADMIN_USERNAME`
  - `AUTH_BOOTSTRAP_ADMIN_PASSWORD_HASH`
  - `AUTH_BOOTSTRAP_ADMIN_ROLE`
- Optional (project defaults exist in `application*.properties`):
  - all remaining variables listed below

## Grouped by Context

### Profile Selection

- `SPRING_PROFILES_ACTIVE`
  - Typical values: `dev`, `qa`, `prod`
  - Local recommendation: `dev`

### Database

- `DB_URL`
  - Local example: `jdbc:postgresql://localhost:5432/fixpoint`
  - Cloud example: `jdbc:postgresql://host:5432/db?sslmode=require`
- `DB_USERNAME`
- `DB_PASSWORD`

### CORS

- `CORS_ALLOWED_ORIGINS`
  - Comma-separated list of allowed frontend origins
  - Example: `http://localhost:4200,http://192.168.1.50:4200`

### File Storage and Time Zone

- `FILE_UPLOAD_DIR`
  - Relative or absolute path for uploaded files
- `APP_TIMEZONE`
  - Server baseline timezone
  - Recommendation: `America/Argentina/Buenos_Aires`

### JWT Access Token

- `JWT_SECRET`
  - Minimum length: 32 characters
  - Generate example:
    - `openssl rand -base64 48`
- `JWT_ACCESS_EXPIRATION_SECONDS`
  - Access token lifetime (short-lived)
  - Recommended range: `300` to `1800`
  - Project default: `900`

### Refresh Session and Cookie

- `AUTH_REFRESH_EXPIRATION_SECONDS`
  - Lifetime for non-remember session
- `AUTH_REFRESH_REMEMBER_EXPIRATION_SECONDS`
  - Lifetime for remember-me session
  - Must be greater than or equal to `AUTH_REFRESH_EXPIRATION_SECONDS`
- `AUTH_REFRESH_COOKIE_NAME`
  - Default: `fixpoint_refresh_token`
- `AUTH_REFRESH_COOKIE_PATH`
  - Default: `/api/auth`
- `AUTH_REFRESH_COOKIE_SAME_SITE`
  - Allowed values: `Lax`, `Strict`, `None`
- `AUTH_REFRESH_COOKIE_SECURE`
  - `true` required for `SameSite=None` in production browsers
- `AUTH_REFRESH_COOKIE_DOMAIN`
  - Optional
  - Leave empty unless cross-subdomain cookie sharing is required

### Production Bootstrap User (Flyway, prod only)

- `AUTH_BOOTSTRAP_ADMIN_USERNAME`
- `AUTH_BOOTSTRAP_ADMIN_PASSWORD_HASH`
  - BCrypt hash only (never plain password)
- `AUTH_BOOTSTRAP_ADMIN_ROLE`
  - Allowed values: `ADMIN`, `TECH`

## Production Notes

- Never commit real values.
- Prefer secret manager or deployment-level env injection.
- Keep `AUTH_BOOTSTRAP_ADMIN_*` empty unless you explicitly want to seed one bootstrap user.
- If frontend and backend are different sites and cookie refresh must work cross-site:
  - `AUTH_REFRESH_COOKIE_SAME_SITE=None`
  - `AUTH_REFRESH_COOKIE_SECURE=true`
  - HTTPS is mandatory

## Minimal `.env.dev` Example

```dotenv
SPRING_PROFILES_ACTIVE=dev

DB_URL=jdbc:postgresql://localhost:5432/fixpoint
DB_USERNAME=postgres
DB_PASSWORD=adminpostgresql

JWT_SECRET=replace-with-32-plus-character-secret

CORS_ALLOWED_ORIGINS=http://localhost:4200
```

## Minimal `.env.prod` Example

```dotenv
SPRING_PROFILES_ACTIVE=prod

DB_URL=jdbc:postgresql://host:5432/db?sslmode=require
DB_USERNAME=prod_user
DB_PASSWORD=prod_password

JWT_SECRET=replace-with-32-plus-character-secret

AUTH_REFRESH_COOKIE_SAME_SITE=None
AUTH_REFRESH_COOKIE_SECURE=true

AUTH_BOOTSTRAP_ADMIN_USERNAME=admin
AUTH_BOOTSTRAP_ADMIN_PASSWORD_HASH=$2a$10$replace-with-real-bcrypt-hash
AUTH_BOOTSTRAP_ADMIN_ROLE=ADMIN
```
