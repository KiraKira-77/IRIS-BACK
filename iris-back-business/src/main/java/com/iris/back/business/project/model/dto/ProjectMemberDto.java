package com.iris.back.business.project.model.dto;

public record ProjectMemberDto(
    String id,
    String personnelId,
    String personnelName,
    String employeeNo,
    String department,
    String role
) {
}
