package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.iris.back.business.project.mapper.BizProjectMapper;
import com.iris.back.business.project.mapper.BizProjectRectificationMapper;
import com.iris.back.business.project.mapper.BizProjectTaskWorkOrderMapper;
import com.iris.back.business.project.model.entity.BizProjectEntity;
import com.iris.back.business.project.model.entity.BizProjectRectificationEntity;
import com.iris.back.business.project.model.entity.BizProjectTaskWorkOrderEntity;
import com.iris.back.business.project.model.request.AlertQuery;
import com.iris.back.business.project.service.AlertService;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertServiceTests {

  @Mock
  private BizProjectMapper projectMapper;

  @Mock
  private BizProjectTaskWorkOrderMapper workOrderMapper;

  @Mock
  private BizProjectRectificationMapper rectificationMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  private AlertService alertService;

  @BeforeEach
  void setUp() {
    alertService = new AlertService(projectMapper, workOrderMapper, rectificationMapper, currentUserContext);
  }

  @Test
  void listBuildsRealAlertsFromFailedOmsSyncPendingArchiveAndOverdueRectification() {
    mockCurrentUser();
    when(projectMapper.selectList(any())).thenReturn(List.of(completedProject()));
    when(workOrderMapper.selectList(any())).thenReturn(List.of(failedWorkOrder()));
    when(rectificationMapper.selectList(any())).thenReturn(List.of(overdueRectification()));

    var page = alertService.list(new AlertQuery(null, null, 1L, 10L));

    assertThat(page.getTotal()).isEqualTo(3);
    assertThat(page.getRecords())
        .extracting("title")
        .contains("OMS工单同步失败", "项目待归档", "整改单已逾期");
    assertThat(page.getRecords().get(0).acknowledged()).isFalse();
  }

  @Test
  void listFiltersAlertsByLevelAndKeyword() {
    mockCurrentUser();
    when(projectMapper.selectList(any())).thenReturn(List.of(completedProject()));
    when(workOrderMapper.selectList(any())).thenReturn(List.of(failedWorkOrder()));
    when(rectificationMapper.selectList(any())).thenReturn(List.of(overdueRectification()));

    var page = alertService.list(new AlertQuery("OMS", "critical", 1L, 10L));

    assertThat(page.getTotal()).isEqualTo(1);
    assertThat(page.getRecords()).singleElement().satisfies(alert -> {
      assertThat(alert.level()).isEqualTo("critical");
      assertThat(alert.source()).isEqualTo("OMS工单");
      assertThat(alert.content()).contains("OMS接口超时");
    });
  }

  private BizProjectEntity completedProject() {
    BizProjectEntity entity = new BizProjectEntity();
    entity.setId(7001L);
    entity.setTenantId(1001L);
    entity.setProjectCode("PRJ-001");
    entity.setProjectName("资金检查");
    entity.setStatus("completed");
    entity.setArchiveStatus("none");
    entity.setUpdatedAt(LocalDateTime.of(2026, 5, 7, 10, 0));
    return entity;
  }

  private BizProjectTaskWorkOrderEntity failedWorkOrder() {
    BizProjectTaskWorkOrderEntity entity = new BizProjectTaskWorkOrderEntity();
    entity.setId(8001L);
    entity.setTenantId(1001L);
    entity.setProjectId(7001L);
    entity.setTaskId(7201L);
    entity.setOmsWorkOrderId("OMS-001");
    entity.setWorkOrderTitle("资金检查工单");
    entity.setSyncStatus("failed");
    entity.setSyncError("OMS接口超时");
    entity.setLastSyncedAt(LocalDateTime.of(2026, 5, 7, 10, 30));
    return entity;
  }

  private BizProjectRectificationEntity overdueRectification() {
    BizProjectRectificationEntity entity = new BizProjectRectificationEntity();
    entity.setId(9001L);
    entity.setTenantId(1001L);
    entity.setRectificationCode("RECT-001");
    entity.setTitle("补充审批记录");
    entity.setProjectName("资金检查");
    entity.setStatus("in_progress");
    entity.setDeadline(LocalDateTime.now().minusDays(1));
    return entity;
  }

  private void mockCurrentUser() {
    when(currentUserContext.requireCurrentUser()).thenReturn(new CurrentUserPrincipal(
        "token",
        2001L,
        1001L,
        "admin",
        "Platform Administrator",
        "IRIS",
        List.of("SUPER_ADMIN")
    ));
  }
}
