package com.iris.back.business.ai.service;

import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

@Service
public class AiChatToolRegistry {

  private final List<ToolDefinition> definitions = List.of(
      new ToolDefinition(
          "ProjectQueryTool",
          "Query live projects visible to the current account.",
          List.of(List.of("project", "\u9879\u76ee", "\u68c0\u67e5\u9879", "task"))
      ),
      new ToolDefinition(
          "ProjectArchiveQueryTool",
          "Query archived project archive records and frozen archive snapshots.",
          List.of(List.of("archive", "\u6863\u6848", "\u5f52\u6863", "\u5df2\u5f52\u6863"), List.of("project", "\u9879\u76ee"))
      ),
      new ToolDefinition(
          "ModelConfigQueryTool",
          "Query AI model library and tenant model configurations.",
          List.of(List.of("model", "\u6a21\u578b", "\u6a21\u578b\u5e93"))
      ),
      new ToolDefinition(
          "RectificationQueryTool",
          "Query rectifications and work orders visible to the current account.",
          List.of(List.of("rectification", "\u6574\u6539", "\u5de5\u5355"))
      ),
      new ToolDefinition(
          "PlanQueryTool",
          "Query control plans visible to the current account.",
          List.of(List.of("plan", "\u8ba1\u5212"))
      ),
      new ToolDefinition(
          "StandardQueryTool",
          "Query standards, policies, and documents visible to the current account.",
          List.of(List.of("standard", "\u6807\u51c6", "\u5236\u5ea6", "\u6587\u6863"))
      ),
      new ToolDefinition(
          "WorkbenchQueryTool",
          "Query alerts, pending items, and overdue reminders.",
          List.of(List.of("alert", "\u9884\u8b66", "\u5f85\u529e", "overdue", "\u903e\u671f"))
      )
  );

  public List<ToolDefinition> definitions() {
    return definitions;
  }

  public ToolDefinition require(String toolName) {
    return definitions.stream()
        .filter(definition -> definition.toolName().equals(toolName))
        .findFirst()
        .orElseThrow(() -> new NoSuchElementException("AI_CHAT_TOOL_NOT_FOUND: " + toolName));
  }

  public record ToolDefinition(
      String toolName,
      String capability,
      List<List<String>> keywordGroups
  ) {

    public boolean matchedBy(String question) {
      String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
      return keywordGroups.stream()
          .allMatch(group -> group.stream()
              .filter(keyword -> keyword != null && !keyword.isBlank())
              .anyMatch(keyword -> normalized.contains(keyword.toLowerCase(Locale.ROOT))));
    }
  }
}
