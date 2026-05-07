ALTER TABLE biz_project_rectification
  ADD COLUMN completed_at DATETIME NULL AFTER deadline,
  ADD COLUMN review_result VARCHAR(32) NULL AFTER completed_at,
  ADD COLUMN rectification_oms_work_order_id VARCHAR(100) NULL AFTER review_result,
  ADD COLUMN rectification_oms_status VARCHAR(32) NULL AFTER rectification_oms_work_order_id,
  ADD COLUMN rectification_oms_status_name VARCHAR(100) NULL AFTER rectification_oms_status,
  ADD COLUMN rectification_work_order_created_at DATETIME NULL AFTER rectification_oms_status_name,
  ADD COLUMN rectification_work_order_completed_at DATETIME NULL AFTER rectification_work_order_created_at;
