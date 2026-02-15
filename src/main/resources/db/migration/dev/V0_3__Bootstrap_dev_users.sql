-- Development-only bootstrap user.
-- Plain password for this hash: admin123456
-- Never reuse this credential outside local/dev environments.

SET search_path TO public;

INSERT INTO app_users (username, password_hash, role, enabled, created_at)
VALUES (
    'admin',
    '$2a$10$LZgvDbioLsZGXCmq.AH8TubH37f.TruxzaXZtuJR/YX8AdocWr1F.',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;
