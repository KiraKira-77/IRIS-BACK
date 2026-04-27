package com.iris.back.business.standard.model.dto;

import com.iris.back.system.model.dto.FileAttachmentDto;
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
    List<FileAttachmentDto> attachments,
    String description,
    String createdAt,
    String updatedAt,
    Integer versionNumber,
    Integer versionCount,
    String previousVersionId,
    String visibilityLevel,
    String ownerScopeId,
    List<ScopeGrantDto> grants,
    String changeLog,
    String operatorName
) {
  public StandardDto(
      String id,
      String standardGroupId,
      String standardCode,
      String title,
      String category,
      String version,
      String publishDate,
      String status,
      List<FileAttachmentDto> attachments,
      String description,
      String createdAt,
      String updatedAt,
      Integer versionNumber,
      String previousVersionId,
      String visibilityLevel,
      String ownerScopeId,
      List<ScopeGrantDto> grants,
      String changeLog,
      String operatorName
  ) {
    this(
        id,
        standardGroupId,
        standardCode,
        title,
        category,
        version,
        publishDate,
        status,
        attachments,
        description,
        createdAt,
        updatedAt,
        versionNumber,
        null,
        previousVersionId,
        visibilityLevel,
        ownerScopeId,
        grants,
        changeLog,
        operatorName
    );
  }

  public record ScopeGrantDto(
      String scopeId,
      List<String> actions
  ) {
  }
}
