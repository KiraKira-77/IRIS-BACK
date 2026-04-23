package com.iris.back.system.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoleUpsertRequest(
    @NotNull(message = "must not be null") Long tenantId,
    @NotBlank(message = "must not be blank") String roleCode,
    @NotBlank(message = "must not be blank") String roleName,
    @NotBlank(message = "must not be blank") String scopeType,
    @NotNull(message = "must not be null") Integer status,
    String remark,
    List<String> menuCodes
) {
}
