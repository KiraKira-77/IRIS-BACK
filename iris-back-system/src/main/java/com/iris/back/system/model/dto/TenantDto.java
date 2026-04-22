package com.iris.back.system.model.dto;

public record TenantDto(
    Long id,
    Long tenantId,
    String tenantCode,
    String tenantName,
    Integer status,
    String remark
) {
}
