package com.iris.back.business.project.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ProjectUpsertRequest(
    String code,
    @NotBlank String name,
    @NotBlank String source,
    String planId,
    String planName,
    String description,
    @NotBlank String startDate,
    String endDate,
    List<String> tagIds,
    List<String> tagNames,
    @NotBlank String leaderId,
    @NotBlank String leaderName,
    @NotEmpty List<String> checklistIds,
    @Valid List<ProjectMemberRequest> members
) {

  public record ProjectMemberRequest(
      @NotBlank String personnelId,
      @NotBlank String personnelName,
      String employeeNo,
      String department,
      @NotBlank String role
  ) {
  }
}
