package com.iris.back.business.checklist.model.dto;

import java.util.List;

public record ChecklistDto(
    String id,
    String code,
    String name,
    String description,
    String version,
    String ownerScopeId,
    List<ScopeGrantDto> grants,
    List<ChecklistItemDto> items,
    String status,
    String uploadDate,
    String createdAt,
    String updatedAt
) {
  public record ScopeGrantDto(
      String scopeId,
      List<String> actions
  ) {
  }
}
