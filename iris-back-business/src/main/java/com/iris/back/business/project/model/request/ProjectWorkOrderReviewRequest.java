package com.iris.back.business.project.model.request;

import jakarta.validation.constraints.NotBlank;

public record ProjectWorkOrderReviewRequest(
    @NotBlank String reviewStatus,
    String opinion
) {
}
