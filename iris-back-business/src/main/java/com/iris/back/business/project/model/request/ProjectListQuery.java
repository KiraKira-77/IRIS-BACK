package com.iris.back.business.project.model.request;

public record ProjectListQuery(
    String keyword,
    String status,
    String tagId,
    String source,
    String startDate,
    String endDate,
    Long page,
    Long pageSize
) {
}
