package com.iris.back.business.project.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ProjectTaskAssignRequest(
    @NotEmpty List<String> taskIds,
    @NotBlank String assigneeId,
    @NotBlank String assigneeName,
    String contactId,
    String contactName
) {
}
