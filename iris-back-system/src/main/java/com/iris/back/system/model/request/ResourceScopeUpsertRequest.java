package com.iris.back.system.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResourceScopeUpsertRequest(
    @NotNull(message = "must not be null") Long tenantId,
    String scopeCode,
    @NotBlank(message = "must not be blank") String scopeName,
    @NotBlank(message = "must not be blank") String scopeType,
    @NotNull(message = "must not be null") Integer status,
    String remark
) {
}
