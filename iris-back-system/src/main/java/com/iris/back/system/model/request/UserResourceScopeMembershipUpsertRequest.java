package com.iris.back.system.model.request;

import jakarta.validation.constraints.NotNull;

public record UserResourceScopeMembershipUpsertRequest(
    @NotNull(message = "must not be null") Long scopeId,
    @NotNull(message = "must not be null") Boolean canView,
    @NotNull(message = "must not be null") Boolean canCreate,
    @NotNull(message = "must not be null") Boolean canEdit,
    @NotNull(message = "must not be null") Boolean canDelete,
    @NotNull(message = "must not be null") Boolean canManage,
    String remark
) {
}
