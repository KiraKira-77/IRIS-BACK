package com.iris.back.system.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrgUpsertRequest(
    @NotNull(message = "must not be null") Long tenantId,
    Long parentId,
    @NotBlank(message = "must not be blank") String orgCode,
    @NotBlank(message = "must not be blank") String orgName,
    @NotNull(message = "must not be null") Integer orgLevel,
    @NotNull(message = "must not be null") Integer sortOrder,
    @NotNull(message = "must not be null") Integer status,
    String remark
) {
}
