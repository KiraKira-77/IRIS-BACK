package com.iris.back;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FlywayLocationTests {

  @Test
  void migrationScriptExistsOnClasspath() {
    ClassPathResource resource = new ClassPathResource("db/migration/V1__init_platform_schema.sql");
    assertThat(resource.exists()).isTrue();
  }
}
