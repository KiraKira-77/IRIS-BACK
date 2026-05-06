package com.iris.back.business.project.model.dto;

import java.util.List;

public record RectificationDto(
    String id,
    String code,
    String source,
    String taskId,
    String projectId,
    String projectName,
    String title,
    String description,
    String assigneeId,
    String assigneeName,
    String reviewerId,
    String reviewerName,
    String status,
    String deadline,
    List<Object> attachments,
    String reviewComment,
    List<Object> logs,
    String createdAt,
    String updatedAt
) {
}
