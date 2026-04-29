package com.iris.back.business.plan.model.dto;

import java.util.List;

public record PlanDto(
    String id,
    String code,
    String name,
    String cycle,
    Integer year,
    String period,
    String status,
    String description,
    String ownerScopeId,
    List<ScopeGrantDto> grants,
    List<PlanItemDto> items,
    String parentId,
    List<PlanDto> children,
    String createdBy,
    String approvedBy,
    String createdAt,
    String updatedAt
) {
  public record ScopeGrantDto(
      String scopeId,
      List<String> actions
  ) {
  }
}
