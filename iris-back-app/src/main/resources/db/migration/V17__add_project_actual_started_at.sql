SET @biz_project_actual_started_at_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_project'
    AND COLUMN_NAME = 'actual_started_at'
);

SET @biz_project_actual_started_at_ddl = IF(
  @biz_project_actual_started_at_exists = 0,
  'ALTER TABLE biz_project ADD COLUMN actual_started_at DATETIME NULL AFTER end_date',
  'SELECT 1'
);

PREPARE biz_project_actual_started_at_stmt FROM @biz_project_actual_started_at_ddl;
EXECUTE biz_project_actual_started_at_stmt;
DEALLOCATE PREPARE biz_project_actual_started_at_stmt;

UPDATE biz_project
SET actual_started_at = CAST(start_date AS DATETIME)
WHERE actual_started_at IS NULL
  AND status <> 'not_started';
