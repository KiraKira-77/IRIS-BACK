package com.iris.back.business.project.model.dto;

import java.util.List;

public record RectificationDto(
    String id,
    String code,
    String source,
    String taskId,
    String taskName,
    String taskDescription,
    String projectId,
    String projectName,
    String checkContent,
    String sourceWorkOrderRecordId,
    String sourceOmsWorkOrderId,
    String title,
    String description,
    String assigneeId,
    String assigneeName,
    String reviewerId,
    String reviewerName,
    String status,
    String issuedAt,
    String deadline,
    String completedAt,
    String reviewResult,
    String rectificationOmsWorkOrderId,
    String rectificationOmsStatus,
    String rectificationOmsStatusName,
    String rectificationWorkOrderCreatedAt,
    String rectificationWorkOrderCompletedAt,
    String rectificationOmsDetailPayload,
    String rectificationOmsLogPayload,
    String rectificationOmsAttachmentPayload,
    List<Object> attachments,
    String reviewComment,
    List<Object> logs,
    String createdAt,
    String updatedAt
) {
}
