package com.iris.back.system.model.dto;

public record ResourceScopeDto(
    String id,
    String tenantId,
    String scopeCode,
    String scopeName,
    Integer status,
    String remark
) {
}
