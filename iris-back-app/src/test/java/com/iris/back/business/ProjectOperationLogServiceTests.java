package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iris.back.business.project.mapper.BizProjectOperationLogMapper;
import com.iris.back.business.project.model.dto.ProjectOperationLogDto;
import com.iris.back.business.project.model.entity.BizProjectOperationLogEntity;
import com.iris.back.business.project.model.request.ProjectOperationLogQuery;
import com.iris.back.business.project.service.ProjectOperationLogService;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectOperationLogServiceTests {

  @Mock
  private BizProjectOperationLogMapper operationLogMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  private ProjectOperationLogService operationLogService;

  @BeforeEach
  void setUp() {
    operationLogService = new ProjectOperationLogService(operationLogMapper, currentUserContext);
  }

  @Test
  void listReturnsProjectOperationLogsWithKeywordAndLevelFilters() {
    mockCurrentUser();
    BizProjectOperationLogEntity reviewLog = log(9001L, "审核工单", "业务确认通过", "Platform Administrator");
    reviewLog.setProjectId(7001L);
    reviewLog.setTaskId(7201L);
    reviewLog.setWorkOrderId(8001L);
    reviewLog.setCreatedAt(LocalDateTime.of(2026, 5, 7, 10, 30, 0));
    BizProjectOperationLogEntity failedLog = log(9002L, "同步OMS工单失败", "OMS接口超时", "Platform Administrator");
    failedLog.setCreatedAt(LocalDateTime.of(2026, 5, 7, 10, 35, 0));
    when(operationLogMapper.selectList(any())).thenReturn(List.of(reviewLog, failedLog));

    var page = operationLogService.list(new ProjectOperationLogQuery(
        "OMS",
        "项目管理",
        "error",
        null,
        null,
        1L,
        10L
    ));

    assertThat(page.getTotal()).isEqualTo(1);
    assertThat(page.getRecords()).singleElement().satisfies(log -> {
      assertThat(log.id()).isEqualTo("9002");
      assertThat(log.source()).isEqualTo("项目管理");
      assertThat(log.level()).isEqualTo("error");
      assertThat(log.message()).isEqualTo("同步OMS工单失败");
      assertThat(log.detail()).isEqualTo("OMS接口超时");
      assertThat(log.timestamp()).isEqualTo("2026-05-07 10:35:00");
    });
  }

  @Test
  void recordProjectLogPersistsCurrentUserAndBusinessContext() {
    mockCurrentUser();

    operationLogService.recordProjectLog(
        7001L,
        7201L,
        8001L,
        "生成整改单",
        "来源工单审核为不符合项"
    );

    ArgumentCaptor<BizProjectOperationLogEntity> logCaptor =
        ArgumentCaptor.forClass(BizProjectOperationLogEntity.class);
    verify(operationLogMapper).insert(logCaptor.capture());
    assertThat(logCaptor.getValue()).satisfies(log -> {
      assertThat(log.getTenantId()).isEqualTo(1001L);
      assertThat(log.getProjectId()).isEqualTo(7001L);
      assertThat(log.getTaskId()).isEqualTo(7201L);
      assertThat(log.getWorkOrderId()).isEqualTo(8001L);
      assertThat(log.getAction()).isEqualTo("生成整改单");
      assertThat(log.getOperatorId()).isEqualTo(2001L);
      assertThat(log.getOperatorName()).isEqualTo("Platform Administrator");
      assertThat(log.getRemark()).isEqualTo("来源工单审核为不符合项");
      assertThat(log.getDeleted()).isEqualTo(0);
      assertThat(log.getVersion()).isEqualTo(0L);
      assertThat(log.getCreatedAt()).isNotNull();
      assertThat(log.getUpdatedAt()).isNotNull();
    });
  }

  private BizProjectOperationLogEntity log(Long id, String action, String remark, String operatorName) {
    BizProjectOperationLogEntity entity = new BizProjectOperationLogEntity();
    entity.setId(id);
    entity.setTenantId(1001L);
    entity.setProjectId(7001L);
    entity.setAction(action);
    entity.setRemark(remark);
    entity.setOperatorName(operatorName);
    entity.setDeleted(0);
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
