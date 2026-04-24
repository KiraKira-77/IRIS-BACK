package com.iris.back.system.model.dto;

public record FileAttachmentDto(
    String id,
    String name,
    String url,
    Long size,
    String type,
    String uploadedBy,
    String uploadedAt
) {
}
