package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
import com.iris.back.business.checklist.model.entity.BizChecklistEntity;
import com.iris.back.business.checklist.model.entity.BizChecklistItemEntity;
import com.iris.back.business.project.mapper.BizProjectMapper;
import com.iris.back.business.project.mapper.BizProjectMemberMapper;
import com.iris.back.business.project.mapper.BizProjectTaskMapper;
import com.iris.back.business.project.mapper.BizProjectTaskWorkOrderMapper;
import com.iris.back.business.project.model.dto.ProjectDto;
import com.iris.back.business.project.model.entity.BizProjectEntity;
import com.iris.back.business.project.model.entity.BizProjectMemberEntity;
import com.iris.back.business.project.model.entity.BizProjectTaskEntity;
import com.iris.back.business.project.model.entity.BizProjectTaskWorkOrderEntity;
import com.iris.back.business.project.model.request.ProjectListQuery;
import com.iris.back.business.project.model.request.ProjectUpsertRequest;
import com.iris.back.business.project.service.ProjectService;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTests {

  @Mock
  private BizProjectMapper projectMapper;

  @Mock
  private BizProjectMemberMapper projectMemberMapper;

  @Mock
  private BizProjectTaskMapper projectTaskMapper;

  @Mock
  private BizProjectTaskWorkOrderMapper projectTaskWorkOrderMapper;

  @Mock
  private BizChecklistMapper checklistMapper;

  @Mock
  private BizChecklistItemMapper checklistItemMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  @Mock
  private IdentifierGenerator identifierGenerator;

  private ProjectService projectService;

  @BeforeEach
  void setUp() {
    projectService = new ProjectService(
        projectMapper,
        projectMemberMapper,
        projectTaskMapper,
        projectTaskWorkOrderMapper,
        checklistMapper,
        checklistItemMapper,
        currentUserContext,
        identifierGenerator
    );
  }

  @Test
  void workOrderModelSeparatesLocalAndOmsIdsAndUsesIdempotencyKey() {
    BizProjectTaskWorkOrderEntity workOrder = new BizProjectTaskWorkOrderEntity();
    workOrder.setId(1001L);
    workOrder.setOmsWorkOrderId("OMS-20260427-0001");
    workOrder.setIdempotencyKey("task-1:handler-1");

    assertThat(workOrder.getId()).isEqualTo(1001L);
    assertThat(workOrder.getOmsWorkOrderId()).isEqualTo("OMS-20260427-0001");
    assertThat(workOrder.getIdempotencyKey()).isEqualTo("task-1:handler-1");
  }

  @Test
  void createGeneratesTasksFromSelectedChecklistItems() {
    mockCurrentUser();
    when(identifierGenerator.nextId(any()))
        .thenReturn(7001L)
        .thenReturn(7101L)
        .thenReturn(7201L)
        .thenReturn(7202L);
    when(checklistMapper.selectList(any())).thenReturn(List.of(checklist()));
    when(checklistItemMapper.selectList(any())).thenReturn(List.of(
        checklistItem(9901L, "Bank reconciliation", "Complete by the 5th day"),
        checklistItem(9902L, "Payment approval", "Complete maker checker approval")
    ));

    ProjectDto created = projectService.create(new ProjectUpsertRequest(
        "PRJ-2026-001",
        "2026 Finance Control Project",
        "manual",
        null,
        null,
        "Finance controls",
        "2026-04-27",
        null,
        List.of("maintenance"),
        List.of("维护域"),
        "2001",
        "Platform Administrator",
        List.of("8801"),
        List.of(new ProjectUpsertRequest.ProjectMemberRequest(
            "2001",
            "Platform Administrator",
            "E2001",
            "Finance",
            "leader"
        ))
    ));

    ArgumentCaptor<BizProjectEntity> projectCaptor = ArgumentCaptor.forClass(BizProjectEntity.class);
    ArgumentCaptor<BizProjectTaskEntity> taskCaptor = ArgumentCaptor.forClass(BizProjectTaskEntity.class);
    verify(projectMapper).insert(projectCaptor.capture());
    verify(projectTaskMapper, times(2)).insert(taskCaptor.capture());

    assertThat(created.id()).isEqualTo("7001");
    assertThat(created.status()).isEqualTo("not_started");
    assertThat(created.taskCount()).isEqualTo(2);
    assertThat(created.progress()).isZero();
    assertThat(projectCaptor.getValue().getTenantId()).isEqualTo(1001L);
    assertThat(projectCaptor.getValue().getStatus()).isEqualTo("not_started");
    assertThat(taskCaptor.getAllValues())
        .extracting(BizProjectTaskEntity::getChecklistItemId)
        .containsExactly(9901L, 9902L);
    assertThat(taskCaptor.getAllValues())
        .allSatisfy(task -> {
          assertThat(task.getProjectId()).isEqualTo(7001L);
          assertThat(task.getStatus()).isEqualTo("pending");
        });
  }

  @Test
  void listReturnsOnlyProjectsWhereCurrentUserIsMember() {
    mockCurrentUser();
    when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(7001L, 2001L, "leader")));
    when(projectMapper.selectList(any())).thenReturn(List.of(
        project(7001L, "PRJ-2026-001", "Finance project", "in_progress"),
        project(7002L, "PRJ-2026-002", "Hidden project", "in_progress")
    ));
    when(projectTaskMapper.selectList(any())).thenReturn(List.of(
        task(7201L, 7001L, "passed"),
        task(7202L, 7001L, "pending")
    ));

    var page = projectService.list(new ProjectListQuery("Finance", "in_progress", null, null, null, null, 1L, 10L));

    assertThat(page.getTotal()).isEqualTo(1);
    assertThat(page.getRecords()).singleElement().satisfies(project -> {
      assertThat(project.id()).isEqualTo("7001");
      assertThat(project.taskCount()).isEqualTo(2);
      assertThat(project.progress()).isEqualTo(50);
    });
  }

  @Test
  void detailRejectsNonProjectMember() {
    mockCurrentUser();
    BizProjectEntity project = project(7001L, "PRJ-2026-001", "Finance project", "in_progress");
    project.setLeaderId(4001L);
    when(projectMapper.selectById(7001L)).thenReturn(project);
    when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(7001L, 3001L, "member")));

    Assertions.assertThatThrownBy(() -> projectService.get("7001"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PROJECT_FORBIDDEN");
  }

  @Test
  void leaderStartsNotStartedProject() {
    mockCurrentUser();
    when(projectMapper.selectById(7001L)).thenReturn(project(7001L, "PRJ-2026-001", "Finance project", "not_started"));
    when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(7001L, 2001L, "leader")));

    ProjectDto started = projectService.start("7001");

    ArgumentCaptor<BizProjectEntity> projectCaptor = ArgumentCaptor.forClass(BizProjectEntity.class);
    verify(projectMapper).updateById(projectCaptor.capture());
    assertThat(started.status()).isEqualTo("in_progress");
    assertThat(projectCaptor.getValue().getStatus()).isEqualTo("in_progress");
  }

  @Test
  void leaderCompletesProjectWhenEveryTaskIsHandled() {
    mockCurrentUser();
    when(projectMapper.selectById(7001L)).thenReturn(project(7001L, "PRJ-2026-001", "Finance project", "in_progress"));
    when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(7001L, 2001L, "leader")));
    when(projectTaskMapper.selectList(any())).thenReturn(List.of(
        task(7201L, 7001L, "passed"),
        task(7202L, 7001L, "nonconforming")
    ));

    ProjectDto completed = projectService.complete("7001");

    ArgumentCaptor<BizProjectEntity> projectCaptor = ArgumentCaptor.forClass(BizProjectEntity.class);
    verify(projectMapper).updateById(projectCaptor.capture());
    assertThat(completed.status()).isEqualTo("completed");
    assertThat(projectCaptor.getValue().getStatus()).isEqualTo("completed");
  }

  @Test
  void deleteOnlyAllowsNotStartedProject() {
    mockCurrentUser();
    when(projectMapper.selectById(7001L)).thenReturn(project(7001L, "PRJ-2026-001", "Finance project", "in_progress"));

    Assertions.assertThatThrownBy(() -> projectService.delete("7001"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PROJECT_DELETE_STATUS_INVALID");
  }

  @Test
  void nonLeaderCannotStartCompleteArchiveDeleteOrAssignTasks() {
    mockCurrentUser();
    BizProjectEntity project = project(7001L, "PRJ-2026-001", "Finance project", "not_started");
    project.setLeaderId(3001L);
    when(projectMapper.selectById(7001L)).thenReturn(project);

    Assertions.assertThatThrownBy(() -> projectService.start("7001"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PROJECT_LEADER_REQUIRED");
    Assertions.assertThatThrownBy(() -> projectService.delete("7001"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PROJECT_LEADER_REQUIRED");
  }

  private BizChecklistEntity checklist() {
    BizChecklistEntity entity = new BizChecklistEntity();
    entity.setId(8801L);
    entity.setTenantId(1001L);
    entity.setChecklistCode("CL-2026-001");
    entity.setChecklistName("Finance Checklist");
    entity.setStatus("active");
    return entity;
  }

  private BizChecklistItemEntity checklistItem(Long id, String content, String criterion) {
    BizChecklistItemEntity entity = new BizChecklistItemEntity();
    entity.setId(id);
    entity.setTenantId(1001L);
    entity.setChecklistId(8801L);
    entity.setContent(content);
    entity.setCriterion(criterion);
    entity.setControlFrequency("monthly");
    entity.setEvaluationType("operation");
    return entity;
  }

  private BizProjectEntity project(Long id, String code, String name, String status) {
    BizProjectEntity entity = new BizProjectEntity();
    entity.setId(id);
    entity.setTenantId(1001L);
    entity.setProjectCode(code);
    entity.setProjectName(name);
    entity.setSource("manual");
    entity.setStartDate(java.time.LocalDate.of(2026, 4, 27));
    entity.setStatus(status);
    entity.setTagIds("maintenance");
    entity.setTagNames("ç»´æŠ¤åŸŸ");
    entity.setLeaderId(2001L);
    entity.setLeaderName("Platform Administrator");
    entity.setChecklistIds("8801");
    entity.setArchiveStatus("none");
    return entity;
  }

  private BizProjectMemberEntity member(Long projectId, Long personnelId, String role) {
    BizProjectMemberEntity entity = new BizProjectMemberEntity();
    entity.setId(projectId + personnelId);
    entity.setTenantId(1001L);
    entity.setProjectId(projectId);
    entity.setPersonnelId(personnelId);
    entity.setPersonnelName("User " + personnelId);
    entity.setRole(role);
    return entity;
  }

  private BizProjectTaskEntity task(Long id, Long projectId, String status) {
    BizProjectTaskEntity entity = new BizProjectTaskEntity();
    entity.setId(id);
    entity.setTenantId(1001L);
    entity.setProjectId(projectId);
    entity.setChecklistId(8801L);
    entity.setChecklistItemId(9901L + id);
    entity.setCheckContent("Check " + id);
    entity.setCheckCriterion("Criterion " + id);
    entity.setStatus(status);
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
