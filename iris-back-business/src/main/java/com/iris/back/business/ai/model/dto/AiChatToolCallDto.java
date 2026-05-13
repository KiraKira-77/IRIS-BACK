package com.iris.back.business.ai.model.dto;

public record AiChatToolCallDto(
    String toolName,
    String keyword,
    String scope,
    String entityType,
    String entityId,
    String reason
) {
}
