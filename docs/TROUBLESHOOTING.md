# Backend Troubleshooting

Common issues and practical fixes for startup and auth integration.

## 1. Application Fails During Startup

### Symptom

- Spring context fails with bean creation errors around JPA/Auth.

### Frequent Root Cause

- Flyway migration failed earlier, so JPA `entityManagerFactory` cannot initialize.

### What to Check

- Look for the first `Caused by` in logs.
- Validate migrations executed before JPA startup.

## 2. Flyway Placeholder Error

### Symptom

- `No value provided for placeholder: ${bootstrap_admin_username}`

### Cause

- Prod bootstrap migration was loaded but required placeholder value is missing.

### Fix

- Confirm active profile (`dev`, `qa`, `prod`).
- Ensure Flyway location per profile is correct.
- If profile is `prod`, provide:
  - `AUTH_BOOTSTRAP_ADMIN_USERNAME`
  - `AUTH_BOOTSTRAP_ADMIN_PASSWORD_HASH`
  - `AUTH_BOOTSTRAP_ADMIN_ROLE`

## 3. DB Connection Errors

### Symptom

- Connection timeout/authentication failure during Flyway or Hikari startup.

### Fix

- Re-check:
  - `DB_URL`
  - `DB_USERNAME`
  - `DB_PASSWORD`
- For managed/cloud PostgreSQL, add `sslmode=require` when required by provider.

## 4. Environment Validator Fails

### Symptom

- Clear validation messages in startup logs.

### Typical Fixes

- `JWT_SECRET` shorter than 32 chars -> provide longer secret.
- `AUTH_REFRESH_REMEMBER_EXPIRATION_SECONDS` lower than `AUTH_REFRESH_EXPIRATION_SECONDS` -> raise remember value.
- `SameSite=None` with `secure=false` -> set `AUTH_REFRESH_COOKIE_SECURE=true`.

## 5. Login Returns "Invalid username or password"

### Checks

- Username is normalized to lowercase by backend.
- User exists and `enabled=true` in `app_users`.
- Password matches BCrypt hash in `password_hash`.

## 6. Refresh Fails (`401`) After Login

### Checks

- Frontend must send credentials (`withCredentials=true`).
- CORS origin must be listed in `CORS_ALLOWED_ORIGINS`.
- Cookie settings must match deployment:
  - local HTTP: usually `Lax` + `secure=false`
  - cross-site HTTPS: `None` + `secure=true`

## 7. Logout Called but Session Still Appears Active

### Expected Behavior

- Logout revokes refresh session and clears cookie.
- Existing access token may work until its short expiration time.

### Mitigation

- Keep access token TTL short (`JWT_ACCESS_EXPIRATION_SECONDS`).
- Rely on refresh revocation to block session continuation.
