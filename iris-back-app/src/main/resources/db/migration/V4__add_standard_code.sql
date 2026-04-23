SET @biz_standard_standard_code_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_standard'
    AND COLUMN_NAME = 'standard_code'
);

SET @biz_standard_standard_code_ddl = IF(
  @biz_standard_standard_code_exists = 0,
  'ALTER TABLE biz_standard ADD COLUMN standard_code VARCHAR(64) NULL AFTER standard_group_id',
  'SELECT 1'
);

PREPARE biz_standard_standard_code_stmt FROM @biz_standard_standard_code_ddl;
EXECUTE biz_standard_standard_code_stmt;
DEALLOCATE PREPARE biz_standard_standard_code_stmt;

UPDATE biz_standard
SET standard_code = CONCAT('STD-', id)
WHERE standard_code IS NULL OR TRIM(standard_code) = '';

ALTER TABLE biz_standard
  MODIFY COLUMN standard_code VARCHAR(64) NOT NULL AFTER standard_group_id;
