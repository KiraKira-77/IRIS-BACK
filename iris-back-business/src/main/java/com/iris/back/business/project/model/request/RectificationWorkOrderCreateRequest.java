package com.iris.back.business.project.model.request;

import jakarta.validation.constraints.NotBlank;

public record RectificationWorkOrderCreateRequest(
    @NotBlank String title,
    String description,
    @NotBlank String handlerId,
    @NotBlank String handlerEmployeeNo,
    @NotBlank String handlerName
) {
}
