package com.iris.back.business.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.project.mapper.BizProjectMemberMapper;
import com.iris.back.business.project.mapper.BizProjectRectificationMapper;
import com.iris.back.business.project.model.dto.ProjectTaskDto;
import com.iris.back.business.project.model.dto.RectificationDto;
import com.iris.back.business.project.model.entity.BizProjectMemberEntity;
import com.iris.back.business.project.model.entity.BizProjectRectificationEntity;
import com.iris.back.business.project.model.request.ProjectWorkOrderReturnRequest;
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
  private final BizProjectMemberMapper projectMemberMapper;
  private final CurrentUserContext currentUserContext;
  private final IdentifierGenerator identifierGenerator;
  private final OmsClient omsClient;

  public RectificationService(
      BizProjectRectificationMapper rectificationMapper,
      BizProjectMemberMapper projectMemberMapper,
      CurrentUserContext currentUserContext,
      IdentifierGenerator identifierGenerator,
      OmsClient omsClient
  ) {
    this.rectificationMapper = rectificationMapper;
    this.projectMemberMapper = projectMemberMapper;
    this.currentUserContext = currentUserContext;
    this.identifierGenerator = identifierGenerator;
    this.omsClient = omsClient;
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

  @Transactional
  public RectificationDto get(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectRectificationEntity entity = requireRectification(parseId(id, "RECTIFICATION_ID_INVALID"), principal.tenantId());
    if (refreshRectificationOmsSnapshot(entity)) {
      entity.setUpdatedBy(principal.userId());
      rectificationMapper.updateById(entity);
    }
    return toDto(entity);
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
  public RectificationDto createWorkOrder(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectRectificationEntity entity = requireRectification(parseId(id, "RECTIFICATION_ID_INVALID"), principal.tenantId());
    if (!"pending".equals(entity.getStatus())) {
      throw new BusinessException("RECTIFICATION_WORK_ORDER_STATUS_INVALID", "RECTIFICATION_WORK_ORDER_STATUS_INVALID");
    }
    if (trimToNull(entity.getRectificationOmsWorkOrderId()) != null) {
      throw new BusinessException("RECTIFICATION_WORK_ORDER_EXISTS", "RECTIFICATION_WORK_ORDER_EXISTS");
    }
    BizProjectMemberEntity assignee = requireAssigneeMember(entity, principal.tenantId());
    String employeeNo = normalizeRequiredText(assignee.getEmployeeNo(), "RECTIFICATION_ASSIGNEE_EMPLOYEE_NO_REQUIRED");
    OmsClient.OmsCreateCommand command = new OmsClient.OmsCreateCommand(
        String.valueOf(entity.getAssigneeId()),
        employeeNo,
        entity.getAssigneeName(),
        normalizeRequiredText(entity.getTitle(), "RECTIFICATION_TITLE_REQUIRED"),
        trimToNull(entity.getDescription()),
        "rectification:" + entity.getId(),
        entity.getId()
    );
    // 整改 OMS 工单是整改单自己的处理工单，和来源检查 OMS 工单不是同一条业务记录。
    OmsClient.OmsCreateResult result = omsClient.createWorkOrders(toProjectTaskDto(entity), List.of(command))
        .stream()
        .filter(item -> Objects.equals(item.handlerId(), command.handlerId()))
        .findFirst()
        .orElseThrow(() -> new BusinessException("RECTIFICATION_OMS_CREATE_FAILED", "RECTIFICATION_OMS_CREATE_FAILED"));
    if (trimToNull(result.error()) != null) {
      throw new BusinessException("RECTIFICATION_OMS_CREATE_FAILED", result.error());
    }
    entity.setRectificationOmsWorkOrderId(normalizeRequiredText(
        result.omsWorkOrderId(),
        "RECTIFICATION_OMS_WORK_ORDER_ID_REQUIRED"
    ));
    entity.setRectificationOmsStatus(result.status());
    entity.setRectificationOmsStatusName(result.status());
    entity.setRectificationWorkOrderCreatedAt(LocalDateTime.now());
    entity.setStatus("in_progress");
    entity.setUpdatedBy(principal.userId());
    rectificationMapper.updateById(entity);
    return toDto(entity);
  }

  @Transactional
  public RectificationDto returnWorkOrder(String id, ProjectWorkOrderReturnRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectRectificationEntity entity = requireRectification(parseId(id, "RECTIFICATION_ID_INVALID"), principal.tenantId());
    if (!"in_progress".equals(entity.getStatus())) {
      throw new BusinessException("RECTIFICATION_RETURN_STATUS_INVALID", "RECTIFICATION_RETURN_STATUS_INVALID");
    }
    String omsWorkOrderId = normalizeRequiredText(
        entity.getRectificationOmsWorkOrderId(),
        "RECTIFICATION_OMS_WORK_ORDER_ID_REQUIRED"
    );
    String reason = normalizeRequiredText(request.reason(), "RECTIFICATION_RETURN_REASON_REQUIRED");
    refreshRectificationOmsSnapshotWhenNotCompleted(entity);
    if (!isOmsCompleted(entity)) {
      throw new BusinessException("RECTIFICATION_WORK_ORDER_NOT_COMPLETED", "RECTIFICATION_WORK_ORDER_NOT_COMPLETED");
    }
    // 退回只针对整改 OMS 工单；内控侧审核完成后状态会变为 approved，此处不会再允许退回。
    omsClient.returnWorkOrder(omsWorkOrderId, reason);
    OmsClient.OmsWorkOrderSnapshot snapshot = omsClient.getWorkOrder(omsWorkOrderId);
    applyOmsSnapshot(entity, snapshot);
    if (!isOmsCompleted(entity)) {
      entity.setRectificationWorkOrderCompletedAt(null);
    }
    entity.setUpdatedBy(principal.userId());
    rectificationMapper.updateById(entity);
    return toDto(entity);
  }

  @Transactional
  public RectificationDto review(String id, RectificationReviewRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectRectificationEntity entity = requireRectification(parseId(id, "RECTIFICATION_ID_INVALID"), principal.tenantId());
    if (!"in_progress".equals(entity.getStatus())) {
      throw new BusinessException("RECTIFICATION_REVIEW_STATUS_INVALID", "RECTIFICATION_REVIEW_STATUS_INVALID");
    }
    String action = normalizeRequiredText(request.action(), "RECTIFICATION_REVIEW_ACTION_REQUIRED");
    if (!List.of("approve", "reject").contains(action)) {
      throw new BusinessException("RECTIFICATION_REVIEW_ACTION_INVALID", "RECTIFICATION_REVIEW_ACTION_INVALID");
    }
    refreshRectificationOmsSnapshotWhenNotCompleted(entity);
    if (!isOmsCompleted(entity)) {
      throw new BusinessException("RECTIFICATION_WORK_ORDER_NOT_COMPLETED", "RECTIFICATION_WORK_ORDER_NOT_COMPLETED");
    }
    // 整改单审核只有“通过/不通过”的结论差异，流程上都会终结为已完成，后续不再允许退回或重复审核。
    entity.setStatus("approved");
    entity.setReviewResult(action);
    entity.setCompletedAt(LocalDateTime.now());
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
        entity.getTaskName(),
        entity.getTaskDescription(),
        entity.getProjectId() == null ? null : String.valueOf(entity.getProjectId()),
        entity.getProjectName(),
        entity.getCheckContent(),
        entity.getSourceWorkOrderRecordId() == null ? null : String.valueOf(entity.getSourceWorkOrderRecordId()),
        entity.getOmsWorkOrderId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getAssigneeId() == null ? null : String.valueOf(entity.getAssigneeId()),
        entity.getAssigneeName(),
        entity.getContactId() == null ? null : String.valueOf(entity.getContactId()),
        entity.getContactName(),
        entity.getStatus(),
        DateTimeFormatters.formatDateTime(entity.getIssuedAt()),
        DateTimeFormatters.formatDateTime(entity.getDeadline()),
        DateTimeFormatters.formatDateTime(entity.getCompletedAt()),
        entity.getReviewResult(),
        entity.getRectificationOmsWorkOrderId(),
        entity.getRectificationOmsStatus(),
        entity.getRectificationOmsStatusName(),
        DateTimeFormatters.formatDateTime(entity.getRectificationWorkOrderCreatedAt()),
        DateTimeFormatters.formatDateTime(entity.getRectificationWorkOrderCompletedAt()),
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

  private BizProjectMemberEntity requireAssigneeMember(BizProjectRectificationEntity entity, Long tenantId) {
    Long projectId = entity.getProjectId();
    Long assigneeId = entity.getAssigneeId();
    if (projectId == null || assigneeId == null) {
      throw new BusinessException("RECTIFICATION_ASSIGNEE_REQUIRED", "RECTIFICATION_ASSIGNEE_REQUIRED");
    }
    return nullToList(projectMemberMapper.selectList(new LambdaQueryWrapper<BizProjectMemberEntity>()
        .eq(BizProjectMemberEntity::getTenantId, tenantId)
        .eq(BizProjectMemberEntity::getProjectId, projectId)))
        .stream()
        .filter(member -> Objects.equals(member.getPersonnelId(), assigneeId))
        .findFirst()
        .orElseThrow(() -> new BusinessException(
            "RECTIFICATION_ASSIGNEE_MEMBER_NOT_FOUND",
            "RECTIFICATION_ASSIGNEE_MEMBER_NOT_FOUND"
        ));
  }

  private ProjectTaskDto toProjectTaskDto(BizProjectRectificationEntity entity) {
    return new ProjectTaskDto(
        entity.getTaskId() == null ? null : String.valueOf(entity.getTaskId()),
        entity.getProjectId() == null ? null : String.valueOf(entity.getProjectId()),
        null,
        null,
        entity.getChecklistItemId() == null ? null : String.valueOf(entity.getChecklistItemId()),
        entity.getCheckContent(),
        null,
        null,
        null,
        entity.getTaskName(),
        entity.getTaskDescription(),
        entity.getAssigneeId() == null ? null : String.valueOf(entity.getAssigneeId()),
        entity.getAssigneeName(),
        entity.getContactId() == null ? null : String.valueOf(entity.getContactId()),
        entity.getContactName(),
        entity.getStatus(),
        DateTimeFormatters.formatDateTime(entity.getIssuedAt()),
        DateTimeFormatters.formatDateTime(entity.getCompletedAt()),
        0,
        0,
        0,
        List.of(),
        List.of()
    );
  }

  private void applyOmsSnapshot(BizProjectRectificationEntity entity, OmsClient.OmsWorkOrderSnapshot snapshot) {
    entity.setRectificationOmsStatus(snapshot.omsStatus());
    entity.setRectificationOmsStatusName(snapshot.omsStatusName());
    if (snapshot.reviewable() || isCompletedStatusText(snapshot.omsStatusName()) || isCompletedStatusText(snapshot.omsStatus())) {
      entity.setRectificationWorkOrderCompletedAt(LocalDateTime.now());
    }
  }

  private boolean refreshRectificationOmsSnapshot(BizProjectRectificationEntity entity) {
    String omsWorkOrderId = trimToNull(entity.getRectificationOmsWorkOrderId());
    if (!"in_progress".equals(entity.getStatus()) || omsWorkOrderId == null) {
      return false;
    }
    // 整改工单状态以 OMS 实时结果为准；内控侧只缓存最近一次查询结果，避免用旧状态阻塞退回或审核。
    applyOmsSnapshot(entity, omsClient.getWorkOrder(omsWorkOrderId));
    return true;
  }

  private void refreshRectificationOmsSnapshotWhenNotCompleted(BizProjectRectificationEntity entity) {
    if (!isOmsCompleted(entity)) {
      refreshRectificationOmsSnapshot(entity);
    }
  }

  private boolean isOmsCompleted(BizProjectRectificationEntity entity) {
    return isCompletedStatusText(entity.getRectificationOmsStatusName())
        || isCompletedStatusText(entity.getRectificationOmsStatus());
  }

  private boolean isCompletedStatusText(String status) {
    String normalized = trimToNull(status);
    if (normalized == null) {
      return false;
    }
    return "已完成".equals(normalized)
        || "20".equals(normalized)
        || "complete".equalsIgnoreCase(normalized)
        || "completed".equalsIgnoreCase(normalized);
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
