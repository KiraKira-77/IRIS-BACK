package com.iris.back.business.project.model.dto;

import java.util.List;

public record ProjectArchiveDocumentDto(
    String id,
    String archiveId,
    String category,
    String name,
    List<Object> attachments
) {
}
