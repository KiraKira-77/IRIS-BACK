package com.iris.back;

public record VersionResponse(
    String service,
    String version,
    String status
) {
}
