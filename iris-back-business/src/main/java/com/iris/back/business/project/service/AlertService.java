package com.iris.back.business.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.iris.back.business.project.mapper.BizProjectMapper;
import com.iris.back.business.project.mapper.BizProjectRectificationMapper;
import com.iris.back.business.project.mapper.BizProjectTaskWorkOrderMapper;
import com.iris.back.business.project.model.dto.AlertEventDto;
import com.iris.back.business.project.model.entity.BizProjectEntity;
import com.iris.back.business.project.model.entity.BizProjectRectificationEntity;
import com.iris.back.business.project.model.entity.BizProjectTaskWorkOrderEntity;
import com.iris.back.business.project.model.request.AlertQuery;
import com.iris.back.common.model.PageResponse;
import com.iris.back.common.util.DateTimeFormatters;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AlertService {

  private final BizProjectMapper projectMapper;
  private final BizProjectTaskWorkOrderMapper workOrderMapper;
  private final BizProjectRectificationMapper rectificationMapper;
  private final CurrentUserContext currentUserContext;
  private final Set<String> acknowledgedAlertIds = ConcurrentHashMap.newKeySet();

  public AlertService(
      BizProjectMapper projectMapper,
      BizProjectTaskWorkOrderMapper workOrderMapper,
      BizProjectRectificationMapper rectificationMapper,
      CurrentUserContext currentUserContext
  ) {
    this.projectMapper = projectMapper;
    this.workOrderMapper = workOrderMapper;
    this.rectificationMapper = rectificationMapper;
    this.currentUserContext = currentUserContext;
  }

  public PageResponse<AlertEventDto> list(AlertQuery query) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    AlertQuery safeQuery = query == null ? new AlertQuery(null, null, 1L, 10L) : query;
    List<AlertEventDto> alerts = new ArrayList<>();
    // 告警中心不再使用前端 mock，而是从当前租户的项目、OMS 工单快照和整改单实时计算异常事件。
    alerts.addAll(buildOmsSyncAlerts(principal.tenantId()));
    alerts.addAll(buildPendingArchiveAlerts(principal.tenantId()));
    alerts.addAll(buildOverdueRectificationAlerts(principal.tenantId()));

    List<AlertEventDto> filtered = alerts.stream()
        .filter(alert -> matches(alert, safeQuery))
        .sorted(Comparator.comparing(AlertService::timestampText, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
    long pageNo = normalizedPage(safeQuery.page());
    long pageSize = normalizedPageSize(safeQuery.pageSize());
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * pageSize);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + pageSize);
    return PageResponse.of(filtered.size(), pageNo, pageSize, filtered.subList(fromIndex, toIndex));
  }

  public void acknowledge(String id) {
    String normalized = trimToNull(id);
    if (normalized != null) {
      acknowledgedAlertIds.add(normalized);
    }
  }

  private List<AlertEventDto> buildOmsSyncAlerts(Long tenantId) {
    return nullToList(workOrderMapper.selectList(
        new LambdaQueryWrapper<BizProjectTaskWorkOrderEntity>()
            .eq(BizProjectTaskWorkOrderEntity::getTenantId, tenantId)
    )).stream()
        .filter(workOrder -> "failed".equals(workOrder.getSyncStatus()))
        .map(workOrder -> alert(
            "work-order-sync-" + workOrder.getId(),
            "OMS工单",
            "critical",
            "OMS工单同步失败",
            "工单：" + nonBlank(workOrder.getWorkOrderTitle(), workOrder.getOmsWorkOrderId(), String.valueOf(workOrder.getId()))
                + "，错误信息：" + nonBlank(workOrder.getSyncError(), null, "未知错误"),
            coalesceTime(workOrder.getLastSyncedAt(), workOrder.getUpdatedAt(), workOrder.getCreatedAt())
        ))
        .toList();
  }

  private List<AlertEventDto> buildPendingArchiveAlerts(Long tenantId) {
    return nullToList(projectMapper.selectList(
        new LambdaQueryWrapper<BizProjectEntity>()
            .eq(BizProjectEntity::getTenantId, tenantId)
    )).stream()
        .filter(project -> "completed".equals(project.getStatus()))
        .filter(project -> !"completed".equals(project.getArchiveStatus()))
        .map(project -> alert(
            "project-archive-" + project.getId(),
            "项目归档",
            "warning",
            "项目待归档",
            "项目：" + nonBlank(project.getProjectName(), project.getProjectCode(), String.valueOf(project.getId()))
                + " 已完成，等待项目负责人归档。",
            coalesceTime(project.getUpdatedAt(), project.getArchiveStartedAt(), project.getCreatedAt())
        ))
        .toList();
  }

  private List<AlertEventDto> buildOverdueRectificationAlerts(Long tenantId) {
    LocalDateTime now = LocalDateTime.now();
    return nullToList(rectificationMapper.selectList(
        new LambdaQueryWrapper<BizProjectRectificationEntity>()
            .eq(BizProjectRectificationEntity::getTenantId, tenantId)
    )).stream()
        .filter(rectification -> rectification.getDeadline() != null && rectification.getDeadline().isBefore(now))
        .filter(rectification -> !Set.of("completed", "approved").contains(rectification.getStatus()))
        .map(rectification -> alert(
            "rectification-overdue-" + rectification.getId(),
            "整改管理",
            "warning",
            "整改单已逾期",
            "整改单：" + nonBlank(rectification.getTitle(), rectification.getRectificationCode(), String.valueOf(rectification.getId()))
                + " 已超过截止时间，请跟进处理。",
            rectification.getDeadline()
        ))
        .toList();
  }

  private AlertEventDto alert(
      String id,
      String source,
      String level,
      String title,
      String content,
      LocalDateTime timestamp
  ) {
    return new AlertEventDto(
        id,
        source,
        level,
        title,
        content,
        DateTimeFormatters.formatDateTime(timestamp),
        acknowledgedAlertIds.contains(id)
    );
  }

  private boolean matches(AlertEventDto alert, AlertQuery query) {
    String level = trimToNull(query.level());
    if (level != null && !level.equals(alert.level())) {
      return false;
    }
    String keyword = trimToNull(query.keyword());
    if (keyword == null) {
      return true;
    }
    return (alert.source() + " " + alert.title() + " " + alert.content())
        .toLowerCase(Locale.ROOT)
        .contains(keyword.toLowerCase(Locale.ROOT));
  }

  private static String timestampText(AlertEventDto alert) {
    return alert.timestamp() == null ? "" : alert.timestamp();
  }

  private LocalDateTime coalesceTime(LocalDateTime first, LocalDateTime second, LocalDateTime fallback) {
    if (first != null) {
      return first;
    }
    return second == null ? fallback : second;
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
