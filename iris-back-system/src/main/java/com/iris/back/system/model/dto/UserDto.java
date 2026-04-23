package com.iris.back.system.model.dto;

import java.util.List;

public record UserDto(
    String id,
    String tenantId,
    String orgId,
    String account,
    String username,
    String email,
    String mobile,
    Integer status,
    String remark,
    List<String> roleIds,
    List<String> roleCodes
) {
}
