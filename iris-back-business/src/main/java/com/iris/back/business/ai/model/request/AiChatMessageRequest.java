package com.iris.back.business.ai.model.request;

public record AiChatMessageRequest(
    String sessionId,
    String message,
    AiChatPageContextRequest pageContext
) {
}
