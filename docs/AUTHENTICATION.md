# Backend Authentication Flow

## Goals

- Keep access tokens short-lived
- Avoid long-lived tokens in browser storage
- Support optional "remember session"
- Invalidate session server-side on logout

## Current Design

- Access token:
  - JWT
  - Returned in response body
  - Sent by frontend in `Authorization: Bearer <token>`
- Refresh token:
  - Opaque random token
  - Stored server-side as SHA-256 hash
  - Sent to browser as HttpOnly cookie
  - Rotated on each refresh
- HTTP session:
  - Not used (`STATELESS` security policy)
  - Session authority is the refresh session row in DB

## Endpoints

### `POST /api/auth/login`

- Input:
  - `username`
  - `password`
  - `rememberMe` (optional boolean)
- Output:
  - Access token payload (`tokenType`, `accessToken`, `expiresAt`, `username`, `role`)
  - `Set-Cookie` for refresh token
- Typical status:
  - `200` success
  - `401` invalid credentials

### `POST /api/auth/refresh`

- Input:
  - Refresh cookie only
- Output:
  - New access token payload
  - Rotated refresh cookie (`Set-Cookie`)

If cookie is invalid/expired/revoked:

- Returns `401`
- Sends clear-cookie header

### `POST /api/auth/logout`

- Input:
  - Refresh cookie only
- Effect:
  - Revokes current refresh session in DB
  - Returns clear-cookie header
- Typical status:
  - `204` (idempotent logout behavior)

### `POST /api/auth/register` (dev only)

- Enabled only in `dev`
- Creates user with role `TECH`
- Returns access token and refresh cookie like login
- Not available in `qa` or `prod`

## Remember Session Behavior

- `rememberMe=false`:
  - Uses `AUTH_REFRESH_EXPIRATION_SECONDS`
- `rememberMe=true`:
  - Uses `AUTH_REFRESH_REMEMBER_EXPIRATION_SECONDS`

## Frontend Reload Behavior

- Access token is usually kept in frontend memory, so browser reload may clear it.
- Frontend can recover auth by calling `/api/auth/refresh` if refresh cookie is still valid.
- If refresh fails, user is considered logged out.

## Session Storage Model

Table: `refresh_sessions`

- `token_hash`
- `user_id`
- `remember_me`
- `issued_at`
- `last_used_at`
- `expires_at`
- `revoked_at`

The raw refresh token is never stored in plaintext.

## Security Notes

- This model protects against replay after logout for refresh flow.
- A stolen access token can still be used until it expires, which is why access lifetime is short.
- Use HTTPS in QA/Prod.
- For cross-site cookie scenarios, configure SameSite/Secure properly.
