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
  2001, 1001, 1101, 'admin', 'Platform Administrator', '$2a$10$Ak9ZwaN41jdqAdEXZrHj/uSxgXcgFKslD71EunlAecIBvtggwglwe', 'admin@iris.local', '13800000000', 1, 'Bootstrap admin user', 0, 0, 2001, 2001
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

INSERT INTO sys_user (
  id, tenant_id, org_id, account, username, password_hash, email, mobile, status, remark, deleted, version, created_by, updated_by
) VALUES
  (2002, 1001, 1101, 'finance_mgr', 'Finance Manager', '$2a$10$Ak9ZwaN41jdqAdEXZrHj/uSxgXcgFKslD71EunlAecIBvtggwglwe', 'finance@iris.local', '13800000001', 1, 'Finance scope manager', 0, 0, 2001, 2001),
  (2003, 1001, 1101, 'it_mgr', 'IT Manager', '$2a$10$Ak9ZwaN41jdqAdEXZrHj/uSxgXcgFKslD71EunlAecIBvtggwglwe', 'it@iris.local', '13800000002', 1, 'IT scope manager', 0, 0, 2001, 2001),
  (2004, 1001, 1101, 'auditor_1', 'Senior Auditor', '$2a$10$Ak9ZwaN41jdqAdEXZrHj/uSxgXcgFKslD71EunlAecIBvtggwglwe', 'auditor@iris.local', '13800000003', 1, 'Read-only auditor user', 0, 0, 2001, 2001)
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
  (3001, 1001, 'PLATFORM_ADMIN', '超级管理员', 'GLOBAL', 1, '全局级超级管理员角色', 0, 0, 2001, 2001),
  (3002, 1001, 'TENANT_ADMIN', '管理员', 'BUSINESS', 1, '业务级管理员角色', 0, 0, 2001, 2001),
  (3003, 1001, 'AUDITOR', '审计员', 'BUSINESS', 1, '业务级只读审计角色', 0, 0, 2001, 2001)
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
  (5002, 1001, 2001, 3002, 'Bootstrap tenant admin role binding', 0, 0, 2001, 2001),
  (5003, 1001, 2002, 3002, 'Finance admin role binding', 0, 0, 2001, 2001),
  (5004, 1001, 2003, 3002, 'IT admin role binding', 0, 0, 2001, 2001),
  (5005, 1001, 2004, 3003, 'Auditor role binding', 0, 0, 2001, 2001)
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

INSERT INTO sys_role_menu (
  id, tenant_id, role_id, menu_code, remark, deleted, version, created_by, updated_by
) VALUES
  (6101, 1001, 3001, 'workbench.dashboard', 'Platform admin menu', 0, 0, 2001, 2001),
  (6102, 1001, 3001, 'workbench.alerts', 'Platform admin menu', 0, 0, 2001, 2001),
  (6103, 1001, 3001, 'workbench.logs', 'Platform admin menu', 0, 0, 2001, 2001),
  (6104, 1001, 3001, 'resource.standards', 'Platform admin menu', 0, 0, 2001, 2001),
  (6105, 1001, 3001, 'resource.scopes', 'Platform admin menu', 0, 0, 2001, 2001),
  (6106, 1001, 3001, 'resource.checklists', 'Platform admin menu', 0, 0, 2001, 2001),
  (6107, 1001, 3001, 'resource.archives', 'Platform admin menu', 0, 0, 2001, 2001),
  (6108, 1001, 3001, 'resource.personnel', 'Platform admin menu', 0, 0, 2001, 2001),
  (6109, 1001, 3001, 'plan.create', 'Platform admin menu', 0, 0, 2001, 2001),
  (6110, 1001, 3001, 'plan.list', 'Platform admin menu', 0, 0, 2001, 2001),
  (6111, 1001, 3001, 'plan.overview', 'Platform admin menu', 0, 0, 2001, 2001),
  (6112, 1001, 3001, 'project.list', 'Platform admin menu', 0, 0, 2001, 2001),
  (6113, 1001, 3001, 'project.create', 'Platform admin menu', 0, 0, 2001, 2001),
  (6114, 1001, 3001, 'rectification.list', 'Platform admin menu', 0, 0, 2001, 2001),
  (6115, 1001, 3001, 'rectification.create', 'Platform admin menu', 0, 0, 2001, 2001),
  (6116, 1001, 3001, 'smart.analysis', 'Platform admin menu', 0, 0, 2001, 2001),
  (6117, 1001, 3001, 'smart.rules', 'Platform admin menu', 0, 0, 2001, 2001),
  (6118, 1001, 3001, 'smart.models', 'Platform admin menu', 0, 0, 2001, 2001),
  (6119, 1001, 3001, 'smart.tools', 'Platform admin menu', 0, 0, 2001, 2001),
  (6120, 1001, 3001, 'system.roles', 'Platform admin menu', 0, 0, 2001, 2001),
  (6121, 1001, 3002, 'workbench.dashboard', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6122, 1001, 3002, 'resource.standards', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6123, 1001, 3002, 'resource.scopes', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6124, 1001, 3002, 'resource.checklists', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6125, 1001, 3002, 'resource.archives', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6126, 1001, 3002, 'resource.personnel', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6127, 1001, 3002, 'plan.create', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6128, 1001, 3002, 'plan.list', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6129, 1001, 3002, 'plan.overview', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6130, 1001, 3002, 'project.list', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6131, 1001, 3002, 'project.create', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6132, 1001, 3002, 'rectification.list', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6133, 1001, 3002, 'rectification.create', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6134, 1001, 3002, 'smart.analysis', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6135, 1001, 3002, 'smart.rules', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6136, 1001, 3002, 'system.roles', 'Tenant admin menu', 0, 0, 2001, 2001),
  (6137, 1001, 3003, 'workbench.dashboard', 'Auditor menu', 0, 0, 2001, 2001),
  (6138, 1001, 3003, 'resource.standards', 'Auditor menu', 0, 0, 2001, 2001),
  (6139, 1001, 3003, 'resource.checklists', 'Auditor menu', 0, 0, 2001, 2001),
  (6140, 1001, 3003, 'resource.archives', 'Auditor menu', 0, 0, 2001, 2001),
  (6141, 1001, 3003, 'plan.list', 'Auditor menu', 0, 0, 2001, 2001),
  (6142, 1001, 3003, 'plan.overview', 'Auditor menu', 0, 0, 2001, 2001),
  (6143, 1001, 3003, 'project.list', 'Auditor menu', 0, 0, 2001, 2001),
  (6144, 1001, 3003, 'rectification.list', 'Auditor menu', 0, 0, 2001, 2001),
  (6145, 1001, 3003, 'smart.analysis', 'Auditor menu', 0, 0, 2001, 2001)
ON DUPLICATE KEY UPDATE
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_resource_scope (
  id, tenant_id, scope_code, scope_name, scope_type, status, remark, deleted, version, created_by, updated_by
) VALUES
  (9001, 1001, 'FINANCE', 'Finance Scope', 'RESOURCE', 1, 'Finance-owned standards and checklists', 0, 0, 2001, 2001),
  (9002, 1001, 'IT', 'IT Scope', 'RESOURCE', 1, 'IT-owned standards and checklists', 0, 0, 2001, 2001),
  (9003, 1001, 'COMPLIANCE', 'Compliance Scope', 'RESOURCE', 1, 'Compliance-owned shared resources', 0, 0, 2001, 2001)
ON DUPLICATE KEY UPDATE
  scope_name = VALUES(scope_name),
  scope_type = VALUES(scope_type),
  status = VALUES(status),
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_resource_scope_member (
  id, tenant_id, scope_id, user_id, can_view, can_create, can_edit, can_delete, can_manage, remark, deleted, version, created_by, updated_by
) VALUES
  (9101, 1001, 9001, 2001, 1, 1, 1, 1, 1, 'Bootstrap admin membership', 0, 0, 2001, 2001),
  (9102, 1001, 9002, 2001, 1, 1, 1, 1, 1, 'Bootstrap admin membership', 0, 0, 2001, 2001),
  (9103, 1001, 9003, 2001, 1, 1, 1, 1, 1, 'Bootstrap admin membership', 0, 0, 2001, 2001),
  (9104, 1001, 9001, 2002, 1, 1, 1, 0, 1, 'Finance manager membership', 0, 0, 2001, 2001),
  (9105, 1001, 9002, 2003, 1, 1, 1, 0, 1, 'IT manager membership', 0, 0, 2001, 2001),
  (9106, 1001, 9003, 2004, 1, 0, 0, 0, 0, 'Auditor membership', 0, 0, 2001, 2001)
ON DUPLICATE KEY UPDATE
  can_view = VALUES(can_view),
  can_create = VALUES(can_create),
  can_edit = VALUES(can_edit),
  can_delete = VALUES(can_delete),
  can_manage = VALUES(can_manage),
  remark = VALUES(remark),
  updated_by = VALUES(updated_by),
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO biz_standard (
  id, tenant_id, standard_group_id, standard_code, title, category, standard_version, version_number, previous_version_id,
  publish_date, status, description, visibility_level, owner_scope_id, shared_scope_ids, change_log, remark,
  deleted, version, created_by, updated_by
) VALUES
  (9901, 1001, 'std-001', 'STD-FIN-001', 'Finance Standard Baseline', 'internal', 'V1.0', 1, NULL,
   '2026-04-23', 'active', 'Finance-owned internal control baseline', 'PUBLIC', 9001, '9002', 'Initial release',
   'Finance baseline seed data', 0, 0, 2001, 2001),
  (9902, 1001, 'std-002', 'STD-IT-002', 'IT Security Checklist Standard', 'system', 'V1.0', 1, NULL,
   '2026-04-23', 'active', 'IT-owned security standard', 'SCOPED', 9002, '9003', 'Initial release',
   'IT security seed data', 0, 0, 2001, 2001),
  (9903, 1001, 'std-003', 'STD-COMP-003', 'Compliance Review Procedure', 'industry', 'V1.0', 1, NULL,
   '2026-04-23', 'draft', 'Compliance review draft', 'SCOPED', 9003, NULL, 'Draft created',
   'Compliance draft seed data', 0, 0, 2001, 2001)
ON DUPLICATE KEY UPDATE
  standard_code = VALUES(standard_code),
  title = VALUES(title),
  category = VALUES(category),
  standard_version = VALUES(standard_version),
  version_number = VALUES(version_number),
  previous_version_id = VALUES(previous_version_id),
  publish_date = VALUES(publish_date),
  status = VALUES(status),
  description = VALUES(description),
  visibility_level = VALUES(visibility_level),
  owner_scope_id = VALUES(owner_scope_id),
  shared_scope_ids = VALUES(shared_scope_ids),
  change_log = VALUES(change_log),
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
