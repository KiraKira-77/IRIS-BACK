package com.iris.back.business.project.model.request;

public record AlertQuery(
    String keyword,
    String level,
    Long page,
    Long pageSize
) {
}
