package com.iris.back.business.ai.service;

import com.iris.back.business.ai.model.dto.AiChatAgentPlanDto;
import com.iris.back.business.ai.model.dto.AiChatToolCallDto;
import com.iris.back.business.ai.model.request.AiChatPageContextRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AiChatAgentPlanner {

  private static final String PROJECT_TOOL = "ProjectQueryTool";
  private static final String RECTIFICATION_TOOL = "RectificationQueryTool";
  private static final String WORKBENCH_TOOL = "WorkbenchQueryTool";
  private static final String PLAN_TOOL = "PlanQueryTool";
  private static final String STANDARD_TOOL = "StandardQueryTool";
  private static final String MODEL_TOOL = "ModelConfigQueryTool";
  private static final String PROJECT_ARCHIVE_TOOL = "ProjectArchiveQueryTool";
  private static final Pattern PROJECT_CODE_PATTERN = Pattern.compile("\\bPRJ-[A-Za-z0-9-]+\\b", Pattern.CASE_INSENSITIVE);
  private final AiChatToolRegistry toolRegistry;

  public AiChatAgentPlanner() {
    this(new AiChatToolRegistry());
  }

  public AiChatAgentPlanner(AiChatToolRegistry toolRegistry) {
    this.toolRegistry = toolRegistry;
  }

  public AiChatAgentPlanDto plan(String question, AiChatPageContextRequest pageContext) {
    String normalized = normalize(question);
    String projectCode = extractProjectCode(question);
    if (projectCode != null) {
      return referencedProjectPlan(projectCode, "The user referenced a project code, so query both live project data and archived project records by code.");
    }
    if (isProjectCodeContextAnalysis(normalized, pageContext)) {
      return referencedProjectPlan(pageContext.entityId(), "The user is asking about the referenced project from context, so query both live project data and archived project records by code.");
    }
    if (isCurrentProjectAnalysis(normalized, pageContext)) {
      String entityId = pageContext == null ? null : pageContext.entityId();
      return new AiChatAgentPlanDto(
          "analyze_current_project",
          "The user is asking about the current project, so gather project, rectification, and alert context before answering.",
          true,
          List.of(
              tool(PROJECT_TOOL, null, "current", "project", entityId, "Load the current project context."),
              tool(RECTIFICATION_TOOL, null, "current_project", "project", entityId, "Check rectifications linked to the current project."),
              tool(WORKBENCH_TOOL, null, "current_project", "project", entityId, "Check warnings or pending events related to the current project.")
          )
      );
    }
    if (isProjectArchiveQuestion(normalized)) {
      return new AiChatAgentPlanDto(
          "summarize_project_archives",
          "The user is asking for archived project archive records, so use the project archive tool instead of live project search.",
          true,
          List.of(tool(PROJECT_ARCHIVE_TOOL, null, "archive", null, null, "List project archives visible under current permissions."))
      );
    }
    if (isVisibleProjectInventory(normalized)) {
      return new AiChatAgentPlanDto(
          "list_visible_projects",
          "The user wants an inventory of projects visible to the current account, not a keyword search.",
          true,
          List.of(tool(PROJECT_TOOL, null, "visible", null, null, "List projects visible under current permissions."))
      );
    }
    if (matchesTool(MODEL_TOOL, question)) {
      return new AiChatAgentPlanDto(
          "list_ai_models",
          "The user is asking about configured AI models, so use the model configuration tool.",
          true,
          List.of(tool(MODEL_TOOL, null, "tenant", null, null, "List model configs in the current tenant."))
      );
    }

    List<AiChatToolCallDto> calls = new ArrayList<>();
    if (matchesTool(PROJECT_TOOL, question)) {
      calls.add(tool(PROJECT_TOOL, question, "keyword", null, null, "Find projects matching the question."));
    }
    if (matchesTool(RECTIFICATION_TOOL, question)) {
      calls.add(tool(RECTIFICATION_TOOL, question, "keyword", null, null, "Find rectifications or work orders matching the question."));
    }
    if (matchesTool(PLAN_TOOL, question)) {
      calls.add(tool(PLAN_TOOL, question, "keyword", null, null, "Find plans matching the question."));
    }
    if (matchesTool(STANDARD_TOOL, question)) {
      calls.add(tool(STANDARD_TOOL, question, "keyword", null, null, "Find standards or documents matching the question."));
    }
    if (matchesTool(WORKBENCH_TOOL, question)) {
      calls.add(tool(WORKBENCH_TOOL, question, "keyword", null, null, "Find alerts or pending events matching the question."));
    }
    if (matchesTool(PROJECT_ARCHIVE_TOOL, question)) {
      calls.add(tool(PROJECT_ARCHIVE_TOOL, null, "archive", null, null, "Find project archives matching the question."));
    }

    return new AiChatAgentPlanDto(
        calls.isEmpty() ? "general_question" : "business_data_question",
        calls.isEmpty()
            ? "No supported business tool was required by the current question."
            : "The question needs one or more business tools before the model writes the answer.",
        !calls.isEmpty(),
        calls
    );
  }

  private boolean isCurrentProjectAnalysis(String normalized, AiChatPageContextRequest pageContext) {
    return pageContext != null
        && "project".equals(pageContext.entityType())
        && trimToNull(pageContext.entityId()) != null
        && containsAny(normalized, "\u8fd9\u4e2a\u9879\u76ee", "\u5f53\u524d\u9879\u76ee", "this project", "current project");
  }

  private boolean isProjectCodeContextAnalysis(String normalized, AiChatPageContextRequest pageContext) {
    return pageContext != null
        && "project_code".equals(pageContext.entityType())
        && trimToNull(pageContext.entityId()) != null
        && containsAny(
            normalized,
            "\u8fd9\u4e2a\u9879\u76ee",
            "\u5f53\u524d\u9879\u76ee",
            "\u4e0a\u9762",
            "\u521a\u624d",
            "this project",
            "current project"
        );
  }

  private boolean isVisibleProjectInventory(String normalized) {
    return containsAny(
        normalized,
        "which project",
        "what project",
        "project can i see",
        "projects can i see",
        "my project",
        "\u54ea\u4e9b\u9879\u76ee",
        "\u770b\u5230\u54ea\u4e9b\u9879\u76ee",
        "\u80fd\u770b\u5230",
        "\u6211\u7684\u9879\u76ee",
        "\u8d1f\u8d23\u7684\u9879\u76ee"
    );
  }

  private boolean isProjectArchiveQuestion(String normalized) {
    return matchesTool(PROJECT_ARCHIVE_TOOL, normalized);
  }

  private AiChatAgentPlanDto referencedProjectPlan(String projectCode, String reason) {
    String keyword = trimToNull(projectCode);
    return new AiChatAgentPlanDto(
        "analyze_referenced_project",
        reason,
        true,
        List.of(
            tool(PROJECT_TOOL, keyword, "keyword", "project_code", keyword, "Find the referenced project by code."),
            tool(PROJECT_ARCHIVE_TOOL, keyword, "archive", "project_code", keyword, "Find archived project records by code.")
        )
    );
  }

  private String extractProjectCode(String question) {
    if (question == null) {
      return null;
    }
    Matcher matcher = PROJECT_CODE_PATTERN.matcher(question);
    return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : null;
  }

  private boolean matchesTool(String toolName, String question) {
    return toolRegistry.require(toolName).matchedBy(question);
  }

  private AiChatToolCallDto tool(
      String toolName,
      String keyword,
      String scope,
      String entityType,
      String entityId,
      String reason
  ) {
    return new AiChatToolCallDto(toolName, trimToNull(keyword), scope, entityType, entityId, reason);
  }

  private String normalize(String value) {
    return value == null ? "" : value.toLowerCase(Locale.ROOT);
  }

  private boolean containsAny(String value, String... keywords) {
    for (String keyword : keywords) {
      if (value.contains(keyword.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
