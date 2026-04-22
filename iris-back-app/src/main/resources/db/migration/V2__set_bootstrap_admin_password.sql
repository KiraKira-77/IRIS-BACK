UPDATE sys_user
SET password_hash = '$2a$10$/pmZM1BgC/b/BVIE9jvKxuKO40rU3kGBxKx7O0ncn3kqqLzVPoKzC',
    updated_at = CURRENT_TIMESTAMP
WHERE tenant_id = 1001
  AND account = 'admin'
  AND deleted = 0;
