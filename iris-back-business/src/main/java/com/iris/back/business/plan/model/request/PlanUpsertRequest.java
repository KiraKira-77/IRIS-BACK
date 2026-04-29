package com.iris.back.business.plan.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PlanUpsertRequest(
    String code,
    @NotBlank String name,
    @NotBlank String cycle,
    @NotNull Integer year,
    @NotBlank String period,
    String status,
    String description,
    @NotBlank String ownerScopeId,
    List<String> grantScopeIds,
    String parentId,
    @Valid List<PlanItemUpsertRequest> items
) {
}
