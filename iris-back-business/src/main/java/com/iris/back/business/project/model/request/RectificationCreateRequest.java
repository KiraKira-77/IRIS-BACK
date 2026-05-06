package com.iris.back.business.project.model.request;

import jakarta.validation.constraints.NotBlank;

public record RectificationCreateRequest(
    @NotBlank String title,
    String description,
    String projectId,
    String projectName,
    String taskId,
    @NotBlank String assigneeId,
    @NotBlank String assigneeName,
    String reviewerId,
    String reviewerName,
    String deadline
) {
}
