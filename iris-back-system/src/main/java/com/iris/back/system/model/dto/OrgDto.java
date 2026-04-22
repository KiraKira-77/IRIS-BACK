package com.iris.back.system.model.dto;

public record OrgDto(
    Long id,
    Long tenantId,
    Long parentId,
    String orgCode,
    String orgName,
    Integer orgLevel,
    Integer sortOrder,
    Integer status,
    String remark
) {
}
