CREATE TABLE IF NOT EXISTS sys_role_menu (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  menu_code VARCHAR(128) NOT NULL,
  remark VARCHAR(500) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_role_menu (tenant_id, role_id, menu_code),
  KEY idx_sys_role_menu_role (tenant_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_resource_scope (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  scope_code VARCHAR(64) NOT NULL,
  scope_name VARCHAR(128) NOT NULL,
  scope_type VARCHAR(32) NOT NULL DEFAULT 'RESOURCE',
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(500) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_resource_scope_code (tenant_id, scope_code)
);

CREATE TABLE IF NOT EXISTS sys_resource_scope_member (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  scope_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  can_view TINYINT NOT NULL DEFAULT 1,
  can_create TINYINT NOT NULL DEFAULT 0,
  can_edit TINYINT NOT NULL DEFAULT 0,
  can_delete TINYINT NOT NULL DEFAULT 0,
  can_manage TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(500) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sys_resource_scope_member (tenant_id, scope_id, user_id),
  KEY idx_sys_resource_scope_member_user (tenant_id, user_id)
);

CREATE TABLE IF NOT EXISTS biz_standard (
  id BIGINT NOT NULL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  standard_group_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  category VARCHAR(32) NOT NULL,
  standard_version VARCHAR(64) NOT NULL,
  version_number INT NOT NULL DEFAULT 1,
  previous_version_id BIGINT NULL,
  publish_date DATE NULL,
  status VARCHAR(32) NOT NULL,
  description TEXT NULL,
  visibility_level VARCHAR(32) NOT NULL DEFAULT 'PUBLIC',
  owner_scope_id BIGINT NOT NULL,
  shared_scope_ids VARCHAR(500) NULL,
  change_log VARCHAR(500) NULL,
  remark VARCHAR(500) NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_biz_standard_group (tenant_id, standard_group_id),
  KEY idx_biz_standard_scope (tenant_id, owner_scope_id),
  KEY idx_biz_standard_status (tenant_id, status)
);

SET @biz_standard_remark_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_standard'
    AND COLUMN_NAME = 'remark'
);

SET @biz_standard_remark_ddl = IF(
  @biz_standard_remark_exists = 0,
  'ALTER TABLE biz_standard ADD COLUMN remark VARCHAR(500) NULL AFTER change_log',
  'SELECT 1'
);

PREPARE biz_standard_remark_stmt FROM @biz_standard_remark_ddl;
EXECUTE biz_standard_remark_stmt;
DEALLOCATE PREPARE biz_standard_remark_stmt;
