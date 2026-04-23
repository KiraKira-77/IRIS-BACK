package com.iris.back.system.model.dto;

public record ResourceScopeMemberDto(
    Long id,
    Long scopeId,
    Long userId,
    String account,
    String username,
    Integer canView,
    Integer canCreate,
    Integer canEdit,
    Integer canDelete,
    Integer canManage,
    String remark
) {
}
