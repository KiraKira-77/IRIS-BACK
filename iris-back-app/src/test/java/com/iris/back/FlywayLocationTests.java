package com.iris.back;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FlywayLocationTests {

  @Test
  void migrationScriptExistsOnClasspath() {
    ClassPathResource resource = new ClassPathResource("db/migration/V1__init_platform_schema.sql");
    assertThat(resource.exists()).isTrue();
  }

  @Test
  void permissionExperimentSchemaKeepsBizStandardRemarkColumnInSync() throws IOException {
    Path workspaceRoot = Path.of(System.getProperty("user.dir")).getParent();
    String initSql = Files.readString(workspaceRoot.resolve("db").resolve("init.sql"), StandardCharsets.UTF_8);
    assertThat(initSql)
        .contains("CREATE TABLE IF NOT EXISTS biz_standard")
        .contains("remark VARCHAR(500) NULL");

    ClassPathResource resource =
        new ClassPathResource("db/migration/V3__sync_permission_experiment_schema.sql");
    assertThat(resource.exists()).isTrue();
  }
}
