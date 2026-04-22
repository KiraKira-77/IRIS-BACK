package com.iris.back.system.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TenantUpsertRequest(
    @NotBlank(message = "must not be blank") String tenantCode,
    @NotBlank(message = "must not be blank") String tenantName,
    @NotNull(message = "must not be null") Integer status,
    String remark
) {
}
