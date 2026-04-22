package com.iris.back.system.model.dto;

public record UserDto(
    Long id,
    Long tenantId,
    Long orgId,
    String account,
    String username,
    String email,
    String mobile,
    Integer status,
    String remark
) {
}
