ALTER TABLE biz_project_task_work_order
  ADD COLUMN nonconformity_disposition VARCHAR(32) NULL AFTER rectification_id,
  ADD COLUMN risk_acceptance_reason TEXT NULL AFTER nonconformity_disposition,
  ADD COLUMN risk_accepted_at DATETIME NULL AFTER risk_acceptance_reason,
  ADD COLUMN risk_accepted_by BIGINT NULL AFTER risk_accepted_at;

ALTER TABLE biz_project_task_work_order
  ADD KEY idx_biz_project_wo_disposition (tenant_id, nonconformity_disposition);
