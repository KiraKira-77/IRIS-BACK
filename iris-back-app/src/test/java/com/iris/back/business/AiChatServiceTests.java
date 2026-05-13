package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iris.back.business.ai.mapper.AiModelConfigMapper;
import com.iris.back.business.ai.model.dto.AiChatAgentPlanDto;
import com.iris.back.business.ai.model.dto.AiChatCitationDto;
import com.iris.back.business.ai.model.dto.AiChatToolCallDto;
import com.iris.back.business.ai.model.dto.AiChatToolResultDto;
import com.iris.back.business.ai.model.entity.AiModelConfigEntity;
import com.iris.back.business.ai.model.request.AiChatMessageRequest;
import com.iris.back.business.ai.model.request.AiChatPageContextRequest;
import com.iris.back.business.ai.service.AiChatAgentPlanner;
import com.iris.back.business.ai.service.AiChatClient;
import com.iris.back.business.ai.service.AiChatContextService;
import com.iris.back.business.ai.service.AiChatService;
import com.iris.back.business.ai.service.AiChatTraceService;
import com.iris.back.business.ai.service.AiModelConfigService;
import com.iris.back.business.ai.service.LlmConnectivityClient;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTests {

  @Mock
  private AiModelConfigMapper aiModelConfigMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  @Mock
  private AiChatClient aiChatClient;

  @Mock
  private AiChatContextService aiChatContextService;

  @Mock
  private AiChatAgentPlanner aiChatAgentPlanner;

  @Mock
  private AiChatTraceService aiChatTraceService;

  @Mock
  private LlmConnectivityClient connectivityClient;

  private AiChatService aiChatService;

  @BeforeEach
  void setUp() {
    aiChatService = new AiChatService(
        aiModelConfigMapper,
        currentUserContext,
        aiChatClient,
        aiChatContextService,
        aiChatAgentPlanner,
        aiChatTraceService,
        "unit-test-secret"
    );
  }

  @Test
  void sendMessageReturnsSetupMessageWhenNoDefaultModelConfigured() {
    mockCurrentUser();
    mockTrace();
    when(aiModelConfigMapper.selectList(any())).thenReturn(List.of());

    var response = aiChatService.sendMessage(new AiChatMessageRequest(
        "session-1",
        "总结我负责的整改",
        null
    ));

    assertThat(response.status()).isEqualTo("model_unconfigured");
    assertThat(response.traceId()).isEqualTo("trace-1");
    assertThat(response.content()).contains("默认 AI 模型");
  }

  @Test
  void sendMessageReturnsOfflineMessageWhenDefaultModelIsOffline() {
    mockCurrentUser();
    mockTrace();
    AiModelConfigEntity entity = model("offline");
    when(aiModelConfigMapper.selectList(any())).thenReturn(List.of(entity));

    var response = aiChatService.sendMessage(new AiChatMessageRequest(
        "session-1",
        "查询项目风险",
        null
    ));

    assertThat(response.status()).isEqualTo("model_offline");
    assertThat(response.traceId()).isEqualTo("trace-1");
    assertThat(response.content()).contains("未启用");
  }

  @Test
  void sendMessageCallsChatClientWithDecryptedDefaultModelAndToolContext() {
    mockCurrentUser();
    mockTrace();
    AiModelConfigEntity entity = encryptedDefaultModel();
    when(aiModelConfigMapper.selectList(any())).thenReturn(List.of(entity));
    when(aiChatAgentPlanner.plan(any(), any())).thenReturn(new AiChatAgentPlanDto(
        "business_data_question",
        "Use project context before answering.",
        true,
        List.of(new AiChatToolCallDto("ProjectQueryTool", "项目", "keyword", null, null, "Find matching projects."))
    ));
    when(aiChatContextService.collectContext(any(AiChatAgentPlanDto.class))).thenReturn(List.of(
        new AiChatToolResultDto(
            "ProjectQueryTool",
            "项目：内控巡检，状态：in_progress",
            List.of(new AiChatCitationDto("project", "9001", "内控巡检", "/project/detail/9001"))
        )
    ));
    when(aiChatClient.chat(any())).thenReturn(new AiChatClient.ChatResult(
        "当前权限下找到 1 个进行中项目。",
        120L
    ));

    var response = aiChatService.sendMessage(new AiChatMessageRequest(
        "session-1",
        "总结当前项目",
        new AiChatPageContextRequest("/project/detail/9001", "project", "9001")
    ));

    assertThat(response.status()).isEqualTo("ok");
    assertThat(response.traceId()).isEqualTo("trace-1");
    assertThat(response.content()).isEqualTo("当前权限下找到 1 个进行中项目。");
    assertThat(response.citations()).hasSize(1);
    ArgumentCaptor<AiChatClient.ChatCommand> commandCaptor =
        ArgumentCaptor.forClass(AiChatClient.ChatCommand.class);
    verify(aiChatClient).chat(commandCaptor.capture());
    assertThat(commandCaptor.getValue().apiKey()).isEqualTo("sk-chat-secret");
    assertThat(commandCaptor.getValue().baseUrl()).isEqualTo("https://ai.example.com/v1");
    assertThat(commandCaptor.getValue().modelName()).isEqualTo("deepseek-chat");
    assertThat(commandCaptor.getValue().messages()).anySatisfy(message ->
        assertThat(message.content()).contains("项目：内控巡检"));
    verify(aiChatAgentPlanner).plan(any(), any());
    verify(aiChatContextService).collectContext(any(AiChatAgentPlanDto.class));
    verify(aiChatTraceService).recordEvent(
        any(),
        org.mockito.ArgumentMatchers.eq("agent"),
        org.mockito.ArgumentMatchers.eq("plan"),
        org.mockito.ArgumentMatchers.eq("ok"),
        any(),
        org.mockito.ArgumentMatchers.isNull()
    );
    verify(aiChatTraceService).recordEvent(
        any(),
        org.mockito.ArgumentMatchers.eq("agent"),
        org.mockito.ArgumentMatchers.eq("validate_tool_results"),
        org.mockito.ArgumentMatchers.eq("ok"),
        any(),
        org.mockito.ArgumentMatchers.isNull()
    );
  }

  @Test
  void sendMessageResolvesProjectCodeFromSessionBeforePlanningCurrentProjectReference() {
    mockCurrentUser();
    mockTrace();
    AiModelConfigEntity entity = encryptedDefaultModel();
    when(aiModelConfigMapper.selectList(any())).thenReturn(List.of(entity));
    when(aiChatTraceService.findLastProjectCodeInSession(any(), any())).thenReturn("PRJ-2049683439946584066");
    when(aiChatAgentPlanner.plan(any(), any())).thenReturn(new AiChatAgentPlanDto(
        "analyze_referenced_project",
        "Use referenced project context.",
        true,
        List.of(new AiChatToolCallDto(
            "ProjectArchiveQueryTool",
            "PRJ-2049683439946584066",
            "archive",
            "project_code",
            "PRJ-2049683439946584066",
            "Find archived project records by code."
        ))
    ));
    when(aiChatContextService.collectContext(any(AiChatAgentPlanDto.class))).thenReturn(List.of(
        new AiChatToolResultDto(
            "ProjectArchiveQueryTool",
            "å½’æ¡£é¡¹ç›®ï¼šPRJ-2049683439946584066",
            List.of(new AiChatCitationDto("project_archive", "PRJ-2049683439946584066", "å½’æ¡£é¡¹ç›®", "/project/archive"))
        )
    ));
    when(aiChatClient.chat(any())).thenReturn(new AiChatClient.ChatResult(
        "è¿™ä¸ªé¡¹ç›®çš„å½’æ¡£ä¿¡æ¯å¦‚ä¸‹ã€‚",
        120L
    ));

    aiChatService.sendMessage(new AiChatMessageRequest(
        "session-1",
        "\u4f60\u80fd\u603b\u7ed3\u4e00\u4e0b\u8fd9\u4e2a\u9879\u76ee\u5b8c\u6210\u7684\u5185\u5bb9\u5417",
        new AiChatPageContextRequest("/project/list", null, null)
    ));

    ArgumentCaptor<AiChatPageContextRequest> pageContextCaptor =
        ArgumentCaptor.forClass(AiChatPageContextRequest.class);
    verify(aiChatAgentPlanner).plan(any(), pageContextCaptor.capture());
    assertThat(pageContextCaptor.getValue().routePath()).isEqualTo("/project/list");
    assertThat(pageContextCaptor.getValue().entityType()).isEqualTo("project_code");
    assertThat(pageContextCaptor.getValue().entityId()).isEqualTo("PRJ-2049683439946584066");
    verify(aiChatTraceService).recordEvent(
        any(),
        org.mockito.ArgumentMatchers.eq("agent"),
        org.mockito.ArgumentMatchers.eq("resolve_session_context"),
        org.mockito.ArgumentMatchers.eq("ok"),
        any(),
        org.mockito.ArgumentMatchers.isNull()
    );
  }

  @Test
  void sendMessageAddsEmptyResultReviewInstructionBeforeModelAnswer() {
    mockCurrentUser();
    mockTrace();
    AiModelConfigEntity entity = encryptedDefaultModel();
    when(aiModelConfigMapper.selectList(any())).thenReturn(List.of(entity));
    when(aiChatAgentPlanner.plan(any(), any())).thenReturn(new AiChatAgentPlanDto(
        "list_visible_projects",
        "List visible projects.",
        true,
        List.of(new AiChatToolCallDto("ProjectQueryTool", null, "visible", null, null, "List visible projects."))
    ));
    when(aiChatContextService.collectContext(any(AiChatAgentPlanDto.class))).thenReturn(List.of(
        new AiChatToolResultDto(
            "ProjectQueryTool",
            "当前权限下未找到相关项目。",
            List.of()
        )
    ));
    when(aiChatClient.chat(any())).thenReturn(new AiChatClient.ChatResult(
        "当前工具没有检索到项目。",
        120L
    ));

    aiChatService.sendMessage(new AiChatMessageRequest(
        "session-1",
        "我负责的项目有哪些",
        null
    ));

    ArgumentCaptor<AiChatClient.ChatCommand> commandCaptor =
        ArgumentCaptor.forClass(AiChatClient.ChatCommand.class);
    verify(aiChatClient).chat(commandCaptor.capture());
    assertThat(commandCaptor.getValue().messages()).anySatisfy(message ->
        assertThat(message.content()).contains("不要直接判断为没有权限", "空结果复核"));
    assertThat(commandCaptor.getValue().messages()).anySatisfy(message ->
        assertThat(message.content()).contains("当前登录用户", "Platform Administrator", "不要回答“无法识别你的姓名”"));
    assertThat(commandCaptor.getValue().messages()).anySatisfy(message ->
        assertThat(message.content()).contains("项目工具返回的是当前账号在权限范围内可见的项目"));
    verify(aiChatTraceService).recordEvent(
        any(),
        org.mockito.ArgumentMatchers.eq("agent"),
        org.mockito.ArgumentMatchers.eq("validate_tool_results"),
        org.mockito.ArgumentMatchers.eq("needs_review"),
        any(),
        org.mockito.ArgumentMatchers.isNull()
    );
  }

  private AiModelConfigEntity encryptedDefaultModel() {
    AiModelConfigService configService = new AiModelConfigService(
        aiModelConfigMapper,
        currentUserContext,
        connectivityClient,
        "unit-test-secret"
    );
    configService.create(new com.iris.back.business.ai.model.request.AiModelConfigUpsertRequest(
        "默认模型",
        "openai_compatible",
        "https://ai.example.com/v1",
        "deepseek-chat",
        "sk-chat-secret",
        "online",
        true,
        20,
        0.1,
        2000,
        null
    ));
    ArgumentCaptor<AiModelConfigEntity> captor = ArgumentCaptor.forClass(AiModelConfigEntity.class);
    verify(aiModelConfigMapper).insert(captor.capture());
    AiModelConfigEntity entity = captor.getValue();
    entity.setId(9001L);
    return entity;
  }

  private AiModelConfigEntity model(String status) {
    AiModelConfigEntity entity = new AiModelConfigEntity();
    entity.setId(9001L);
    entity.setTenantId(1001L);
    entity.setDisplayName("默认模型");
    entity.setProviderType("openai_compatible");
    entity.setProviderName("OpenAI Compatible");
    entity.setBaseUrl("https://ai.example.com/v1");
    entity.setModelName("deepseek-chat");
    entity.setApiKeyCipher("cipher");
    entity.setStatus(status);
    entity.setDefaultModel(1);
    entity.setTimeoutSeconds(20);
    entity.setTemperature(0.1);
    entity.setMaxTokens(2000);
    return entity;
  }

  private void mockTrace() {
    when(aiChatTraceService.start(any(), any())).thenReturn(
        new AiChatTraceService.TraceContext("trace-1", 10001L)
    );
  }

  private void mockCurrentUser() {
    when(currentUserContext.requireCurrentUser()).thenReturn(new CurrentUserPrincipal(
        "token",
        2001L,
        1001L,
        "admin",
        "Platform Administrator",
        "IRIS",
        List.of("PLATFORM_ADMIN")
    ));
  }
}
