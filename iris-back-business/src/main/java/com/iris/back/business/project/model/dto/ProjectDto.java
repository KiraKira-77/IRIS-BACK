package com.iris.back.business.project.model.dto;

import java.util.List;

public record ProjectDto(
    String id,
    String code,
    String name,
    String source,
    String planId,
    String planName,
    String description,
    String startDate,
    String endDate,
    String status,
    List<String> tagIds,
    List<String> tagNames,
    String leaderId,
    String leaderName,
    List<String> checklistIds,
    String archiveStatus,
    String archiveStartedAt,
    String archiveCompletedAt,
    String archiveError,
    Integer taskCount,
    Integer passedTaskCount,
    Integer nonconformingTaskCount,
    Integer progress,
    List<ProjectMemberDto> members,
    List<ProjectTaskDto> tasks,
    List<String> actions,
    String createdAt,
    String updatedAt
) {
}
