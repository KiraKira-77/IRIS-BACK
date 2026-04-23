package com.iris.back.system.model.dto;

import java.util.List;

public record RoleDto(
    String id,
    String tenantId,
    String roleCode,
    String roleName,
    String scopeType,
    Integer status,
    String remark,
    List<String> menuCodes
) {
}
