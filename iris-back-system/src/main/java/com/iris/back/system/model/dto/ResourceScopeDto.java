package com.iris.back.system.model.dto;

public record ResourceScopeDto(
    Long id,
    Long tenantId,
    String scopeCode,
    String scopeName,
    String scopeType,
    Integer status,
    String remark
) {
}
