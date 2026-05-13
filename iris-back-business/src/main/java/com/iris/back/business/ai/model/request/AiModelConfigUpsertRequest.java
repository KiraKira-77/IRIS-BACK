package com.iris.back.business.ai.model.request;

public record AiModelConfigUpsertRequest(
    String name,
    String providerType,
    String baseUrl,
    String modelName,
    String apiKey,
    String status,
    Boolean defaultModel,
    Integer timeoutSeconds,
    Double temperature,
    Integer maxTokens,
    String remark
) {
}
