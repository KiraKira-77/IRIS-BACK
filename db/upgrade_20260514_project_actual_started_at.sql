ALTER TABLE biz_project
  ADD COLUMN actual_started_at DATETIME NULL AFTER end_date;

UPDATE biz_project
SET actual_started_at = CAST(start_date AS DATETIME)
WHERE actual_started_at IS NULL
  AND status <> 'not_started';
