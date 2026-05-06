package com.iris.back.business.project.model.request;

public record RectificationListQuery(
    String keyword,
    String status,
    String projectId,
    String assigneeId,
    Long page,
    Long pageSize
) {
}
