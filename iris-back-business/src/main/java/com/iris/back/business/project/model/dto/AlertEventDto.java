package com.iris.back.business.project.model.dto;

public record AlertEventDto(
    String id,
    String source,
    String level,
    String title,
    String content,
    String timestamp,
    boolean acknowledged
) {
}
