package com.iris.back.business.standard.model.dto;

import java.util.List;

public record StandardDto(
    String id,
    String standardGroupId,
    String standardCode,
    String title,
    String category,
    String version,
    String publishDate,
    String status,
    List<String> attachments,
    String description,
    String createdAt,
    String updatedAt,
    Integer versionNumber,
    String previousVersionId,
    String visibilityLevel,
    String ownerScopeId,
    List<ScopeGrantDto> grants,
    String changeLog
) {
  public record ScopeGrantDto(
      String scopeId,
      List<String> actions
  ) {
  }
}
