SET search_path TO public;

CREATE TABLE IF NOT EXISTS refresh_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    remember_me BOOLEAN NOT NULL DEFAULT FALSE,
    issued_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked_at TIMESTAMP NULL,
    CONSTRAINT fk_refresh_sessions_user
        FOREIGN KEY (user_id)
        REFERENCES app_users (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_sessions_user_id ON refresh_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_sessions_expires_at ON refresh_sessions(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_sessions_revoked_at ON refresh_sessions(revoked_at);
