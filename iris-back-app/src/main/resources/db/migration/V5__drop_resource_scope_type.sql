SET @resource_scope_type_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_resource_scope'
    AND COLUMN_NAME = 'scope_type'
);

SET @resource_scope_type_ddl = IF(
  @resource_scope_type_exists > 0,
  'ALTER TABLE sys_resource_scope DROP COLUMN scope_type',
  'SELECT 1'
);

PREPARE resource_scope_type_stmt FROM @resource_scope_type_ddl;
EXECUTE resource_scope_type_stmt;
DEALLOCATE PREPARE resource_scope_type_stmt;
