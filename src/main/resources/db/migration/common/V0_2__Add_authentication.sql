-- Authentication users for API access.
-- User rows should be provisioned through:
-- - DEV: POST /api/auth/register
-- - QA/PROD: dedicated Flyway SQL scripts with BCrypt hashes.

SET search_path TO public;

CREATE TABLE IF NOT EXISTS app_users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'TECH')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
