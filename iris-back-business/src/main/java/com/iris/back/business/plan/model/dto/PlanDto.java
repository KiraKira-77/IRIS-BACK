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
    String generatedProjectId,
    String generatedProjectName,
    String createdBy,
    String approvedBy,
    String createdAt,
    String updatedAt
) {
  public PlanDto(
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
    this(
        id,
        code,
        name,
        cycle,
        year,
        period,
        status,
        description,
        ownerScopeId,
        grants,
        items,
        parentId,
        children,
        null,
        null,
        createdBy,
        approvedBy,
        createdAt,
        updatedAt
    );
  }

  public record ScopeGrantDto(
      String scopeId,
      List<String> actions
  ) {
  }
}
