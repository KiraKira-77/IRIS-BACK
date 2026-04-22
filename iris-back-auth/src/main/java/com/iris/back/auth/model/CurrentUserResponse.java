package com.iris.back.auth.model;

import java.util.List;
public record CurrentUserResponse(
    Long userId,
    Long tenantId,
    String username,
    String displayName,
    String tenantName,
    List<String> roles
) {
}
