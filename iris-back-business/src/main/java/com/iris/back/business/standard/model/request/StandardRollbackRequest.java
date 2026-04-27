package com.iris.back.business.standard.model.request;

import jakarta.validation.constraints.NotBlank;

public record StandardRollbackRequest(
    @NotBlank String version,
    @NotBlank String reason
) {
}
