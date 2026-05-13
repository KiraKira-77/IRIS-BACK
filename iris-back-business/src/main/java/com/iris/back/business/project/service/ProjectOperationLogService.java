package com.iris.back.business.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iris.back.business.project.mapper.BizProjectOperationLogMapper;
import com.iris.back.business.project.model.dto.ProjectOperationLogDto;
import com.iris.back.business.project.model.entity.BizProjectOperationLogEntity;
import com.iris.back.business.project.model.request.ProjectOperationLogQuery;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectOperationLogService {

  private static final String PROJECT_SOURCE = "项目管理";

  private final BizProjectOperationLogMapper operationLogMapper;
  private final CurrentUserContext currentUserContext;

  public ProjectOperationLogService(
      BizProjectOperationLogMapper operationLogMapper,
      CurrentUserContext currentUserContext
  ) {
    this.operationLogMapper = operationLogMapper;
    this.currentUserContext = currentUserContext;
  }

  public PageResponse<ProjectOperationLogDto> list(ProjectOperationLogQuery query) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ProjectOperationLogQuery safeQuery = query == null
        ? new ProjectOperationLogQuery(null, null, null, null, null, 1L, 10L)
        : query;
    List<ProjectOperationLogDto> filtered = operationLogMapper.selectList(
            new LambdaQueryWrapper<BizProjectOperationLogEntity>()
                .eq(BizProjectOperationLogEntity::getTenantId, principal.tenantId())
                .orderByDesc(BizProjectOperationLogEntity::getCreatedAt)
                .orderByDesc(BizProjectOperationLogEntity::getId)
        ).stream()
        .filter(log -> matches(log, safeQuery))
        .sorted(Comparator
            .comparing(BizProjectOperationLogEntity::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(BizProjectOperationLogEntity::getId, Comparator.nullsLast(Comparator.reverseOrder())))
        .map(this::toDto)
        .toList();

    long pageNo = normalizedPage(safeQuery.page());
    long pageSize = normalizedPageSize(safeQuery.pageSize());
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * pageSize);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + pageSize);
    return PageResponse.of(filtered.size(), pageNo, pageSize, filtered.subList(fromIndex, toIndex));
  }

  @Transactional
  public void recordProjectLog(
      Long projectId,
      Long taskId,
      Long workOrderId,
      String action,
      String remark
  ) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    recordProjectLog(principal, projectId, taskId, workOrderId, action, remark);
  }

  @Transactional
  public void recordProjectLog(
      CurrentUserPrincipal principal,
      Long projectId,
      Long taskId,
      Long workOrderId,
      String action,
      String remark
  ) {
    if (projectId == null) {
      throw new BusinessException("PROJECT_LOG_PROJECT_ID_REQUIRED", "PROJECT_LOG_PROJECT_ID_REQUIRED");
    }
    String normalizedAction = normalizeRequiredText(action, "PROJECT_LOG_ACTION_REQUIRED");
    LocalDateTime now = LocalDateTime.now();
    BizProjectOperationLogEntity log = new BizProjectOperationLogEntity();
    log.setTenantId(principal.tenantId());
    log.setProjectId(projectId);
    log.setTaskId(taskId);
    log.setWorkOrderId(workOrderId);
    log.setAction(normalizedAction);
    log.setOperatorId(principal.userId());
    log.setOperatorName(principal.username());
    log.setRemark(trimToNull(remark));
    log.setDeleted(0);
    log.setVersion(0L);
    log.setCreatedBy(principal.userId());
    log.setUpdatedBy(principal.userId());
    log.setCreatedAt(now);
    log.setUpdatedAt(now);
    operationLogMapper.insert(log);
  }

  private boolean matches(BizProjectOperationLogEntity log, ProjectOperationLogQuery query) {
    if (!PROJECT_SOURCE.equals(trimToNull(query.source())) && trimToNull(query.source()) != null) {
      return false;
    }
    if (trimToNull(query.level()) != null && !deriveLevel(log).equals(query.level().trim())) {
      return false;
    }
    if (trimToNull(query.action()) != null && !query.action().trim().equals(log.getAction())) {
      return false;
    }
    if (trimToNull(query.projectId()) != null && !Objects.equals(parseId(query.projectId()), log.getProjectId())) {
      return false;
    }
    String keyword = trimToNull(query.keyword());
    if (keyword == null) {
      return true;
    }
    String searchable = String.join(" ",
        nullToText(log.getAction()),
        nullToText(log.getRemark()),
        nullToText(log.getOperatorName()),
        log.getProjectId() == null ? "" : String.valueOf(log.getProjectId()),
        log.getTaskId() == null ? "" : String.valueOf(log.getTaskId()),
        log.getWorkOrderId() == null ? "" : String.valueOf(log.getWorkOrderId())
    );
    return containsIgnoreCase(searchable, keyword);
  }

  private ProjectOperationLogDto toDto(BizProjectOperationLogEntity log) {
    return new ProjectOperationLogDto(
        String.valueOf(log.getId()),
        PROJECT_SOURCE,
        deriveLevel(log),
        log.getAction(),
        log.getRemark(),
        DateTimeFormatters.formatDateTime(log.getCreatedAt()),
        log.getProjectId() == null ? null : String.valueOf(log.getProjectId()),
        log.getTaskId() == null ? null : String.valueOf(log.getTaskId()),
        log.getWorkOrderId() == null ? null : String.valueOf(log.getWorkOrderId()),
        log.getAction(),
        log.getOperatorName(),
        deriveResult(log)
    );
  }

  private String deriveLevel(BizProjectOperationLogEntity log) {
    String text = nullToText(log.getAction()) + " " + nullToText(log.getRemark());
    if (text.contains("失败") || text.toLowerCase(Locale.ROOT).contains("error")) {
      return "error";
    }
    if (text.contains("退回") || text.contains("删除") || text.contains("承担风险")) {
      return "warn";
    }
    return "info";
  }

  private String deriveResult(BizProjectOperationLogEntity log) {
    return "error".equals(deriveLevel(log)) ? "failure" : "success";
  }

  private Long parseId(String id) {
    try {
      return Long.valueOf(id.trim());
    } catch (NumberFormatException exception) {
      throw new BusinessException("PROJECT_LOG_PROJECT_ID_INVALID", "PROJECT_LOG_PROJECT_ID_INVALID");
    }
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

  private String nullToText(String value) {
    return value == null ? "" : value;
  }

  private boolean containsIgnoreCase(String value, String keyword) {
    return value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
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
}
