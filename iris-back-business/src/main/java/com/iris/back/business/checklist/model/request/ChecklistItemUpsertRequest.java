package com.iris.back.business.checklist.model.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChecklistItemUpsertRequest(
    String id,
    @NotBlank String content,
    @NotBlank String criterion,
    @NotBlank String controlFrequency,
    @NotBlank String evaluationType,
    List<String> organizationIds
) {
}
