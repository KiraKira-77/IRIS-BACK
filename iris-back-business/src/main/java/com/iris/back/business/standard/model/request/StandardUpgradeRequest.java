package com.iris.back.business.standard.model.request;

import jakarta.validation.constraints.NotBlank;

public record StandardUpgradeRequest(
    @NotBlank String version,
    @NotBlank String changeLog
) {
}
