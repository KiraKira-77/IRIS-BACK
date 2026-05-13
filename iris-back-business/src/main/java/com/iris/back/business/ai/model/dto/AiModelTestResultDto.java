package com.iris.back.business.ai.model.dto;

public record AiModelTestResultDto(
    boolean success,
    String message,
    Long latencyMs
) {
}
