package com.iris.back.business.ai.service;

import com.iris.back.business.ai.model.dto.AiChatCitationDto;
import com.iris.back.business.ai.model.dto.AiChatAgentPlanDto;
import com.iris.back.business.ai.model.dto.AiChatToolCallDto;
import com.iris.back.business.ai.model.dto.AiChatToolResultDto;
import com.iris.back.business.ai.model.dto.AiModelConfigDto;
import com.iris.back.business.ai.model.request.AiChatPageContextRequest;
import com.iris.back.business.plan.model.dto.PlanDto;
import com.iris.back.business.plan.model.request.PlanListQuery;
import com.iris.back.business.plan.service.PlanService;
import com.iris.back.business.project.model.dto.AlertEventDto;
import com.iris.back.business.project.model.dto.ProjectArchiveDto;
import com.iris.back.business.project.model.dto.ProjectArchiveDocumentDto;
import com.iris.back.business.project.model.dto.ProjectDto;
import com.iris.back.business.project.model.dto.RectificationDto;
import com.iris.back.business.project.model.request.AlertQuery;
import com.iris.back.business.project.model.request.ProjectListQuery;
import com.iris.back.business.project.model.request.RectificationListQuery;
import com.iris.back.business.project.service.AlertService;
import com.iris.back.business.project.service.ProjectArchiveService;
import com.iris.back.business.project.service.ProjectService;
import com.iris.back.business.project.service.RectificationService;
import com.iris.back.business.standard.model.dto.StandardDto;
import com.iris.back.business.standard.model.request.StandardListQuery;
import com.iris.back.business.standard.service.StandardService;
import com.iris.back.common.model.PageResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class AiChatContextService {

  private final ProjectService projectService;
  private final RectificationService rectificationService;
  private final PlanService planService;
  private final StandardService standardService;
  private final AlertService alertService;
  private final AiModelConfigService aiModelConfigService;
  private final ProjectArchiveService projectArchiveService;

  public AiChatContextService(
      ProjectService projectService,
      RectificationService rectificationService,
      PlanService planService,
      StandardService standardService,
      AlertService alertService,
      AiModelConfigService aiModelConfigService,
      ProjectArchiveService projectArchiveService
  ) {
    this.projectService = projectService;
    this.rectificationService = rectificationService;
    this.planService = planService;
    this.standardService = standardService;
    this.alertService = alertService;
    this.aiModelConfigService = aiModelConfigService;
    this.projectArchiveService = projectArchiveService;
  }

  public List<AiChatToolResultDto> collectContext(String question, AiChatPageContextRequest pageContext) {
    String normalized = normalize(question);
    List<AiChatToolResultDto> results = new ArrayList<>();
    if (containsAny(normalized, "project", "项目", "检查项", "task")) {
      results.add(projectContext(question));
    }
    if (containsAny(normalized, "rectification", "整改", "工单")) {
      results.add(rectificationContext(question));
    }
    if (containsAny(normalized, "plan", "计划")) {
      results.add(planContext(question));
    }
    if (containsAny(normalized, "standard", "标准", "制度", "文档")) {
      results.add(standardContext(question));
    }
    if (containsAny(normalized, "alert", "预警", "待办", "overdue", "逾期")) {
      results.add(workbenchContext(question));
    }
    if (containsAny(normalized, "model", "\u6a21\u578b", "\u6a21\u578b\u5e93")) {
      results.add(modelConfigContext());
    }
    return results.stream()
        .filter(result -> result != null && result.summary() != null && !result.summary().isBlank())
        .toList();
  }

  public List<AiChatToolResultDto> collectContext(AiChatAgentPlanDto plan) {
    if (plan == null || plan.toolCalls() == null || plan.toolCalls().isEmpty()) {
      return List.of();
    }
    return plan.toolCalls().stream()
        .map(this::executeToolCall)
        .filter(result -> result != null && result.summary() != null && !result.summary().isBlank())
        .toList();
  }

  private AiChatToolResultDto executeToolCall(AiChatToolCallDto toolCall) {
    if (toolCall == null || toolCall.toolName() == null) {
      return null;
    }
    return switch (toolCall.toolName()) {
      case "ProjectQueryTool" -> projectContext(toolCall);
      case "RectificationQueryTool" -> rectificationContext(toolCall);
      case "PlanQueryTool" -> planContext(toolCall.keyword());
      case "StandardQueryTool" -> standardContext(toolCall.keyword());
      case "WorkbenchQueryTool" -> workbenchContext(toolCall.keyword());
      case "ModelConfigQueryTool" -> modelConfigContext();
      case "ProjectArchiveQueryTool" -> projectArchiveContext(toolCall.keyword());
      default -> null;
    };
  }

  private AiChatToolResultDto projectContext(String keyword) {
    PageResponse<ProjectDto> page = projectService.list(new ProjectListQuery(projectKeyword(keyword), null, null, null, null, null, 1L, 10L));
    List<ProjectDto> records = nullToList(page.getRecords());
    return new AiChatToolResultDto(
        "ProjectQueryTool",
        records.isEmpty() ? "当前权限下未找到相关项目。" : records.stream().map(this::projectSummary).reduce(this::join).orElse(""),
        records.stream()
            .map(project -> new AiChatCitationDto("project", project.id(), nonBlank(project.name(), project.code(), project.id()), "/project/detail/" + project.id()))
            .toList()
    );
  }

  private AiChatToolResultDto projectContext(AiChatToolCallDto toolCall) {
    if ("current".equals(toolCall.scope()) && trimToNull(toolCall.entityId()) != null) {
      ProjectDto project = projectService.get(toolCall.entityId());
      return new AiChatToolResultDto(
          "ProjectQueryTool",
          projectSummary(project),
          List.of(new AiChatCitationDto("project", project.id(), nonBlank(project.name(), project.code(), project.id()), "/project/detail/" + project.id()))
      );
    }
    return projectContext(toolCall.keyword());
  }

  private AiChatToolResultDto rectificationContext(String keyword) {
    PageResponse<RectificationDto> page = rectificationService.list(new RectificationListQuery(keyword, null, null, null, 1L, 10L));
    List<RectificationDto> records = nullToList(page.getRecords());
    return new AiChatToolResultDto(
        "RectificationQueryTool",
        records.isEmpty() ? "当前权限下未找到相关整改。" : records.stream().map(this::rectificationSummary).reduce(this::join).orElse(""),
        records.stream()
            .map(item -> new AiChatCitationDto("rectification", item.id(), nonBlank(item.title(), item.code(), item.id()), "/rectification/detail/" + item.id()))
            .toList()
    );
  }

  private AiChatToolResultDto rectificationContext(AiChatToolCallDto toolCall) {
    String projectId = "current_project".equals(toolCall.scope()) ? trimToNull(toolCall.entityId()) : null;
    PageResponse<RectificationDto> page = rectificationService.list(new RectificationListQuery(toolCall.keyword(), null, projectId, null, 1L, 10L));
    List<RectificationDto> records = nullToList(page.getRecords());
    return new AiChatToolResultDto(
        "RectificationQueryTool",
        records.isEmpty() ? "å½“å‰æƒé™ä¸‹æœªæ‰¾åˆ°ç›¸å…³æ•´æ”¹ã€‚" : records.stream().map(this::rectificationSummary).reduce(this::join).orElse(""),
        records.stream()
            .map(item -> new AiChatCitationDto("rectification", item.id(), nonBlank(item.title(), item.code(), item.id()), "/rectification/detail/" + item.id()))
            .toList()
    );
  }

  private AiChatToolResultDto planContext(String keyword) {
    PageResponse<PlanDto> page = planService.list(new PlanListQuery(keyword, null, null, 1L, 10L));
    List<PlanDto> records = nullToList(page.getRecords());
    return new AiChatToolResultDto(
        "PlanQueryTool",
        records.isEmpty() ? "当前权限下未找到相关计划。" : records.stream().map(this::planSummary).reduce(this::join).orElse(""),
        records.stream()
            .map(item -> new AiChatCitationDto("plan", item.id(), nonBlank(item.name(), item.code(), item.id()), "/plan/detail/" + item.id()))
            .toList()
    );
  }

  private AiChatToolResultDto standardContext(String keyword) {
    PageResponse<StandardDto> page = standardService.list(new StandardListQuery(keyword, null, null, 1L, 10L));
    List<StandardDto> records = nullToList(page.getRecords());
    return new AiChatToolResultDto(
        "StandardQueryTool",
        records.isEmpty() ? "当前权限下未找到相关标准文档。" : records.stream().map(this::standardSummary).reduce(this::join).orElse(""),
        records.stream()
            .map(item -> new AiChatCitationDto("standard", item.id(), nonBlank(item.title(), item.standardCode(), item.id()), "/resource/standards?id=" + item.id()))
            .toList()
    );
  }

  private AiChatToolResultDto workbenchContext(String keyword) {
    PageResponse<AlertEventDto> page = alertService.list(new AlertQuery(keyword, null, 1L, 10L));
    List<AlertEventDto> records = nullToList(page.getRecords());
    return new AiChatToolResultDto(
        "WorkbenchQueryTool",
        records.isEmpty() ? "当前权限下未找到相关待办或预警。" : records.stream().map(this::alertSummary).reduce(this::join).orElse(""),
        records.stream()
            .map(item -> new AiChatCitationDto("alert", item.id(), item.title(), "/workbench/alerts"))
            .toList()
    );
  }

  private AiChatToolResultDto modelConfigContext() {
    PageResponse<AiModelConfigDto> page = aiModelConfigService.list(null, null, null, 1L, 10L);
    List<AiModelConfigDto> records = nullToList(page.getRecords());
    return new AiChatToolResultDto(
        "ModelConfigQueryTool",
        records.isEmpty() ? "No AI model configs found for the current tenant." : records.stream().map(this::modelConfigSummary).reduce(this::join).orElse(""),
        records.stream()
            .map(item -> new AiChatCitationDto("ai_model", item.id(), nonBlank(item.name(), item.modelName(), item.id()), "/smart/models"))
            .toList()
    );
  }

  private AiChatToolResultDto projectArchiveContext(String keyword) {
    PageResponse<ProjectArchiveDto> page = projectArchiveService.list(keyword, null, 1L, 10L);
    List<ProjectArchiveDto> records = nullToList(page.getRecords());
    return new AiChatToolResultDto(
        "ProjectArchiveQueryTool",
        records.isEmpty() ? "当前权限下未找到相关项目档案。" : records.stream().map(this::projectArchiveSummary).reduce(this::join).orElse(""),
        records.stream()
            .map(item -> new AiChatCitationDto("project_archive", item.id(), nonBlank(item.projectName(), item.projectCode(), item.id()), "/resource/archives"))
            .toList()
    );
  }

  private String projectSummary(ProjectDto project) {
    return "项目：" + nonBlank(project.name(), project.code(), project.id())
        + "，状态：" + nonBlank(project.status(), null, "未知")
        + "，负责人：" + nonBlank(project.leaderName(), project.leaderId(), "未设置")
        + "，进度：" + (project.progress() == null ? "未知" : project.progress() + "%");
  }

  private String rectificationSummary(RectificationDto item) {
    return "整改：" + nonBlank(item.title(), item.code(), item.id())
        + "，状态：" + nonBlank(item.status(), null, "未知")
        + "，负责人：" + nonBlank(item.assigneeName(), item.assigneeId(), "未设置")
        + "，截止：" + nonBlank(item.deadline(), null, "未设置");
  }

  private String planSummary(PlanDto item) {
    return "计划：" + nonBlank(item.name(), item.code(), item.id())
        + "，年度：" + (item.year() == null ? "未知" : item.year())
        + "，状态：" + nonBlank(item.status(), null, "未知");
  }

  private String standardSummary(StandardDto item) {
    return "标准文档：" + nonBlank(item.title(), item.standardCode(), item.id())
        + "，分类：" + nonBlank(item.category(), null, "未分类")
        + "，状态：" + nonBlank(item.status(), null, "未知");
  }

  private String alertSummary(AlertEventDto item) {
    return "预警：" + nonBlank(item.title(), item.source(), item.id())
        + "，级别：" + nonBlank(item.level(), null, "未知")
        + "，内容：" + nonBlank(item.content(), null, "");
  }

  private String modelConfigSummary(AiModelConfigDto item) {
    return "AI model: " + nonBlank(item.name(), item.modelName(), item.id())
        + ", provider: " + nonBlank(item.provider(), item.providerType(), "unknown")
        + ", modelName: " + nonBlank(item.modelName(), null, "unknown")
        + ", status: " + nonBlank(item.status(), null, "unknown")
        + ", default=" + item.defaultModel()
        + ", apiKeyConfigured=" + item.apiKeyConfigured();
  }

  private String projectArchiveSummary(ProjectArchiveDto item) {
    return "项目档案：" + nonBlank(item.projectName(), item.projectCode(), item.id())
        + "，项目编号：" + nonBlank(item.projectCode(), null, "未知")
        + "，归档时间：" + nonBlank(item.archiveDate(), null, "未知")
        + "，归档人：" + nonBlank(item.archivedByName(), item.archivedBy(), "未知")
        + "，检查项：" + countText(item.taskCount(), "个检查项")
        + "，OMS工单：" + countText(item.workOrderCount(), "个工单")
        + "，整改单：" + countText(item.rectificationCount(), "个整改单")
        + "，档案文件：" + countText(item.documentCount(), "个档案文件")
        + archiveDocumentNames(item.documents());
  }

  private String archiveDocumentNames(List<ProjectArchiveDocumentDto> documents) {
    List<ProjectArchiveDocumentDto> safeDocuments = nullToList(documents);
    if (safeDocuments.isEmpty()) {
      return "";
    }
    String names = safeDocuments.stream()
        .map(ProjectArchiveDocumentDto::name)
        .filter(name -> name != null && !name.isBlank())
        .limit(5)
        .reduce(this::joinNames)
        .orElse("");
    return names.isBlank() ? "" : "，文件：" + names;
  }

  private String countText(Integer count, String unit) {
    return (count == null ? 0 : count) + " " + unit;
  }

  private String joinNames(String left, String right) {
    return left + "、" + right;
  }

  private String join(String left, String right) {
    return left + "\n" + right;
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

  private String projectKeyword(String question) {
    String normalized = normalize(question);
    if (containsAny(
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
    )) {
      return null;
    }
    return question;
  }

  private String nonBlank(String first, String second, String fallback) {
    String normalizedFirst = trimToNull(first);
    if (normalizedFirst != null) {
      return normalizedFirst;
    }
    String normalizedSecond = trimToNull(second);
    return normalizedSecond == null ? fallback : normalizedSecond;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private <T> List<T> nullToList(List<T> values) {
    return values == null ? List.of() : values;
  }
}
