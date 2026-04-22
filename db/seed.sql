USE iris_back;

INSERT INTO sys_tenant (
  id, tenant_id, tenant_code, tenant_name, status, remark, deleted, version, created_by, updated_by
) VALUES (
  1001, 1001, 'DEFAULT', 'Default Tenant', 1, 'Bootstrap tenant', 0, 0, 2001, 2001
)
ON DUPLICATE KEY UPDATE
  tenant_name = VALUES(tenant_name),
  status = VALUES(status),
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_org (
  id, tenant_id, parent_id, org_code, org_name, org_level, sort_order, status, remark, deleted, version, created_by, updated_by
) VALUES (
  1101, 1001, NULL, 'ROOT', 'Default Headquarters', 1, 0, 1, 'Root organization', 0, 0, 2001, 2001
)
ON DUPLICATE KEY UPDATE
  org_name = VALUES(org_name),
  status = VALUES(status),
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_user (
  id, tenant_id, org_id, account, username, password_hash, email, mobile, status, remark, deleted, version, created_by, updated_by
) VALUES (
  2001, 1001, 1101, 'admin', 'Platform Administrator', '$2a$10$7EqJtq98hPqEX7fNZaFWoO5a5y9N8MaqkOawx2UY8ailqXF/w3v9e', 'admin@iris.local', '13800000000', 1, 'Bootstrap admin user', 0, 0, 2001, 2001
)
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password_hash = VALUES(password_hash),
  email = VALUES(email),
  mobile = VALUES(mobile),
  status = VALUES(status),
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_role (
  id, tenant_id, role_code, role_name, scope_type, status, remark, deleted, version, created_by, updated_by
) VALUES
  (3001, 1001, 'PLATFORM_ADMIN', 'Platform Administrator', 'PLATFORM', 1, 'Platform-level super role', 0, 0, 2001, 2001),
  (3002, 1001, 'TENANT_ADMIN', 'Tenant Administrator', 'TENANT', 1, 'Tenant-level admin role', 0, 0, 2001, 2001)
ON DUPLICATE KEY UPDATE
  role_name = VALUES(role_name),
  scope_type = VALUES(scope_type),
  status = VALUES(status),
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_permission (
  id, tenant_id, permission_code, permission_name, permission_type, path, method, status, remark, deleted, version, created_by, updated_by
) VALUES
  (4001, 1001, 'system:tenant:list', 'List tenants', 'API', '/api/v1/system/tenants', 'GET', 1, 'Bootstrap permission', 0, 0, 2001, 2001),
  (4002, 1001, 'system:user:list', 'List users', 'API', '/api/v1/system/users', 'GET', 1, 'Bootstrap permission', 0, 0, 2001, 2001),
  (4003, 1001, 'system:role:list', 'List roles', 'API', '/api/v1/system/roles', 'GET', 1, 'Bootstrap permission', 0, 0, 2001, 2001),
  (4004, 1001, 'system:org:list', 'List orgs', 'API', '/api/v1/system/orgs', 'GET', 1, 'Bootstrap permission', 0, 0, 2001, 2001)
ON DUPLICATE KEY UPDATE
  permission_name = VALUES(permission_name),
  permission_type = VALUES(permission_type),
  path = VALUES(path),
  method = VALUES(method),
  status = VALUES(status),
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_user_role (
  id, tenant_id, user_id, role_id, remark, deleted, version, created_by, updated_by
) VALUES
  (5001, 1001, 2001, 3001, 'Bootstrap admin role binding', 0, 0, 2001, 2001),
  (5002, 1001, 2001, 3002, 'Bootstrap tenant admin role binding', 0, 0, 2001, 2001)
ON DUPLICATE KEY UPDATE
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_role_permission (
  id, tenant_id, role_id, permission_id, remark, deleted, version, created_by, updated_by
) VALUES
  (6001, 1001, 3001, 4001, 'Bootstrap binding', 0, 0, 2001, 2001),
  (6002, 1001, 3001, 4002, 'Bootstrap binding', 0, 0, 2001, 2001),
  (6003, 1001, 3001, 4003, 'Bootstrap binding', 0, 0, 2001, 2001),
  (6004, 1001, 3001, 4004, 'Bootstrap binding', 0, 0, 2001, 2001)
ON DUPLICATE KEY UPDATE
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_login_log (
  id, tenant_id, user_id, account, login_result, ip_address, user_agent, remark, deleted, version, created_by, updated_by
) VALUES (
  7001, 1001, 2001, 'admin', 'SUCCESS', '127.0.0.1', 'bootstrap-script', 'Initial bootstrap login log', 0, 0, 2001, 2001
)
ON DUPLICATE KEY UPDATE
  login_result = VALUES(login_result),
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_operation_log (
  id, tenant_id, user_id, module_name, operation_name, request_uri, request_method, operation_result, remark, deleted, version, created_by, updated_by
) VALUES (
  8001, 1001, 2001, 'bootstrap', 'seed-data', '/db/seed.sql', 'SQL', 'SUCCESS', 'Initial bootstrap operation log', 0, 0, 2001, 2001
)
ON DUPLICATE KEY UPDATE
  operation_result = VALUES(operation_result),
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;
