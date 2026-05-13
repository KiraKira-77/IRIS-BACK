package com.iris.back.business.ai.model.dto;

public record AiChatTraceListItemDto(
    String traceId,
    String sessionId,
    String userId,
    String username,
    String routePath,
    String question,
    String status,
    String modelName,
    String toolNamesJson,
    Long latencyMs,
    String createdAt
) {
}
