package com.iris.back.framework.security;

import java.util.List;

public record CurrentUserPrincipal(
    String token,
    Long userId,
    Long tenantId,
    String account,
    String username,
    String tenantName,
    List<String> roles
) {
}
