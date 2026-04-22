package com.iris.back.system.model.dto;

public record RoleDto(
    Long id,
    Long tenantId,
    String roleCode,
    String roleName,
    String scopeType,
    Integer status,
    String remark
) {
}
