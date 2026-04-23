package com.iris.back.system.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UserUpsertRequest(
    @NotNull(message = "must not be null") Long tenantId,
    Long orgId,
    @NotBlank(message = "must not be blank") String account,
    @NotBlank(message = "must not be blank") String username,
    String email,
    String mobile,
    @NotNull(message = "must not be null") Integer status,
    String remark,
    List<Long> roleIds
) {
}
