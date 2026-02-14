-- Production-only user bootstrap migration.
-- Configure placeholders via:
-- AUTH_BOOTSTRAP_ADMIN_USERNAME
-- AUTH_BOOTSTRAP_ADMIN_PASSWORD_HASH
-- AUTH_BOOTSTRAP_ADMIN_ROLE (ADMIN or TECH)

INSERT INTO app_users (username, password_hash, role, enabled, created_at)
SELECT
    '${bootstrap_admin_username}',
    '${bootstrap_admin_password_hash}',
    '${bootstrap_admin_role}',
    true,
    CURRENT_TIMESTAMP
WHERE '${bootstrap_admin_username}' <> ''
  AND '${bootstrap_admin_password_hash}' <> ''
  AND '${bootstrap_admin_role}' IN ('ADMIN', 'TECH')
  AND NOT EXISTS (
      SELECT 1
      FROM app_users
      WHERE username = '${bootstrap_admin_username}'
  );
