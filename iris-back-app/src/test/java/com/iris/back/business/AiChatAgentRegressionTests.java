package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.back.business.ai.service.AiChatAgentPlanner;
import com.iris.back.business.ai.service.AiChatToolRegistry;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AiChatAgentRegressionTests {

  private final AiChatAgentPlanner planner = new AiChatAgentPlanner(new AiChatToolRegistry());

  @ParameterizedTest
  @MethodSource("businessQuestions")
  void commonBusinessQuestionsRouteToExpectedTool(String question, String intent, String toolName) {
    var plan = planner.plan(question, null);

    assertThat(plan.intent()).isEqualTo(intent);
    assertThat(plan.toolCalls()).extracting("toolName").contains(toolName);
  }

  static Stream<Arguments> businessQuestions() {
    return Stream.of(
        Arguments.of("我能看到哪些项目", "list_visible_projects", "ProjectQueryTool"),
        Arguments.of("我负责的项目有哪些", "list_visible_projects", "ProjectQueryTool"),
        Arguments.of("模型库里面现在有哪几个模型", "list_ai_models", "ModelConfigQueryTool"),
        Arguments.of("已归档的项目的档案信息帮我总结一下", "summarize_project_archives", "ProjectArchiveQueryTool"),
        Arguments.of("目前有哪些项目已经归档了", "summarize_project_archives", "ProjectArchiveQueryTool"),
        Arguments.of("现在有多少个项目归档了", "summarize_project_archives", "ProjectArchiveQueryTool"),
        Arguments.of("上面不是说了吗PRJ-2049683439946584066 这个", "analyze_referenced_project", "ProjectArchiveQueryTool"),
        Arguments.of("我有哪些整改待办", "business_data_question", "RectificationQueryTool"),
        Arguments.of("计划关联的标准文档有哪些", "business_data_question", "StandardQueryTool")
    );
  }
}
