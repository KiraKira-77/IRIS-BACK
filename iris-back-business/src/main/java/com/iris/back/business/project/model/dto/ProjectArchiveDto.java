package com.iris.back.business.project.model.dto;

import java.util.List;

public record ProjectArchiveDto(
    String id,
    String projectId,
    String projectCode,
    String projectName,
    String archiveDate,
    String archivedBy,
    String archivedByName,
    String status,
    Integer taskCount,
    Integer workOrderCount,
    Integer rectificationCount,
    Integer documentCount,
    String snapshotVersion,
    String snapshotJson,
    List<ProjectArchiveDocumentDto> documents,
    String createdAt,
    String updatedAt
) {
}
