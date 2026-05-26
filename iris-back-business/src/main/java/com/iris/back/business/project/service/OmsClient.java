package com.iris.back.business.project.service;

import com.iris.back.business.project.model.dto.ProjectTaskDto;
import java.util.List;

public interface OmsClient {

  /**
   * OMS 工单客户端只表达内控系统需要的能力，具体 HTTP 字段兼容留给实现类处理。
   */
  List<OmsCreateResult> createWorkOrders(ProjectTaskDto task, List<OmsCreateCommand> commands);

  OmsWorkOrderSnapshot getWorkOrder(String omsWorkOrderId);

  List<OmsWorkOrderLogSnapshot> getWorkOrderLogs(String omsWorkOrderId);

  List<OmsAttachmentSnapshot> getWorkOrderAttachments(String omsWorkOrderId);

  List<OmsUser> searchUsers(String keyword, int pageNum, int pageSize);

  void returnWorkOrder(String omsWorkOrderId, String reason);

  record OmsCreateCommand(
      String handlerId,
      String handlerEmployeeNo,
      String requesterEmployeeNo,
      String handlerName,
      String title,
      String description,
      String idempotencyKey,
      Long localWorkOrderId
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
      String content,
      String recordDate,
      String duration,
      String attachmentsPayload
  ) {
  }

  record OmsAttachmentSnapshot(
      String attachmentId,
      String fileName,
      String url
  ) {
  }

  record OmsUser(
      String userId,
      String userCode,
      String userName
  ) {
  }
}
