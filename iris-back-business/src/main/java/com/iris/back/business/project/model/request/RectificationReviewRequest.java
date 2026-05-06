package com.iris.back.business.project.model.request;

import jakarta.validation.constraints.NotBlank;

public record RectificationReviewRequest(
    @NotBlank String action,
    String comment
) {
}
