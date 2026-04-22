package com.iris.back.framework.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI irisOpenApi() {
    return new OpenAPI().info(new Info()
        .title("IRIS-BACK API")
        .version("v1")
        .description("Platform-first backend scaffold for IRIS"));
  }
}
