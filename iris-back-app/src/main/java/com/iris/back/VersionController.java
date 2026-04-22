package com.iris.back;

import com.iris.back.common.model.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {

  private final String serviceName;
  private final String appVersion;

  public VersionController(
      @Value("${spring.application.name}") String serviceName,
      @Value("${iris.app-version}") String appVersion
  ) {
    this.serviceName = serviceName;
    this.appVersion = appVersion;
  }

  @GetMapping("/api/version")
  public ApiResponse<VersionResponse> version() {
    return ApiResponse.success(new VersionResponse(serviceName, appVersion, "UP"));
  }
}
