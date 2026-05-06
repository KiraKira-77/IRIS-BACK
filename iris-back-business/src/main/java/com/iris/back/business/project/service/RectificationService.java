package com.iris.back.business.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.project.mapper.BizProjectRectificationMapper;
import com.iris.back.business.project.model.dto.RectificationDto;
import com.iris.back.business.project.model.entity.BizProjectRectificationEntity;
import com.iris.back.business.project.model.request.RectificationCreateRequest;
import com.iris.back.business.project.model.request.RectificationListQuery;
import com.iris.back.business.project.model.request.RectificationReviewRequest;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.common.model.PageResponse;
import com.iris.back.common.util.DateTimeFormatters;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RectificationService {

  private final BizProjectRectificationMapper rectificationMapper;
  private final CurrentUserContext currentUserContext;
  private final IdentifierGenerator identifierGenerator;

  public RectificationService(
      BizProjectRectificationMapper rectificationMapper,
      CurrentUserContext currentUserContext,
      IdentifierGenerator identifierGenerator
  ) {
    this.rectificationMapper = rectificationMapper;
    this.currentUserContext = currentUserContext;
    this.identifierGenerator = identifierGenerator;
  }

  public PageResponse<RectificationDto> list(RectificationListQuery query) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    RectificationListQuery safeQuery = query == null
        ? new RectificationListQuery(null, null, null, null, 1L, 10L)
        : query;
    List<RectificationDto> filtered = nullToList(rectificationMapper.selectList(
        new LambdaQueryWrapper<BizProjectRectificationEntity>()
            .eq(BizProjectRectificationEntity::getTenantId, principal.tenantId())
            .orderByDesc(BizProjectRectificationEntity::getUpdatedAt)
            .orderByDesc(BizProjectRectificationEntity::getId)
    )).stream()
        .filter(entity -> matches(entity, safeQuery))
        .map(this::toDto)
        .toList();
    long pageNo = normalizedPage(safeQuery.page());
    long pageSize = normalizedPageSize(safeQuery.pageSize());
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * pageSize);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + pageSize);
    return PageResponse.of(filtered.size(), pageNo, pageSize, filtered.subList(fromIndex, toIndex));
  }

  public RectificationDto get(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    return toDto(requireRectification(parseId(id, "RECTIFICATION_ID_INVALID"), principal.tenantId()));
  }

  @Transactional
  public RectificationDto create(RectificationCreateRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long id = nextId(new BizProjectRectificationEntity());
    BizProjectRectificationEntity entity = new BizProjectRectificationEntity();
    entity.setId(id);
    entity.setTenantId(principal.tenantId());
    entity.setRectificationCode("RECT-" + id);
    entity.setTitle(normalizeRequiredText(request.title(), "RECTIFICATION_TITLE_REQUIRED"));
    entity.setDescription(trimToNull(request.description()));
    entity.setProjectId(parseNullableId(request.projectId(), "RECTIFICATION_PROJECT_ID_INVALID"));
    entity.setProjectName(trimToNull(request.projectName()));
    entity.setTaskId(parseNullableId(request.taskId(), "RECTIFICATION_TASK_ID_INVALID"));
    entity.setChecklistItemId(0L);
    entity.setSourceWorkOrderRecordId(null);
    entity.setAssigneeId(parseId(request.assigneeId(), "RECTIFICATION_ASSIGNEE_ID_INVALID"));
    entity.setAssigneeName(normalizeRequiredText(request.assigneeName(), "RECTIFICATION_ASSIGNEE_NAME_REQUIRED"));
    entity.setContactId(parseNullableId(request.reviewerId(), "RECTIFICATION_REVIEWER_ID_INVALID"));
    entity.setContactName(trimToNull(request.reviewerName()));
    entity.setIssuedAt(LocalDateTime.now());
    entity.setDeadline(parseNullableDeadline(request.deadline()));
    entity.setStatus("pending");
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(principal.userId());
    entity.setUpdatedBy(principal.userId());
    rectificationMapper.insert(entity);
    return toDto(entity);
  }

  @Transactional
  public RectificationDto submit(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectRectificationEntity entity = requireRectification(parseId(id, "RECTIFICATION_ID_INVALID"), principal.tenantId());
    if (!List.of("pending", "in_progress", "rejected").contains(entity.getStatus())) {
      throw new BusinessException("RECTIFICATION_SUBMIT_STATUS_INVALID", "RECTIFICATION_SUBMIT_STATUS_INVALID");
    }
    entity.setStatus("submitted");
    entity.setUpdatedBy(principal.userId());
    rectificationMapper.updateById(entity);
    return toDto(entity);
  }

  @Transactional
  public RectificationDto review(String id, RectificationReviewRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectRectificationEntity entity = requireRectification(parseId(id, "RECTIFICATION_ID_INVALID"), principal.tenantId());
    if (!"submitted".equals(entity.getStatus()) && !"reviewing".equals(entity.getStatus())) {
      throw new BusinessException("RECTIFICATION_REVIEW_STATUS_INVALID", "RECTIFICATION_REVIEW_STATUS_INVALID");
    }
    String action = normalizeRequiredText(request.action(), "RECTIFICATION_REVIEW_ACTION_REQUIRED");
    if ("approve".equals(action)) {
      entity.setStatus("approved");
    } else if ("reject".equals(action)) {
      entity.setStatus("rejected");
    } else {
      throw new BusinessException("RECTIFICATION_REVIEW_ACTION_INVALID", "RECTIFICATION_REVIEW_ACTION_INVALID");
    }
    entity.setRemark(trimToNull(request.comment()));
    entity.setUpdatedBy(principal.userId());
    rectificationMapper.updateById(entity);
    return toDto(entity);
  }

  private RectificationDto toDto(BizProjectRectificationEntity entity) {
    return new RectificationDto(
        String.valueOf(entity.getId()),
        entity.getRectificationCode(),
        entity.getSourceWorkOrderRecordId() == null ? "manual" : "task",
        entity.getTaskId() == null ? null : String.valueOf(entity.getTaskId()),
        entity.getProjectId() == null ? null : String.valueOf(entity.getProjectId()),
        entity.getProjectName(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getAssigneeId() == null ? null : String.valueOf(entity.getAssigneeId()),
        entity.getAssigneeName(),
        entity.getContactId() == null ? null : String.valueOf(entity.getContactId()),
        entity.getContactName(),
        entity.getStatus(),
        DateTimeFormatters.formatDateTime(entity.getDeadline()),
        List.of(),
        entity.getRemark(),
        List.of(),
        DateTimeFormatters.formatDateTime(entity.getCreatedAt()),
        DateTimeFormatters.formatDateTime(entity.getUpdatedAt())
    );
  }

  private boolean matches(BizProjectRectificationEntity entity, RectificationListQuery query) {
    String keyword = trimToNull(query.keyword());
    String status = trimToNull(query.status());
    Long projectId = parseNullableId(query.projectId(), "RECTIFICATION_PROJECT_ID_INVALID");
    Long assigneeId = parseNullableId(query.assigneeId(), "RECTIFICATION_ASSIGNEE_ID_INVALID");
    return (keyword == null
        || containsIgnoreCase(entity.getRectificationCode(), keyword)
        || containsIgnoreCase(entity.getTitle(), keyword)
        || containsIgnoreCase(entity.getDescription(), keyword))
        && (status == null || status.equalsIgnoreCase(entity.getStatus()))
        && (projectId == null || Objects.equals(projectId, entity.getProjectId()))
        && (assigneeId == null || Objects.equals(assigneeId, entity.getAssigneeId()));
  }

  private BizProjectRectificationEntity requireRectification(Long id, Long tenantId) {
    BizProjectRectificationEntity entity = rectificationMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId) || Objects.equals(entity.getDeleted(), 1)) {
      throw new BusinessException("RECTIFICATION_NOT_FOUND", "rectification not found: " + id);
    }
    return entity;
  }

  private LocalDateTime parseNullableDeadline(String value) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      return null;
    }
    try {
      return LocalDate.parse(normalized).atTime(LocalTime.of(18, 0));
    } catch (DateTimeParseException exception) {
      throw new BusinessException("RECTIFICATION_DEADLINE_INVALID", "RECTIFICATION_DEADLINE_INVALID");
    }
  }

  private Long nextId(Object entity) {
    Object nextId = identifierGenerator.nextId(entity);
    if (nextId instanceof Number number) {
      return number.longValue();
    }
    return Long.valueOf(String.valueOf(nextId));
  }

  private Long parseId(String id, String code) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException exception) {
      throw new BusinessException(code, code);
    }
  }

  private Long parseNullableId(String id, String code) {
    String normalized = trimToNull(id);
    return normalized == null ? null : parseId(normalized, code);
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

  private long normalizedPage(Long page) {
    return page == null || page < 1 ? 1 : page;
  }

  private long normalizedPageSize(Long pageSize) {
    if (pageSize == null || pageSize < 1) {
      return 10;
    }
    return Math.min(pageSize, 100);
  }

  private <T> List<T> nullToList(List<T> values) {
    return values == null ? List.of() : values;
  }
}
