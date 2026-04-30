ALTER TABLE biz_project_task_work_order
  ADD COLUMN handler_employee_no VARCHAR(64) NULL AFTER handler_id;

UPDATE biz_project_task_work_order
SET handler_employee_no = CAST(handler_id AS CHAR)
WHERE handler_employee_no IS NULL OR handler_employee_no = '';

ALTER TABLE biz_project_task_work_order
  MODIFY handler_employee_no VARCHAR(64) NOT NULL;

ALTER TABLE biz_project_task_work_order
  DROP INDEX uk_biz_project_wo_handler;

ALTER TABLE biz_project_task_work_order
  ADD KEY idx_biz_project_wo_handler (tenant_id, task_id, handler_employee_no);
