package com.iris.back.business.project.model.dto;

public record ProjectOperationLogDto(
    String id,
    String source,
    String level,
    String message,
    String detail,
    String timestamp,
    String projectId,
    String taskId,
    String workOrderId,
    String action,
    String operatorName,
    String result
) {
}
