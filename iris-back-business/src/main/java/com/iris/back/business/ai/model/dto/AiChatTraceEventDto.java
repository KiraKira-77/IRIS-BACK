package com.iris.back.business.ai.model.dto;

public record AiChatTraceEventDto(
    Integer sequenceNo,
    String eventType,
    String eventName,
    String status,
    String detailJson,
    Long elapsedMs,
    String createdAt
) {
}
