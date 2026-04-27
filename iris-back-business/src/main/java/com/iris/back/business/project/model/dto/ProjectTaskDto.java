package com.iris.back.business.project.model.dto;

import java.util.List;

public record ProjectTaskDto(
    String id,
    String projectId,
    String checklistId,
    String checklistName,
    String checklistItemId,
    String checkContent,
    String checkCriterion,
    String controlFrequency,
    String evaluationType,
    String taskName,
    String taskDescription,
    String assigneeId,
    String assigneeName,
    String contactId,
    String contactName,
    String status,
    String issuedAt,
    String completedAt,
    Integer workOrderCount,
    Integer passedWorkOrderCount,
    Integer nonconformingWorkOrderCount,
    List<ProjectTaskWorkOrderDto> workOrders,
    List<String> actions
) {
}
