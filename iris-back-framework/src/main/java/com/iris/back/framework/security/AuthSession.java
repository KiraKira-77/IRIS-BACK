package com.iris.back.framework.security;

import java.time.Instant;
import java.util.List;

public record AuthSession(
    String token,
    Long userId,
    Long tenantId,
    String account,
    String username,
    String tenantName,
    List<String> roles,
    Instant loginTime,
    Instant expireAt
) {
}
