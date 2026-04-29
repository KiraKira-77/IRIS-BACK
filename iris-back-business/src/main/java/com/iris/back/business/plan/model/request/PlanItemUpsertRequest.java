package com.iris.back.business.plan.model.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record PlanItemUpsertRequest(
    String id,
    @NotBlank String targetScope,
    List<String> checklistIds,
    @NotBlank String plannedStartDate,
    @NotBlank String plannedEndDate,
    String assignee,
    String remark,
    String projectId
) {
}
