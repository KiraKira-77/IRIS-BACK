package com.iris.back.business.checklist.model.dto;

import java.util.List;

public record ChecklistItemDto(
    String id,
    String checklistId,
    Integer sequence,
    String content,
    String criterion,
    String controlFrequency,
    String evaluationType,
    List<String> organizationIds
) {
}
