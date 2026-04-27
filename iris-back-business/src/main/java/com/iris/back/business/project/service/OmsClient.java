package com.iris.back.business.project.service;

import com.iris.back.business.project.model.dto.ProjectTaskDto;
import java.util.List;

public interface OmsClient {

  List<OmsCreateResult> createWorkOrders(ProjectTaskDto task, List<OmsCreateCommand> commands);

  OmsWorkOrderSnapshot getWorkOrder(String omsWorkOrderId);

  List<OmsWorkOrderLogSnapshot> getWorkOrderLogs(String omsWorkOrderId);

  List<OmsAttachmentSnapshot> getWorkOrderAttachments(String omsWorkOrderId);

  record OmsCreateCommand(
      String handlerId,
      String handlerName,
      String title,
      String description,
      String idempotencyKey
  ) {
  }

  record OmsCreateResult(
      String handlerId,
      String omsWorkOrderId,
      String status,
      String error,
      String responsePayload
  ) {
  }

  record OmsWorkOrderSnapshot(
      String omsWorkOrderId,
      String omsStatus,
      String omsStatusName,
      boolean reviewable,
      String resultSummary,
      String payload
  ) {
  }

  record OmsWorkOrderLogSnapshot(
      String occurredAt,
      String operator,
      String action,
      String content
  ) {
  }

  record OmsAttachmentSnapshot(
      String attachmentId,
      String fileName,
      String url
  ) {
  }
}
