package com.iris.back.business.ai.model.dto;

import java.util.List;

public record AiChatTraceDetailDto(
    String traceId,
    String sessionId,
    String userId,
    String username,
    String routePath,
    String entityType,
    String entityId,
    String question,
    String answer,
    String status,
    String providerType,
    String modelName,
    String toolNamesJson,
    String citationsJson,
    Long latencyMs,
    String errorMessage,
    String createdAt,
    String updatedAt,
    List<AiChatTraceEventDto> events
) {
}
