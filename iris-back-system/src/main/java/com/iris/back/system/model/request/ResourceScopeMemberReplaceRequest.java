package com.iris.back.system.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ResourceScopeMemberReplaceRequest(
    @NotNull(message = "must not be null") List<@Valid ResourceScopeMemberUpsertRequest> members
) {
}
