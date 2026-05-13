package com.iris.back.business.ai.model.dto;

import java.util.List;

public record AiChatAgentPlanDto(
    String intent,
    String reasoning,
    boolean requiresToolResult,
    List<AiChatToolCallDto> toolCalls
) {
}
