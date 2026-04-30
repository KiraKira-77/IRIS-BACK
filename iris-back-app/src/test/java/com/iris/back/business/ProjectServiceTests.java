package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
import com.iris.back.business.checklist.model.entity.BizChecklistEntity;
import com.iris.back.business.checklist.model.entity.BizChecklistItemEntity;
import com.iris.back.business.plan.mapper.BizPlanItemMapper;
import com.iris.back.business.plan.model.entity.BizPlanItemEntity;
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
import com.iris.back.business.project.model.request.ProjectTaskAssignRequest;
import com.iris.back.business.project.model.request.ProjectUpsertRequest;
import com.iris.back.business.project.model.request.ProjectWorkOrderCreateRequest;
import com.iris.back.business.project.service.OmsClient;
import com.iris.back.business.project.service.ProjectService;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDateTime;
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
  private BizPlanItemMapper planItemMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @Mock
  private OmsClient omsClient;

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
        planItemMapper,
        currentUserContext,
        identifierGenerator,
        omsClient,
        new ObjectMapper()
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
    assertThat(projectCaptor.getValue().getTagIds()).isEmpty();
    assertThat(projectCaptor.getValue().getTagNames()).isEmpty();
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
  void createFromPlanLinksPlanItemsToCreatedProject() {
    mockCurrentUser();
    when(identifierGenerator.nextId(any()))
        .thenReturn(7001L)
        .thenReturn(7101L)
        .thenReturn(7201L);
    when(checklistMapper.selectList(any())).thenReturn(List.of(checklist()));
    when(checklistItemMapper.selectList(any())).thenReturn(List.of(
        checklistItem(9901L, "Bank reconciliation", "Complete by the 5th day")
    ));

    projectService.create(new ProjectUpsertRequest(
        "PRJ-2026-001",
        "2026 Finance Control Project",
        "plan",
        "9001",
        "2026 January Control Plan",
        "Finance controls",
        "2026-04-27",
        null,
        List.of(),
        List.of(),
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

    ArgumentCaptor<BizPlanItemEntity> planItemCaptor = ArgumentCaptor.forClass(BizPlanItemEntity.class);
    verify(planItemMapper).update(planItemCaptor.capture(), any());
    assertThat(planItemCaptor.getValue().getProjectId()).isEqualTo("7001");
  }

  @Test
  void createRejectsLegacyProjectMemberRoles() {
    mockCurrentUser();
    when(identifierGenerator.nextId(any())).thenReturn(7001L);
    when(checklistMapper.selectList(any())).thenReturn(List.of(checklist()));
    when(checklistItemMapper.selectList(any())).thenReturn(List.of(
        checklistItem(9901L, "Bank reconciliation", "Complete by the 5th day")
    ));

    Assertions.assertThatThrownBy(() -> projectService.create(new ProjectUpsertRequest(
        "PRJ-2026-001",
        "2026 Finance Control Project",
        "manual",
        null,
        null,
        "Finance controls",
        "2026-04-27",
        null,
        List.of(),
        List.of(),
        "2001",
        "Platform Administrator",
        List.of("8801"),
        List.of(new ProjectUpsertRequest.ProjectMemberRequest(
            "2001",
            "Platform Administrator",
            "E2001",
            "Finance",
            "member"
        ))
    )))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PROJECT_MEMBER_ROLE_INVALID");
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
  void formatsProjectAndTaskTimestampsWithoutIsoSeparator() {
    mockCurrentUser();
    BizProjectEntity project = project(7001L, "PRJ-2026-001", "Finance project", "in_progress");
    project.setArchiveStartedAt(LocalDateTime.of(2026, 4, 29, 9, 10, 11));
    project.setArchiveCompletedAt(LocalDateTime.of(2026, 4, 29, 10, 11, 12));
    project.setCreatedAt(LocalDateTime.of(2026, 4, 29, 13, 6, 50));
    project.setUpdatedAt(LocalDateTime.of(2026, 4, 29, 13, 7, 8));
    BizProjectTaskEntity task = task(7201L, 7001L, "in_progress");
    task.setIssuedAt(LocalDateTime.of(2026, 4, 29, 11, 12, 13));
    task.setCompletedAt(LocalDateTime.of(2026, 4, 29, 12, 13, 14));
    when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(7001L, 2001L, "leader")));
    when(projectMapper.selectList(any())).thenReturn(List.of(project));
    when(projectTaskMapper.selectList(any())).thenReturn(List.of(task));

    var page = projectService.list(new ProjectListQuery(null, null, null, null, null, null, 1L, 10L));

    assertThat(page.getRecords()).singleElement().satisfies(item -> {
      assertThat(item.archiveStartedAt()).isEqualTo("2026-04-29 09:10:11");
      assertThat(item.archiveCompletedAt()).isEqualTo("2026-04-29 10:11:12");
      assertThat(item.createdAt()).isEqualTo("2026-04-29 13:06:50");
      assertThat(item.updatedAt()).isEqualTo("2026-04-29 13:07:08");
      assertThat(item.tasks()).singleElement().satisfies(resultTask -> {
        assertThat(resultTask.issuedAt()).isEqualTo("2026-04-29 11:12:13");
        assertThat(resultTask.completedAt()).isEqualTo("2026-04-29 12:13:14");
      });
    });
  }

  @Test
  void formatsWorkOrderTimestampsWithoutIsoSeparator() {
    mockCurrentUser();
    BizProjectEntity project = project(7001L, "PRJ-2026-001", "Finance project", "in_progress");
    BizProjectTaskEntity task = task(7201L, 7001L, "in_progress");
    task.setAssigneeId(2001L);
    BizProjectTaskWorkOrderEntity workOrder = workOrder(8001L, 7001L, 7201L, "OMS-20260429-0001");
    workOrder.setIssuedAt(LocalDateTime.of(2026, 4, 29, 11, 12, 13));
    workOrder.setCompletedAt(LocalDateTime.of(2026, 4, 29, 12, 13, 14));
    workOrder.setLastSyncedAt(LocalDateTime.of(2026, 4, 29, 13, 14, 15));
    workOrder.setIrisReviewedAt(LocalDateTime.of(2026, 4, 29, 14, 15, 16));
    when(projectMapper.selectById(7001L)).thenReturn(project);
    when(projectTaskMapper.selectById(7201L)).thenReturn(task);
    when(projectTaskWorkOrderMapper.selectList(any())).thenReturn(List.of(workOrder));

    var result = projectService.listTaskWorkOrders("7001", "7201");

    assertThat(result).singleElement().satisfies(item -> {
      assertThat(item.issuedAt()).isEqualTo("2026-04-29 11:12:13");
      assertThat(item.completedAt()).isEqualTo("2026-04-29 12:13:14");
      assertThat(item.lastSyncedAt()).isEqualTo("2026-04-29 13:14:15");
      assertThat(item.irisReviewedAt()).isEqualTo("2026-04-29 14:15:16");
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
  void projectLeaderUpdatesProjectInfoUntilArchived() {
    mockCurrentUser();
    BizProjectEntity project = project(7001L, "PRJ-2026-001", "Finance project", "completed");
    when(projectMapper.selectById(7001L)).thenReturn(project);
    when(projectTaskMapper.selectList(any())).thenReturn(List.of(task(7201L, 7001L, "passed")));
    when(identifierGenerator.nextId(any())).thenReturn(7101L);

    ProjectDto updated = projectService.update("7001", new ProjectUpsertRequest(
        "PRJ-2026-001",
        "Updated finance project",
        "manual",
        null,
        null,
        "Updated controls",
        "2026-04-28",
        "2026-05-01",
        List.of(),
        List.of(),
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
    verify(projectMapper).updateById(projectCaptor.capture());
    verify(projectMemberMapper).delete(any());
    verify(projectMemberMapper).insert(any(BizProjectMemberEntity.class));
    assertThat(updated.name()).isEqualTo("Updated finance project");
    assertThat(updated.status()).isEqualTo("completed");
    assertThat(projectCaptor.getValue().getProjectName()).isEqualTo("Updated finance project");
    assertThat(projectCaptor.getValue().getStatus()).isEqualTo("completed");
  }

  @Test
  void updateRejectsArchivedProject() {
    mockCurrentUser();
    when(projectMapper.selectById(7001L)).thenReturn(project(7001L, "PRJ-2026-001", "Finance project", "archived"));

    Assertions.assertThatThrownBy(() -> projectService.update("7001", new ProjectUpsertRequest(
        "PRJ-2026-001",
        "Updated finance project",
        "manual",
        null,
        null,
        "Updated controls",
        "2026-04-28",
        null,
        List.of(),
        List.of(),
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
    )))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PROJECT_ARCHIVED_EDIT_FORBIDDEN");
    verify(projectMapper, never()).updateById(any(BizProjectEntity.class));
    verify(projectMemberMapper, never()).delete(any());
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

  @Test
  void assignTasksRejectsObserverMembers() {
    mockCurrentUser();
    BizProjectEntity project = project(7001L, "PRJ-2026-001", "Finance project", "in_progress");
    when(projectMapper.selectById(7001L)).thenReturn(project);
    when(projectMemberMapper.selectList(any())).thenReturn(List.of(
        member(7001L, 2001L, "leader"),
        member(7001L, 3001L, "observer")
    ));

    Assertions.assertThatThrownBy(() -> projectService.assignTasks("7001", new ProjectTaskAssignRequest(
        List.of("7201"),
        "3001",
        "Observer",
        null,
        null
    )))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PROJECT_TASK_ASSIGNEE_ROLE_INVALID");
    verify(projectTaskMapper, never()).updateById(any(BizProjectTaskEntity.class));
  }

  @Test
  void assignTasksAllowsLeadersAndAuditors() {
    mockCurrentUser();
    BizProjectEntity project = project(7001L, "PRJ-2026-001", "Finance project", "in_progress");
    BizProjectTaskEntity task = task(7201L, 7001L, "pending");
    when(projectMapper.selectById(7001L)).thenReturn(project);
    when(projectMemberMapper.selectList(any())).thenReturn(List.of(
        member(7001L, 2001L, "leader"),
        member(7001L, 3001L, "auditor")
    ));
    when(projectTaskMapper.selectById(7201L)).thenReturn(task);
    when(projectTaskMapper.selectList(any())).thenReturn(List.of(task));

    ProjectDto result = projectService.assignTasks("7001", new ProjectTaskAssignRequest(
        List.of("7201"),
        "3001",
        "Auditor",
        "4001",
        "Contact"
    ));

    ArgumentCaptor<BizProjectTaskEntity> taskCaptor = ArgumentCaptor.forClass(BizProjectTaskEntity.class);
    verify(projectTaskMapper).updateById(taskCaptor.capture());
    assertThat(taskCaptor.getValue().getAssigneeId()).isEqualTo(3001L);
    assertThat(taskCaptor.getValue().getAssigneeName()).isEqualTo("Auditor");
    assertThat(taskCaptor.getValue().getContactId()).isEqualTo(4001L);
    assertThat(taskCaptor.getValue().getContactName()).isEqualTo("Contact");
    assertThat(result.tasks()).singleElement().satisfies(assignedTask -> {
      assertThat(assignedTask.assigneeId()).isEqualTo("3001");
      assertThat(assignedTask.assigneeName()).isEqualTo("Auditor");
    });
  }

  @Test
  void projectLeaderCanCreateWorkOrdersForAssignedInspectionItems() {
    mockCurrentUser();
    BizProjectTaskEntity task = task(7201L, 7001L, "pending");
    task.setAssigneeId(3001L);
    task.setAssigneeName("Auditor");
    when(projectMapper.selectById(7001L)).thenReturn(project(7001L, "PRJ-2026-001", "Finance project", "in_progress"));
    when(projectTaskMapper.selectById(7201L)).thenReturn(task);
    when(projectTaskWorkOrderMapper.selectList(any())).thenReturn(List.of());
    when(identifierGenerator.nextId(any())).thenReturn(8001L);
    when(omsClient.createWorkOrders(any(), any())).thenReturn(List.of(
        new OmsClient.OmsCreateResult("201", "OMS-20260427-0001", "created", null, "{}")
    ));

    var workOrders = projectService.createWorkOrders("7001", "7201", new ProjectWorkOrderCreateRequest(
        "Finance check",
        "Please complete the check in OMS",
        List.of(new ProjectWorkOrderCreateRequest.HandlerRequest("201", "Handler A"))
    ));

    assertThat(workOrders).hasSize(1);
    verify(projectTaskWorkOrderMapper).insert(any(BizProjectTaskWorkOrderEntity.class));
  }

  @Test
  void createWorkOrdersRejectsNotStartedProjects() {
    mockCurrentUser();
    BizProjectTaskEntity task = task(7201L, 7001L, "pending");
    task.setAssigneeId(2001L);
    when(projectMapper.selectById(7001L)).thenReturn(project(7001L, "PRJ-2026-001", "Finance project", "not_started"));
    when(projectTaskMapper.selectById(7201L)).thenReturn(task);

    Assertions.assertThatThrownBy(() -> projectService.createWorkOrders(
        "7001",
        "7201",
        new ProjectWorkOrderCreateRequest(
            "Finance check",
            "Please complete the check in OMS",
            List.of(new ProjectWorkOrderCreateRequest.HandlerRequest("201", "Handler A"))
        )
    ))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("PROJECT_NOT_STARTED");
    verify(projectTaskWorkOrderMapper, never()).insert(any(BizProjectTaskWorkOrderEntity.class));
  }

  @Test
  void createWorkOrdersCreatesOneChildRowPerHandlerWithoutDuplicatingTasks() {
    mockCurrentUser();
    BizProjectTaskEntity task = task(7201L, 7001L, "pending");
    task.setAssigneeId(2001L);
    task.setAssigneeName("Platform Administrator");
    when(projectMapper.selectById(7001L)).thenReturn(project(7001L, "PRJ-2026-001", "Finance project", "in_progress"));
    when(projectTaskMapper.selectById(7201L)).thenReturn(task);
    when(projectTaskWorkOrderMapper.selectList(any())).thenReturn(List.of());
    when(identifierGenerator.nextId(any()))
        .thenReturn(8001L)
        .thenReturn(8002L);
    when(omsClient.createWorkOrders(any(), any())).thenReturn(List.of(
        new OmsClient.OmsCreateResult("201", "OMS-20260427-0001", "created", null, "{}"),
        new OmsClient.OmsCreateResult("202", "OMS-20260427-0002", "created", null, "{}")
    ));

    var workOrders = projectService.createWorkOrders("7001", "7201", new ProjectWorkOrderCreateRequest(
        "Finance check",
        "Please complete the check in OMS",
        List.of(
            new ProjectWorkOrderCreateRequest.HandlerRequest("201", "Handler A"),
            new ProjectWorkOrderCreateRequest.HandlerRequest("202", "Handler B")
        )
    ));

    ArgumentCaptor<BizProjectTaskWorkOrderEntity> workOrderCaptor =
        ArgumentCaptor.forClass(BizProjectTaskWorkOrderEntity.class);
    verify(projectTaskWorkOrderMapper, times(2)).insert(workOrderCaptor.capture());
    verify(projectTaskMapper, never()).insert(any(BizProjectTaskEntity.class));
    assertThat(workOrders).hasSize(2);
    assertThat(workOrderCaptor.getAllValues())
        .extracting(BizProjectTaskWorkOrderEntity::getIdempotencyKey)
        .containsExactly("7201:201", "7201:202");
    assertThat(workOrderCaptor.getAllValues())
        .extracting(BizProjectTaskWorkOrderEntity::getOmsWorkOrderId)
        .containsExactly("OMS-20260427-0001", "OMS-20260427-0002");
    assertThat(workOrders)
        .extracting("reviewable")
        .containsExactly(false, false);
    assertThat(task.getStatus()).isEqualTo("in_progress");
  }

  @Test
  void refreshWorkOrderStoresOmsSnapshotAndSyncStatus() {
    mockCurrentUser();
    BizProjectTaskEntity task = task(7201L, 7001L, "in_progress");
    task.setAssigneeId(2001L);
    BizProjectTaskWorkOrderEntity workOrder = workOrder(8001L, 7001L, 7201L, "OMS-20260427-0001");
    when(projectMapper.selectById(7001L)).thenReturn(project(7001L, "PRJ-2026-001", "Finance project", "in_progress"));
    when(projectTaskMapper.selectById(7201L)).thenReturn(task);
    when(projectTaskWorkOrderMapper.selectById(8001L)).thenReturn(workOrder);
    when(omsClient.getWorkOrder("OMS-20260427-0001")).thenReturn(new OmsClient.OmsWorkOrderSnapshot(
        "OMS-20260427-0001",
        "20",
        "已完成",
        true,
        "OMS handler completed the work order",
        "{\"status\":\"20\"}"
    ));
    when(omsClient.getWorkOrderLogs("OMS-20260427-0001")).thenReturn(List.of(
        new OmsClient.OmsWorkOrderLogSnapshot("2026-04-27T10:00:00", "Handler A", "complete", "submitted")
    ));
    when(omsClient.getWorkOrderAttachments("OMS-20260427-0001")).thenReturn(List.of(
        new OmsClient.OmsAttachmentSnapshot("file-1", "evidence.pdf", "/mock/evidence.pdf")
    ));

    var refreshed = projectService.refreshWorkOrder("7001", "7201", "8001");

    ArgumentCaptor<BizProjectTaskWorkOrderEntity> workOrderCaptor =
        ArgumentCaptor.forClass(BizProjectTaskWorkOrderEntity.class);
    verify(projectTaskWorkOrderMapper).updateById(workOrderCaptor.capture());
    BizProjectTaskWorkOrderEntity updated = workOrderCaptor.getValue();
    assertThat(refreshed.id()).isEqualTo("8001");
    assertThat(refreshed.omsStatus()).isEqualTo("20");
    assertThat(refreshed.reviewable()).isTrue();
    assertThat(updated.getSyncStatus()).isEqualTo("synced");
    assertThat(updated.getSyncError()).isNull();
    assertThat(updated.getLastSyncedAt()).isNotNull();
    assertThat(updated.getOmsDetailPayload()).contains("\"status\":\"20\"");
    assertThat(updated.getOmsLogPayload()).contains("submitted");
    assertThat(updated.getOmsAttachmentPayload()).contains("evidence.pdf");
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

  private BizProjectTaskWorkOrderEntity workOrder(Long id, Long projectId, Long taskId, String omsWorkOrderId) {
    BizProjectTaskWorkOrderEntity entity = new BizProjectTaskWorkOrderEntity();
    entity.setId(id);
    entity.setTenantId(1001L);
    entity.setProjectId(projectId);
    entity.setTaskId(taskId);
    entity.setOmsWorkOrderId(omsWorkOrderId);
    entity.setIdempotencyKey(taskId + ":201");
    entity.setHandlerId(201L);
    entity.setHandlerName("Handler A");
    entity.setWorkOrderTitle("Finance check");
    entity.setWorkOrderDescription("Handle in OMS");
    entity.setIrisReviewStatus("pending");
    entity.setReviewLocked(0);
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
