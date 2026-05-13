package com.iris.back.business.ai.model.dto;

public record AiModelConfigDto(
    String id,
    String name,
    String type,
    String providerType,
    String provider,
    String baseUrl,
    String modelName,
    boolean apiKeyConfigured,
    String status,
    boolean defaultModel,
    Integer timeoutSeconds,
    Double temperature,
    Integer maxTokens,
    String remark,
    String createdAt,
    String updatedAt
) {
}
