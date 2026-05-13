package com.iris.back.business.ai.model.request;

public record AiChatPageContextRequest(
    String routePath,
    String entityType,
    String entityId
) {
}
