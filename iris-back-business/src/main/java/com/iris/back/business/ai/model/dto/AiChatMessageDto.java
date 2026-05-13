package com.iris.back.business.ai.model.dto;

import java.util.List;

public record AiChatMessageDto(
    String id,
    String traceId,
    String sessionId,
    String role,
    String content,
    String status,
    List<AiChatCitationDto> citations,
    List<AiChatToolResultDto> toolResults,
    Long latencyMs,
    String createdAt
) {
}
