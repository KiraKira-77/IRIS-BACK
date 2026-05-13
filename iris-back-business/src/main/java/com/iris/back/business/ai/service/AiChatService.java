package com.iris.back.business.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iris.back.business.ai.mapper.AiModelConfigMapper;
import com.iris.back.business.ai.model.dto.AiChatAgentPlanDto;
import com.iris.back.business.ai.model.dto.AiChatCitationDto;
import com.iris.back.business.ai.model.dto.AiChatMessageDto;
import com.iris.back.business.ai.model.dto.AiChatSessionDto;
import com.iris.back.business.ai.model.dto.AiChatToolResultDto;
import com.iris.back.business.ai.model.entity.AiModelConfigEntity;
import com.iris.back.business.ai.model.request.AiChatMessageRequest;
import com.iris.back.business.ai.model.request.AiChatPageContextRequest;
import com.iris.back.common.util.DateTimeFormatters;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

  private final AiModelConfigMapper aiModelConfigMapper;
  private final CurrentUserContext currentUserContext;
  private final AiChatClient aiChatClient;
  private final AiChatContextService aiChatContextService;
  private final AiChatAgentPlanner aiChatAgentPlanner;
  private final AiChatTraceService aiChatTraceService;
  private final String secretKey;

  public AiChatService(
      AiModelConfigMapper aiModelConfigMapper,
      CurrentUserContext currentUserContext,
      AiChatClient aiChatClient,
      AiChatContextService aiChatContextService,
      AiChatAgentPlanner aiChatAgentPlanner,
      AiChatTraceService aiChatTraceService,
      @Value("${iris.ai.secret-key:${IRIS_AI_SECRET_KEY:iris-dev-ai-secret}}") String secretKey
  ) {
    this.aiModelConfigMapper = aiModelConfigMapper;
    this.currentUserContext = currentUserContext;
    this.aiChatClient = aiChatClient;
    this.aiChatContextService = aiChatContextService;
    this.aiChatAgentPlanner = aiChatAgentPlanner;
    this.aiChatTraceService = aiChatTraceService;
    this.secretKey = secretKey;
  }

  public AiChatSessionDto createSession() {
    return new AiChatSessionDto(
        UUID.randomUUID().toString(),
        "AI 对话",
        DateTimeFormatters.formatDateTime(LocalDateTime.now())
    );
  }

  public AiChatMessageDto sendMessage(AiChatMessageRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    AiChatTraceService.TraceContext trace = aiChatTraceService.start(principal, request);
    List<AiModelConfigEntity> defaultModels = nullToList(aiModelConfigMapper.selectList(
        new LambdaQueryWrapper<AiModelConfigEntity>()
            .eq(AiModelConfigEntity::getTenantId, principal.tenantId())
            .eq(AiModelConfigEntity::getDefaultModel, 1)
    )).stream()
        .filter(model -> !Objects.equals(model.getDeleted(), 1))
        .toList();
    aiChatTraceService.recordEvent(
        trace,
        "model",
        "resolve_default_model",
        "ok",
        defaultModels.stream()
            .map(model -> new ModelTraceDetail(model.getId(), model.getProviderType(), model.getModelName(), model.getStatus(), model.getDefaultModel()))
            .toList(),
        null
    );

    if (defaultModels.isEmpty()) {
      AiChatMessageDto response = assistant(
          trace.traceId(),
          request.sessionId(),
          "model_unconfigured",
          "当前未配置默认 AI 模型，请管理员在模型库设置默认模型。",
          List.of(),
          List.of(),
          null
      );
      aiChatTraceService.complete(trace, response, null, null, null, List.of(), "NO_DEFAULT_MODEL");
      return response;
    }
    if (defaultModels.size() > 1) {
      AiChatMessageDto response = assistant(
          trace.traceId(),
          request.sessionId(),
          "model_config_invalid",
          "默认 AI 模型配置异常，请管理员重新设置默认模型。",
          List.of(),
          List.of(),
          null
      );
      aiChatTraceService.complete(trace, response, null, null, null, List.of(), "MULTIPLE_DEFAULT_MODELS");
      return response;
    }

    AiModelConfigEntity model = defaultModels.getFirst();
    if (!"online".equals(model.getStatus())) {
      AiChatMessageDto response = assistant(
          trace.traceId(),
          request.sessionId(),
          "model_offline",
          "默认 AI 模型当前未启用，请管理员检查模型库配置。",
          List.of(),
          List.of(),
          null
      );
      aiChatTraceService.complete(trace, response, model.getId(), model.getProviderType(), model.getModelName(), List.of(), "DEFAULT_MODEL_OFFLINE");
      return response;
    }

    AiChatPageContextRequest effectivePageContext = resolveEffectivePageContext(principal, request, trace);
    AiChatAgentPlanDto plan = aiChatAgentPlanner.plan(request.message(), effectivePageContext);
    aiChatTraceService.recordEvent(trace, "agent", "plan", "ok", plan, null);
    List<AiChatToolResultDto> toolResults = aiChatContextService.collectContext(plan);
    aiChatTraceService.recordEvent(trace, "tool_context", "collect_context", "ok", toolResults, null);
    ToolValidationResult validation = validateToolResults(plan, toolResults);
    aiChatTraceService.recordEvent(trace, "agent", "validate_tool_results", validation.status(), validation.detail(), null);
    AiChatClient.ChatResult result = aiChatClient.chat(new AiChatClient.ChatCommand(
        model.getProviderType(),
        model.getBaseUrl(),
        model.getModelName(),
        decryptApiKey(model.getApiKeyCipher()),
        model.getTimeoutSeconds(),
        model.getTemperature(),
        model.getMaxTokens(),
        buildMessages(principal, request.message(), plan, toolResults, validation)
    ));
    aiChatTraceService.recordEvent(trace, "model", "chat_completion", "ok", new ModelResponseTraceDetail(
        model.getId(),
        model.getProviderType(),
        model.getModelName(),
        result.latencyMs()
    ), result.latencyMs());
    List<AiChatCitationDto> citations = toolResults.stream()
        .flatMap(toolResult -> nullToList(toolResult.citations()).stream())
        .toList();
    AiChatMessageDto response = assistant(trace.traceId(), request.sessionId(), "ok", result.content(), citations, toolResults, result.latencyMs());
    aiChatTraceService.complete(trace, response, model.getId(), model.getProviderType(), model.getModelName(), toolResults, null);
    return response;
  }

  private ToolValidationResult validateToolResults(AiChatAgentPlanDto plan, List<AiChatToolResultDto> toolResults) {
    List<AiChatToolResultDto> safeToolResults = nullToList(toolResults);
    long emptyCitationResultCount = safeToolResults.stream()
        .filter(toolResult -> nullToList(toolResult.citations()).isEmpty())
        .count();
    boolean hasBlankSummary = safeToolResults.stream()
        .anyMatch(toolResult -> toolResult.summary() == null || toolResult.summary().isBlank());
    boolean hasEmptyResult = safeToolResults.stream().anyMatch(this::isEmptyToolResult);
    ToolValidationTraceDetail detail = new ToolValidationTraceDetail(
        plan.intent(),
        nullToList(plan.toolCalls()).size(),
        safeToolResults.size(),
        emptyCitationResultCount,
        hasBlankSummary,
        hasEmptyResult,
        hasEmptyResult ? "EMPTY_RESULT_REVIEW_REQUIRED" : null
    );
    String instruction = hasEmptyResult
        ? "空结果复核：工具返回空结果时，不要直接判断为没有权限；只能说明当前工具未检索到数据，并列出已使用工具、可能原因和下一步。"
        : null;
    return new ToolValidationResult(hasEmptyResult ? "needs_review" : "ok", detail, instruction);
  }

  private AiChatPageContextRequest resolveEffectivePageContext(
      CurrentUserPrincipal principal,
      AiChatMessageRequest request,
      AiChatTraceService.TraceContext trace
  ) {
    AiChatPageContextRequest pageContext = request.pageContext();
    if (!needsSessionProjectContext(request.message(), pageContext)) {
      aiChatTraceService.recordEvent(trace, "agent", "resolve_session_context", "skipped", new SessionContextTraceDetail(null, pageContext), null);
      return pageContext;
    }
    String projectCode = aiChatTraceService.findLastProjectCodeInSession(principal, request.sessionId());
    if (projectCode == null) {
      aiChatTraceService.recordEvent(trace, "agent", "resolve_session_context", "not_found", new SessionContextTraceDetail(null, pageContext), null);
      return pageContext;
    }
    AiChatPageContextRequest resolved = new AiChatPageContextRequest(
        pageContext == null ? null : pageContext.routePath(),
        "project_code",
        projectCode
    );
    aiChatTraceService.recordEvent(trace, "agent", "resolve_session_context", "ok", new SessionContextTraceDetail(projectCode, resolved), null);
    return resolved;
  }

  private boolean needsSessionProjectContext(String question, AiChatPageContextRequest pageContext) {
    if (pageContext != null && pageContext.entityId() != null && !pageContext.entityId().isBlank()) {
      return false;
    }
    String normalized = question == null ? "" : question.toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("\u8fd9\u4e2a\u9879\u76ee")
        || normalized.contains("\u5f53\u524d\u9879\u76ee")
        || normalized.contains("\u4e0a\u9762")
        || normalized.contains("\u521a\u624d")
        || normalized.contains("this project")
        || normalized.contains("current project");
  }

  private boolean isEmptyToolResult(AiChatToolResultDto toolResult) {
    if (toolResult == null) {
      return true;
    }
    String summary = toolResult.summary();
    String normalized = summary == null ? "" : summary.toLowerCase();
    return nullToList(toolResult.citations()).isEmpty()
        && !normalized.isBlank()
        && (normalized.contains("未找到")
            || normalized.contains("没有检索")
            || (normalized.contains("no ") && normalized.contains("found")));
  }

  private List<AiChatClient.ChatMessage> buildMessages(
      CurrentUserPrincipal principal,
      String question,
      AiChatAgentPlanDto plan,
      List<AiChatToolResultDto> toolResults,
      ToolValidationResult validation
  ) {
    String context = toolResults.isEmpty()
        ? "当前没有检索到结构化业务上下文。"
        : toolResults.stream()
            .map(tool -> tool.toolName() + ": " + tool.summary())
            .reduce((left, right) -> left + "\n" + right)
            .orElse("");
    List<AiChatClient.ChatMessage> messages = new java.util.ArrayList<>(List.of(
        new AiChatClient.ChatMessage(
            "system",
            "你是 IRIS 内控平台 AI 助手。只能基于后端提供的工具结果回答，不能编造系统数据。"
        ),
        new AiChatClient.ChatMessage(
            "system",
            "当前登录用户：" + principal.username() + "（账号：" + principal.account() + "，用户ID：" + principal.userId()
                + "）。当用户说“我”“我的”“我负责的”时，必须理解为当前登录用户；不要回答“无法识别你的姓名”，也不要要求用户再提供姓名。"
        ),
        new AiChatClient.ChatMessage("system", "工具结果：\n" + context)
    ));
    if (isVisibleProjectPlan(plan)) {
      messages.add(new AiChatClient.ChatMessage(
          "system",
          "项目工具返回的是当前账号在权限范围内可见的项目。回答“我负责的项目”时，基于工具结果说明当前账号可见/负责相关项目；如果工具无法区分负责人和可见范围，要明确说明这个限制，不要编造过滤结果。"
      ));
    }
    if (validation != null && validation.instruction() != null) {
      messages.add(new AiChatClient.ChatMessage("system", validation.instruction()));
    }
    messages.add(new AiChatClient.ChatMessage("user", question == null ? "" : question));
    return messages;
  }

  private boolean isVisibleProjectPlan(AiChatAgentPlanDto plan) {
    return plan != null
        && "list_visible_projects".equals(plan.intent())
        && nullToList(plan.toolCalls()).stream()
            .anyMatch(toolCall -> "ProjectQueryTool".equals(toolCall.toolName()));
  }

  private AiChatMessageDto assistant(
      String traceId,
      String sessionId,
      String status,
      String content,
      List<AiChatCitationDto> citations,
      List<AiChatToolResultDto> toolResults,
      Long latencyMs
  ) {
    return new AiChatMessageDto(
        UUID.randomUUID().toString(),
        traceId,
        sessionId,
        "assistant",
        content,
        status,
        nullToList(citations),
        nullToList(toolResults),
        latencyMs,
        DateTimeFormatters.formatDateTime(LocalDateTime.now())
    );
  }

  private String decryptApiKey(String cipherText) {
    try {
      byte[] payload = Base64.getDecoder().decode(cipherText);
      byte[] iv = Arrays.copyOfRange(payload, 0, 12);
      byte[] encrypted = Arrays.copyOfRange(payload, 12, payload.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey(), "AES"), new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new IllegalStateException("AI_MODEL_API_KEY_DECRYPT_FAILED", exception);
    }
  }

  private byte[] encryptionKey() throws Exception {
    return Arrays.copyOf(MessageDigest.getInstance("SHA-256")
        .digest(secretKey.getBytes(StandardCharsets.UTF_8)), 16);
  }

  private <T> List<T> nullToList(List<T> values) {
    return values == null ? List.of() : values;
  }

  private record ModelTraceDetail(
      Long id,
      String providerType,
      String modelName,
      String status,
      Integer defaultModel
  ) {
  }

  private record ModelResponseTraceDetail(
      Long id,
      String providerType,
      String modelName,
      Long latencyMs
  ) {
  }

  private record ToolValidationTraceDetail(
      String intent,
      int plannedToolCount,
      int returnedToolCount,
      long emptyCitationResultCount,
      boolean hasBlankSummary,
      boolean hasEmptyResult,
      String reviewReason
  ) {
  }

  private record ToolValidationResult(
      String status,
      ToolValidationTraceDetail detail,
      String instruction
  ) {
  }

  private record SessionContextTraceDetail(
      String projectCode,
      AiChatPageContextRequest pageContext
  ) {
  }
}
