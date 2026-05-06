ALTER TABLE biz_project_rectification
  DROP INDEX uk_biz_project_rect_source;

ALTER TABLE biz_project_rectification
  MODIFY source_work_order_record_id BIGINT NULL;

ALTER TABLE biz_project_rectification
  ADD KEY idx_biz_project_rect_source (tenant_id, source_work_order_record_id);
