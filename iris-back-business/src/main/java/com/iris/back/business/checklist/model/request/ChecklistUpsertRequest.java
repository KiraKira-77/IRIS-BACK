package com.iris.back.business.checklist.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChecklistUpsertRequest(
    @NotBlank String code,
    @NotBlank String name,
    String description,
    @NotBlank String version,
    @NotBlank String ownerScopeId,
    List<String> grantScopeIds,
    @NotBlank String status,
    String uploadDate,
    @Valid List<ChecklistItemUpsertRequest> items
) {
}
