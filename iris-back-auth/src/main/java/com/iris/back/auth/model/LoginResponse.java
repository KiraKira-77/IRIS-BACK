package com.iris.back.auth.model;

public record LoginResponse(
    String token,
    Long userId,
    Long tenantId,
    String username,
    String tenantName
) {
}
