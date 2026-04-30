ALTER TABLE biz_project_task_work_order
  DROP INDEX uk_biz_project_wo_handler;

ALTER TABLE biz_project_task_work_order
  ADD KEY idx_biz_project_wo_handler (tenant_id, task_id, handler_employee_no);
