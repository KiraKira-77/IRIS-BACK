package com.iris.back.business.ai.model.dto;

import java.util.List;

public record AiChatToolResultDto(
    String toolName,
    String summary,
    List<AiChatCitationDto> citations
) {
}
