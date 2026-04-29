package com.iris.back.business.plan.model.dto;

import java.util.List;

public record PlanItemDto(
    String id,
    String planId,
    Integer sequence,
    String targetScope,
    List<String> checklistIds,
    String plannedStartDate,
    String plannedEndDate,
    String assignee,
    String remark,
    String projectId
) {
}
