package com.iris.back.business.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.ai.mapper.AiChatTraceEventMapper;
import com.iris.back.business.ai.mapper.AiChatTraceMapper;
import com.iris.back.business.ai.model.dto.AiChatCitationDto;
import com.iris.back.business.ai.model.dto.AiChatMessageDto;
import com.iris.back.business.ai.model.dto.AiChatTraceDetailDto;
import com.iris.back.business.ai.model.dto.AiChatTraceEventDto;
import com.iris.back.business.ai.model.dto.AiChatTraceListItemDto;
import com.iris.back.business.ai.model.dto.AiChatToolResultDto;
import com.iris.back.business.ai.model.entity.AiChatTraceEntity;
import com.iris.back.business.ai.model.entity.AiChatTraceEventEntity;
import com.iris.back.business.ai.model.request.AiChatMessageRequest;
import com.iris.back.business.ai.model.request.AiChatPageContextRequest;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.common.model.PageResponse;
import com.iris.back.common.util.DateTimeFormatters;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class AiChatTraceService {

  private static final Pattern PROJECT_CODE_PATTERN = Pattern.compile("\\bPRJ-[A-Za-z0-9-]+\\b", Pattern.CASE_INSENSITIVE);

  private final AiChatTraceMapper traceMapper;
  private final AiChatTraceEventMapper eventMapper;
  private final ObjectMapper objectMapper;
  private final CurrentUserContext currentUserContext;

  public AiChatTraceService(
      AiChatTraceMapper traceMapper,
      AiChatTraceEventMapper eventMapper,
      ObjectMapper objectMapper,
      CurrentUserContext currentUserContext
  ) {
    this.traceMapper = traceMapper;
    this.eventMapper = eventMapper;
    this.objectMapper = objectMapper;
    this.currentUserContext = currentUserContext;
  }

  public PageResponse<AiChatTraceListItemDto> list(String keyword, String status, Long page, Long pageSize) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    String normalizedKeyword = trimToNull(keyword);
    String normalizedStatus = trimToNull(status);
    List<AiChatTraceListItemDto> filtered = nullToList(traceMapper.selectList(
            new LambdaQueryWrapper<AiChatTraceEntity>()
                .eq(AiChatTraceEntity::getTenantId, principal.tenantId())
                .orderByDesc(AiChatTraceEntity::getCreatedAt)
                .orderByDesc(AiChatTraceEntity::getId)))
        .stream()
        .filter(entity -> !Objects.equals(entity.getDeleted(), 1))
        .filter(entity -> normalizedStatus == null || normalizedStatus.equals(entity.getStatus()))
        .filter(entity -> normalizedKeyword == null
            || containsIgnoreCase(entity.getTraceId(), normalizedKeyword)
            || containsIgnoreCase(entity.getQuestion(), normalizedKeyword)
            || containsIgnoreCase(entity.getAnswer(), normalizedKeyword)
            || containsIgnoreCase(entity.getUsername(), normalizedKeyword)
            || containsIgnoreCase(entity.getRoutePath(), normalizedKeyword)
            || containsIgnoreCase(entity.getModelName(), normalizedKeyword))
        .sorted(Comparator
            .comparing(AiChatTraceEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(AiChatTraceEntity::getId, Comparator.nullsLast(Comparator.reverseOrder())))
        .map(this::toListItemDto)
        .toList();
    long pageNo = normalizedPage(page);
    long size = normalizedPageSize(pageSize);
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * size);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + size);
    return PageResponse.of(filtered.size(), pageNo, size, filtered.subList(fromIndex, toIndex));
  }

  public AiChatTraceDetailDto detail(String traceId) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    String normalizedTraceId = normalizeRequiredText(traceId, "AI_CHAT_TRACE_ID_REQUIRED");
    AiChatTraceEntity entity = traceMapper.selectOne(new LambdaQueryWrapper<AiChatTraceEntity>()
        .eq(AiChatTraceEntity::getTenantId, principal.tenantId())
        .eq(AiChatTraceEntity::getTraceId, normalizedTraceId)
        .last("limit 1"));
    if (entity == null || Objects.equals(entity.getDeleted(), 1)) {
      throw new BusinessException("AI_CHAT_TRACE_NOT_FOUND", "AI_CHAT_TRACE_NOT_FOUND");
    }
    List<AiChatTraceEventDto> events = nullToList(eventMapper.selectList(
            new LambdaQueryWrapper<AiChatTraceEventEntity>()
                .eq(AiChatTraceEventEntity::getTenantId, principal.tenantId())
                .eq(AiChatTraceEventEntity::getTraceId, normalizedTraceId)
                .orderByAsc(AiChatTraceEventEntity::getSequenceNo)
                .orderByAsc(AiChatTraceEventEntity::getId)))
        .stream()
        .filter(event -> !Objects.equals(event.getDeleted(), 1))
        .sorted(Comparator
            .comparing(AiChatTraceEventEntity::getSequenceNo, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AiChatTraceEventEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(this::toEventDto)
        .toList();
    return toDetailDto(entity, events);
  }

  public String findLastProjectCodeInSession(CurrentUserPrincipal principal, String sessionId) {
    String normalizedSessionId = trimToNull(sessionId);
    if (principal == null || normalizedSessionId == null) {
      return null;
    }
    return nullToList(traceMapper.selectList(
            new LambdaQueryWrapper<AiChatTraceEntity>()
                .eq(AiChatTraceEntity::getTenantId, principal.tenantId())
                .eq(AiChatTraceEntity::getUserId, principal.userId())
                .eq(AiChatTraceEntity::getSessionId, normalizedSessionId)
                .orderByDesc(AiChatTraceEntity::getCreatedAt)
                .orderByDesc(AiChatTraceEntity::getId)
                .last("limit 20")))
        .stream()
        .filter(entity -> !Objects.equals(entity.getDeleted(), 1))
        .map(this::extractProjectCode)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  public TraceContext start(CurrentUserPrincipal principal, AiChatMessageRequest request) {
    String traceId = UUID.randomUUID().toString();
    LocalDateTime now = LocalDateTime.now();
    AiChatTraceEntity entity = new AiChatTraceEntity();
    entity.setTraceId(traceId);
    entity.setSessionId(request.sessionId());
    entity.setTenantId(principal.tenantId());
    entity.setUserId(principal.userId());
    entity.setUsername(principal.username());
    entity.setQuestion(request.message());
    AiChatPageContextRequest pageContext = request.pageContext();
    if (pageContext != null) {
      entity.setRoutePath(pageContext.routePath());
      entity.setEntityType(pageContext.entityType());
      entity.setEntityId(pageContext.entityId());
    }
    entity.setStatus("started");
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setCreatedBy(principal.userId());
    entity.setUpdatedBy(principal.userId());
    traceMapper.insert(entity);

    TraceContext context = new TraceContext(traceId, entity.getId(), principal.tenantId(), principal.userId(), new AtomicInteger());
    recordEvent(context, "trace", "start", "ok", new TraceStartDetail(
        request.sessionId(),
        request.message(),
        pageContext
    ), null);
    return context;
  }

  public void recordEvent(
      TraceContext context,
      String eventType,
      String eventName,
      String status,
      Object detail,
      Long elapsedMs
  ) {
    if (context == null) {
      return;
    }
    AiChatTraceEventEntity event = new AiChatTraceEventEntity();
    event.setTraceId(context.traceId());
    event.setSequenceNo(context.nextSequence());
    event.setEventType(eventType);
    event.setEventName(eventName);
    event.setStatus(status);
    event.setDetailJson(toJson(detail));
    event.setElapsedMs(elapsedMs);
    event.setTenantId(context.tenantId());
    event.setCreatedBy(context.userId());
    event.setUpdatedBy(context.userId());
    event.setDeleted(0);
    event.setVersion(0L);
    LocalDateTime now = LocalDateTime.now();
    event.setCreatedAt(now);
    event.setUpdatedAt(now);
    eventMapper.insert(event);
  }

  public void complete(
      TraceContext context,
      AiChatMessageDto response,
      Long modelConfigId,
      String providerType,
      String modelName,
      List<AiChatToolResultDto> toolResults,
      String errorMessage
  ) {
    if (context == null) {
      return;
    }
    AiChatTraceEntity entity = new AiChatTraceEntity();
    entity.setId(context.recordId());
    entity.setAnswer(response.content());
    entity.setStatus(response.status());
    entity.setModelConfigId(modelConfigId);
    entity.setProviderType(providerType);
    entity.setModelName(modelName);
    entity.setToolNamesJson(toJson(nullToList(toolResults).stream().map(AiChatToolResultDto::toolName).toList()));
    entity.setCitationsJson(toJson(nullToList(response.citations())));
    entity.setLatencyMs(response.latencyMs());
    entity.setErrorMessage(errorMessage);
    entity.setUpdatedAt(LocalDateTime.now());
    traceMapper.updateById(entity);
    recordEvent(context, "trace", "complete", response.status(), new TraceCompleteDetail(
        response.status(),
        modelConfigId,
        providerType,
        modelName,
        nullToList(toolResults).stream().map(AiChatToolResultDto::toolName).toList(),
        nullToList(response.citations())
    ), response.latencyMs());
  }

  private String toJson(Object detail) {
    if (detail == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(detail);
    } catch (JsonProcessingException exception) {
      return "{\"serializationError\":\"" + exception.getClass().getSimpleName() + "\"}";
    }
  }

  private <T> List<T> nullToList(List<T> values) {
    return values == null ? List.of() : values;
  }

  private AiChatTraceListItemDto toListItemDto(AiChatTraceEntity entity) {
    return new AiChatTraceListItemDto(
        entity.getTraceId(),
        entity.getSessionId(),
        entity.getUserId() == null ? null : String.valueOf(entity.getUserId()),
        entity.getUsername(),
        entity.getRoutePath(),
        entity.getQuestion(),
        entity.getStatus(),
        entity.getModelName(),
        entity.getToolNamesJson(),
        entity.getLatencyMs(),
        DateTimeFormatters.formatDateTime(entity.getCreatedAt())
    );
  }

  private AiChatTraceDetailDto toDetailDto(AiChatTraceEntity entity, List<AiChatTraceEventDto> events) {
    return new AiChatTraceDetailDto(
        entity.getTraceId(),
        entity.getSessionId(),
        entity.getUserId() == null ? null : String.valueOf(entity.getUserId()),
        entity.getUsername(),
        entity.getRoutePath(),
        entity.getEntityType(),
        entity.getEntityId(),
        entity.getQuestion(),
        entity.getAnswer(),
        entity.getStatus(),
        entity.getProviderType(),
        entity.getModelName(),
        entity.getToolNamesJson(),
        entity.getCitationsJson(),
        entity.getLatencyMs(),
        entity.getErrorMessage(),
        DateTimeFormatters.formatDateTime(entity.getCreatedAt()),
        DateTimeFormatters.formatDateTime(entity.getUpdatedAt()),
        events
    );
  }

  private AiChatTraceEventDto toEventDto(AiChatTraceEventEntity entity) {
    return new AiChatTraceEventDto(
        entity.getSequenceNo(),
        entity.getEventType(),
        entity.getEventName(),
        entity.getStatus(),
        entity.getDetailJson(),
        entity.getElapsedMs(),
        DateTimeFormatters.formatDateTime(entity.getCreatedAt())
    );
  }

  private String normalizeRequiredText(String value, String code) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw new BusinessException(code, code);
    }
    return normalized;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private boolean containsIgnoreCase(String value, String keyword) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
  }

  private String extractProjectCode(AiChatTraceEntity entity) {
    if (entity == null) {
      return null;
    }
    if ("project_code".equals(entity.getEntityType())) {
      String entityCode = extractProjectCode(entity.getEntityId());
      if (entityCode != null) {
        return entityCode;
      }
    }
    return Stream.of(entity.getQuestion(), entity.getAnswer(), entity.getCitationsJson())
        .map(this::extractProjectCode)
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(null);
  }

  private String extractProjectCode(String value) {
    if (value == null) {
      return null;
    }
    Matcher matcher = PROJECT_CODE_PATTERN.matcher(value);
    return matcher.find() ? matcher.group().toUpperCase(Locale.ROOT) : null;
  }

  private long normalizedPage(Long page) {
    return page == null || page < 1 ? 1 : page;
  }

  private long normalizedPageSize(Long pageSize) {
    if (pageSize == null || pageSize < 1) {
      return 10;
    }
    return Math.min(pageSize, 100);
  }

  public static final class TraceContext {
    private final String traceId;
    private final Long recordId;
    private final Long tenantId;
    private final Long userId;
    private final AtomicInteger sequence;

    public TraceContext(String traceId, Long recordId) {
      this(traceId, recordId, null, null, new AtomicInteger());
    }

    private TraceContext(String traceId, Long recordId, Long tenantId, Long userId, AtomicInteger sequence) {
      this.traceId = traceId;
      this.recordId = recordId;
      this.tenantId = tenantId;
      this.userId = userId;
      this.sequence = sequence;
    }

    public String traceId() {
      return traceId;
    }

    public Long recordId() {
      return recordId;
    }

    public Long tenantId() {
      return tenantId;
    }

    public Long userId() {
      return userId;
    }

    private int nextSequence() {
      return sequence.incrementAndGet();
    }
  }

  private record TraceStartDetail(
      String sessionId,
      String question,
      AiChatPageContextRequest pageContext
  ) {
  }

  private record TraceCompleteDetail(
      String status,
      Long modelConfigId,
      String providerType,
      String modelName,
      List<String> toolNames,
      List<AiChatCitationDto> citations
  ) {
  }
}
