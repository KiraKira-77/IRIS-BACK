package com.iris.back.business.project.model.dto;

public record ProjectTaskWorkOrderDto(
    String id,
    String projectId,
    String taskId,
    String omsWorkOrderId,
    String idempotencyKey,
    String handlerId,
    String handlerEmployeeNo,
    String handlerName,
    String workOrderTitle,
    String workOrderDescription,
    String issuedAt,
    String completedAt,
    String omsStatus,
    String omsStatusName,
    String omsResultSummary,
    String omsDetailPayload,
    String omsLogPayload,
    String omsAttachmentPayload,
    String syncStatus,
    String lastSyncedAt,
    String syncError,
    String irisReviewStatus,
    String irisReviewOpinion,
    String irisReviewedAt,
    String irisReviewedBy,
    String rectificationId,
    Boolean reviewLocked,
    Boolean reviewable
) {
}
