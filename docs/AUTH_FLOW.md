# Backend Auth Flow

Summary of authentication/session behavior in `Fixpoint-Backend`.

## Current model

- Login uses `username + password`.
- Short-lived JWT access token in response body.
- Refresh token is opaque, stored as HttpOnly cookie.
- Refresh sessions are persisted in DB with hashed token.
- Refresh token is rotated on each refresh.
- Logout revokes active refresh session.

## Auth endpoints

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/register` (dev-only)

## Frontend integration requirements

- Use `withCredentials: true`.
- Do not persist auth tokens in localStorage/sessionStorage.

## Detailed reference

- `docs/AUTHENTICATION.md`
- `src/main/java/com/fixpoint/auth/service/AuthService.java`
- `src/main/java/com/fixpoint/auth/security/RefreshTokenCookieService.java`
- `src/main/java/com/fixpoint/config/SecurityConfig.java`