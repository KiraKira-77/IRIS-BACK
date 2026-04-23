package com.iris.back.business.standard.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StandardUpsertRequest(
    @NotNull Long tenantId,
    @NotBlank String title,
    @NotBlank String category,
    @NotBlank String version,
    @NotBlank String status,
    String publishDate,
    String description,
    @NotBlank String standardCode,
    String standardGroupId,
    Integer versionNumber,
    String previousVersionId,
    @NotBlank String visibilityLevel,
    @NotBlank String ownerScopeId,
    List<String> grantScopeIds,
    String changeLog
) {
}
