package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iris.back.business.ai.model.dto.AiModelConfigDto;
import com.iris.back.business.ai.service.AiModelConfigService;
import com.iris.back.business.ai.service.AiChatContextService;
import com.iris.back.business.plan.model.dto.PlanDto;
import com.iris.back.business.plan.service.PlanService;
import com.iris.back.business.project.model.dto.AlertEventDto;
import com.iris.back.business.project.model.dto.ProjectArchiveDto;
import com.iris.back.business.project.model.dto.ProjectDto;
import com.iris.back.business.project.model.dto.RectificationDto;
import com.iris.back.business.project.model.request.ProjectListQuery;
import com.iris.back.business.project.service.AlertService;
import com.iris.back.business.project.service.ProjectArchiveService;
import com.iris.back.business.project.service.ProjectService;
import com.iris.back.business.project.service.RectificationService;
import com.iris.back.business.standard.model.dto.StandardDto;
import com.iris.back.business.standard.service.StandardService;
import com.iris.back.common.model.PageResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiChatContextServiceTests {

  @Mock
  private ProjectService projectService;

  @Mock
  private RectificationService rectificationService;

  @Mock
  private PlanService planService;

  @Mock
  private StandardService standardService;

  @Mock
  private AlertService alertService;

  @Mock
  private AiModelConfigService aiModelConfigService;

  @Mock
  private ProjectArchiveService projectArchiveService;

  private AiChatContextService aiChatContextService;

  @BeforeEach
  void setUp() {
    aiChatContextService = new AiChatContextService(
        projectService,
        rectificationService,
        planService,
        standardService,
        alertService,
        aiModelConfigService,
        projectArchiveService
    );
  }

  @Test
  void collectContextRoutesProjectQuestionsToProjectService() {
    when(projectService.list(any())).thenReturn(PageResponse.of(1, 1, 10, List.of(project())));

    var results = aiChatContextService.collectContext("总结当前项目风险", null);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().toolName()).isEqualTo("ProjectQueryTool");
    assertThat(results.getFirst().summary()).contains("内控巡检");
    assertThat(results.getFirst().citations().getFirst().path()).isEqualTo("/project/detail/9001");
    verify(projectService).list(any());
  }

  @Test
  void collectContextDoesNotUseGeneralProjectInventoryQuestionAsKeyword() {
    when(projectService.list(any())).thenReturn(PageResponse.of(1, 1, 10, List.of(project())));

    aiChatContextService.collectContext("which projects can I see", null);

    ArgumentCaptor<ProjectListQuery> queryCaptor = ArgumentCaptor.forClass(ProjectListQuery.class);
    verify(projectService).list(queryCaptor.capture());
    assertThat(queryCaptor.getValue().keyword()).isNull();
  }

  @Test
  void collectContextRoutesRectificationAndAlertQuestions() {
    when(rectificationService.list(any())).thenReturn(PageResponse.of(1, 1, 10, List.of(rectification())));
    when(alertService.list(any())).thenReturn(PageResponse.of(1, 1, 10, List.of(alert())));

    var results = aiChatContextService.collectContext("我有哪些整改待办和逾期预警", null);

    assertThat(results).extracting("toolName")
        .containsExactly("RectificationQueryTool", "WorkbenchQueryTool");
    assertThat(results.get(0).citations().getFirst().path()).isEqualTo("/rectification/detail/8001");
    verify(rectificationService).list(any());
    verify(alertService).list(any());
  }

  @Test
  void collectContextRoutesPlanAndStandardQuestions() {
    when(planService.list(any())).thenReturn(PageResponse.of(1, 1, 10, List.of(plan())));
    when(standardService.list(any())).thenReturn(PageResponse.of(1, 1, 10, List.of(standard())));

    var results = aiChatContextService.collectContext("计划关联的标准文档有哪些", null);

    assertThat(results).extracting("toolName")
        .containsExactly("PlanQueryTool", "StandardQueryTool");
    assertThat(results.get(1).citations().getFirst().path()).isEqualTo("/resource/standards?id=7001");
    verify(planService).list(any());
    verify(standardService).list(any());
  }

  @Test
  void collectContextRoutesModelLibraryQuestionsToModelConfigService() {
    when(aiModelConfigService.list(null, null, null, 1L, 10L))
        .thenReturn(PageResponse.of(2, 1, 10, List.of(defaultModel(), secondaryModel())));

    var results = aiChatContextService.collectContext("which models are in the model library", null);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().toolName()).isEqualTo("ModelConfigQueryTool");
    assertThat(results.getFirst().summary()).contains("Default GPT", "qwen-plus", "default=true");
    assertThat(results.getFirst().citations()).hasSize(2);
    assertThat(results.getFirst().citations().getFirst().path()).isEqualTo("/smart/models");
    verify(aiModelConfigService).list(null, null, null, 1L, 10L);
  }

  @Test
  void collectContextRoutesArchiveQuestionsToProjectArchiveService() {
    when(projectArchiveService.list(null, null, 1L, 10L))
        .thenReturn(PageResponse.of(1, 1, 10, List.of(projectArchive())));

    var plan = new com.iris.back.business.ai.model.dto.AiChatAgentPlanDto(
        "summarize_project_archives",
        "Use project archive context before answering.",
        true,
        List.of(new com.iris.back.business.ai.model.dto.AiChatToolCallDto(
            "ProjectArchiveQueryTool",
            null,
            "archive",
            null,
            null,
            "List project archives."
        ))
    );

    var results = aiChatContextService.collectContext(plan);

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().toolName()).isEqualTo("ProjectArchiveQueryTool");
    assertThat(results.getFirst().summary()).contains("Finance project", "1 个检查项", "2 个档案文件");
    assertThat(results.getFirst().citations().getFirst().path()).isEqualTo("/resource/archives");
    verify(projectArchiveService).list(null, null, 1L, 10L);
  }

  private ProjectDto project() {
    return new ProjectDto(
        "9001",
        "P-9001",
        "内控巡检",
        "manual",
        "100",
        "年度计划",
        "检查关键权限",
        "2026-05-01",
        "2026-05-31",
        "in_progress",
        List.of(),
        List.of(),
        "2001",
        "张三",
        List.of(),
        "none",
        null,
        null,
        null,
        3,
        1,
        1,
        40,
        List.of(),
        List.of(),
        List.of(),
        null,
        null
    );
  }

  private RectificationDto rectification() {
    return new RectificationDto(
        "8001",
        "R-8001",
        "manual",
        null,
        null,
        null,
        "9001",
        "内控巡检",
        null,
        null,
        null,
        "整改权限配置",
        "补充审批链路",
        "2001",
        "张三",
        null,
        null,
        "in_progress",
        null,
        "2026-05-20",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(),
        null,
        List.of(),
        null,
        null
    );
  }

  private PlanDto plan() {
    return new PlanDto(
        "6001",
        "PLAN-6001",
        "年度内控计划",
        "annual",
        2026,
        "全年",
        "approved",
        "覆盖核心系统",
        "3001",
        List.of(),
        List.of(),
        null,
        List.of(),
        null,
        null,
        null,
        null
    );
  }

  private StandardDto standard() {
    return new StandardDto(
        "7001",
        "std-group-1",
        "STD-7001",
        "权限管理制度",
        "制度",
        "v1",
        "2026-01-01",
        "active",
        List.of(),
        "账号权限管理要求",
        null,
        null,
        1,
        null,
        "scope",
        "3001",
        List.of(),
        null,
        "管理员"
    );
  }

  private AlertEventDto alert() {
    return new AlertEventDto(
        "alert-1",
        "整改管理",
        "warning",
        "整改单已逾期",
        "整改权限配置 已超过截止时间",
        "2026-05-08 10:00:00",
        false
    );
  }

  private AiModelConfigDto defaultModel() {
    return new AiModelConfigDto(
        "model-1",
        "Default GPT",
        "llm",
        "openai_compatible",
        "OpenAI Compatible",
        "https://api.example.com",
        "gpt-4o-mini",
        true,
        "online",
        true,
        30,
        0.2,
        3000,
        null,
        "2026-05-08 10:00:00",
        "2026-05-08 10:00:00"
    );
  }

  private AiModelConfigDto secondaryModel() {
    return new AiModelConfigDto(
        "model-2",
        "Qwen",
        "llm",
        "openai_compatible",
        "OpenAI Compatible",
        "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "qwen-plus",
        true,
        "offline",
        false,
        30,
        0.2,
        3000,
        null,
        "2026-05-08 10:00:00",
        "2026-05-08 10:00:00"
    );
  }

  private ProjectArchiveDto projectArchive() {
    return new ProjectArchiveDto(
        "9101",
        "7001",
        "PRJ-2026-001",
        "Finance project",
        "2026-05-07 15:30:00",
        "2001",
        "Platform Administrator",
        "active",
        1,
        1,
        0,
        2,
        "v1",
        "{\"workOrders\":[]}",
        List.of(),
        "2026-05-07 15:30:00",
        "2026-05-07 15:30:00"
    );
  }
}
