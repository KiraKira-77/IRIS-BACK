ALTER TABLE biz_control_checklist
  DROP INDEX idx_biz_control_checklist_tag;

ALTER TABLE biz_control_checklist
  ADD COLUMN owner_scope_id BIGINT NULL AFTER checklist_version,
  ADD COLUMN shared_scope_ids VARCHAR(500) NULL AFTER owner_scope_id;

UPDATE biz_control_checklist
SET owner_scope_id = CASE
    WHEN primary_tag_id REGEXP '^[0-9]+$' THEN CAST(primary_tag_id AS UNSIGNED)
    WHEN primary_tag_id = 'finance' THEN 9001
    WHEN primary_tag_id = 'it' THEN 9002
    ELSE 9003
  END,
  shared_scope_ids = TRIM(BOTH ',' FROM REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
    COALESCE(secondary_tag_ids, ''),
    'finance', '9001'),
    'it', '9002'),
    'purchase', '9003'),
    'asset', '9003'),
    'contract', '9003'),
    'hr', '9003'),
    'sales', '9003'),
    'operation', '9003'));

ALTER TABLE biz_control_checklist
  MODIFY COLUMN owner_scope_id BIGINT NOT NULL,
  DROP COLUMN primary_tag_id,
  DROP COLUMN secondary_tag_ids;

ALTER TABLE biz_control_checklist
  ADD KEY idx_biz_control_checklist_scope (tenant_id, owner_scope_id);
