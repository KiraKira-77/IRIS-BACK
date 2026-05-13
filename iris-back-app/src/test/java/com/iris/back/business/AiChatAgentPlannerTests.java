package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.back.business.ai.model.request.AiChatPageContextRequest;
import com.iris.back.business.ai.service.AiChatAgentPlanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiChatAgentPlannerTests {

  private AiChatAgentPlanner planner;

  @BeforeEach
  void setUp() {
    planner = new AiChatAgentPlanner();
  }

  @Test
  void plansVisibleProjectInventoryWithoutKeyword() {
    var plan = planner.plan(
        "我能看到哪些项目",
        new AiChatPageContextRequest("/project/list", null, null)
    );

    assertThat(plan.intent()).isEqualTo("list_visible_projects");
    assertThat(plan.toolCalls()).extracting("toolName").containsExactly("ProjectQueryTool");
    assertThat(plan.toolCalls().getFirst().keyword()).isNull();
    assertThat(plan.toolCalls().getFirst().scope()).isEqualTo("visible");
  }

  @Test
  void plansModelLibraryQuestionToModelTool() {
    var plan = planner.plan("模型库里面现在有哪几个模型", null);

    assertThat(plan.intent()).isEqualTo("list_ai_models");
    assertThat(plan.toolCalls()).extracting("toolName").containsExactly("ModelConfigQueryTool");
    assertThat(plan.requiresToolResult()).isTrue();
  }

  @Test
  void plansArchivedProjectArchiveQuestionToArchiveTool() {
    var plan = planner.plan("已归档的项目的档案信息帮我总结一下", null);

    assertThat(plan.intent()).isEqualTo("summarize_project_archives");
    assertThat(plan.toolCalls()).extracting("toolName").containsExactly("ProjectArchiveQueryTool");
    assertThat(plan.toolCalls().getFirst().keyword()).isNull();
    assertThat(plan.toolCalls().getFirst().scope()).isEqualTo("archive");
  }

  @Test
  void archivedProjectInventoryUsesArchiveToolBeforeVisibleProjectInventory() {
    var plan = planner.plan("目前有哪些项目已经归档了", null);

    assertThat(plan.intent()).isEqualTo("summarize_project_archives");
    assertThat(plan.toolCalls()).extracting("toolName").containsExactly("ProjectArchiveQueryTool");
  }

  @Test
  void projectCodeQuestionQueriesProjectAndArchiveByCode() {
    var plan = planner.plan("上面不是说了吗PRJ-2049683439946584066 这个", null);

    assertThat(plan.intent()).isEqualTo("analyze_referenced_project");
    assertThat(plan.toolCalls()).extracting("toolName")
        .containsExactly("ProjectQueryTool", "ProjectArchiveQueryTool");
    assertThat(plan.toolCalls()).extracting("keyword")
        .containsExactly("PRJ-2049683439946584066", "PRJ-2049683439946584066");
  }

  @Test
  void currentProjectReferenceFromSessionContextQueriesProjectAndArchive() {
    var plan = planner.plan(
        "你能总结一下这个项目完成的内容吗",
        new AiChatPageContextRequest("/project/list", "project_code", "PRJ-2049683439946584066")
    );

    assertThat(plan.intent()).isEqualTo("analyze_referenced_project");
    assertThat(plan.toolCalls()).extracting("toolName")
        .containsExactly("ProjectQueryTool", "ProjectArchiveQueryTool");
    assertThat(plan.toolCalls()).extracting("keyword")
        .containsExactly("PRJ-2049683439946584066", "PRJ-2049683439946584066");
  }

  @Test
  void usesCurrentProjectPageContextForProjectAnalysis() {
    var plan = planner.plan(
        "这个项目有什么风险",
        new AiChatPageContextRequest("/project/detail/9001", "project", "9001")
    );

    assertThat(plan.intent()).isEqualTo("analyze_current_project");
    assertThat(plan.toolCalls()).extracting("toolName")
        .containsExactly("ProjectQueryTool", "RectificationQueryTool", "WorkbenchQueryTool");
    assertThat(plan.toolCalls().getFirst().entityId()).isEqualTo("9001");
  }
}
