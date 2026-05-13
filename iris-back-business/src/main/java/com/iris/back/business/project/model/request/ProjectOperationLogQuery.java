package com.iris.back.business.project.model.request;

public record ProjectOperationLogQuery(
    String keyword,
    String source,
    String level,
    String action,
    String projectId,
    Long page,
    Long pageSize
) {
}
