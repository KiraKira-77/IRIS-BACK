package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.back.business.ai.service.AiChatToolRegistry;
import org.junit.jupiter.api.Test;

class AiChatToolRegistryTests {

  @Test
  void registryDocumentsCoreBusinessTools() {
    AiChatToolRegistry registry = new AiChatToolRegistry();

    assertThat(registry.definitions()).extracting("toolName")
        .contains(
            "ProjectQueryTool",
            "ProjectArchiveQueryTool",
            "ModelConfigQueryTool",
            "RectificationQueryTool",
            "PlanQueryTool",
            "StandardQueryTool",
            "WorkbenchQueryTool"
        );
    assertThat(registry.require("ProjectArchiveQueryTool").capability())
        .contains("archive");
    assertThat(registry.require("ProjectArchiveQueryTool").matchedBy("已归档的项目档案信息")).isTrue();
  }
}
