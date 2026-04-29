package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.plan.mapper.BizPlanItemMapper;
import com.iris.back.business.plan.mapper.BizPlanMapper;
import com.iris.back.business.plan.model.dto.PlanDto;
import com.iris.back.business.plan.model.entity.BizPlanEntity;
import com.iris.back.business.plan.model.entity.BizPlanItemEntity;
import com.iris.back.business.plan.model.request.PlanItemUpsertRequest;
import com.iris.back.business.plan.model.request.PlanListQuery;
import com.iris.back.business.plan.model.request.PlanUpsertRequest;
import com.iris.back.business.plan.service.PlanService;
import com.iris.back.business.project.mapper.BizProjectMapper;
import com.iris.back.business.project.model.entity.BizProjectEntity;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlanServiceTests {

  @Mock
  private BizPlanMapper planMapper;

  @Mock
  private BizPlanItemMapper planItemMapper;

  @Mock
  private BizProjectMapper projectMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @InjectMocks
  private PlanService planService;

  @Test
  void listFiltersByKeywordYearStatusAndIncludesItems() {
    mockCurrentUser();
    BizPlanEntity visible = plan("9001", "PL-2026-001", "2026 annual control plan", "approved");
    BizPlanEntity hidden = plan("9002", "PL-2025-001", "2025 annual control plan", "draft");
    hidden.setPlanYear(2025);
    when(planMapper.selectList(any())).thenReturn(List.of(visible, hidden));
    when(planItemMapper.selectList(any())).thenReturn(List.of(item("9101", "9001")));

    var page = planService.list(new PlanListQuery("2026", 2026, "approved", 1L, 10L));

    assertThat(page.getTotal()).isEqualTo(1);
    assertThat(page.getRecords()).singleElement().satisfies(result -> {
      assertThat(result.id()).isEqualTo("9001");
      assertThat(result.ownerScopeId()).isEqualTo("9001");
      assertThat(result.grants()).containsExactly(new PlanDto.ScopeGrantDto("9002", List.of("view")));
      assertThat(result.items()).singleElement().satisfies(item -> {
        assertThat(item.targetScope()).isEqualTo("Finance");
        assertThat(item.checklistIds()).containsExactly("8801", "8802");
      });
    });
  }

  @Test
  void keepsDraftPlansAsDraftBeforeSubmission() {
    mockCurrentUser();
    BizPlanEntity draft = plan("9001", "PL-2026-001", "2026 annual control plan", "draft");
    when(planMapper.selectById(9001L)).thenReturn(draft);
    when(planItemMapper.selectList(any())).thenReturn(List.of());

    PlanDto result = planService.get("9001");

    assertThat(result.status()).isEqualTo("draft");
  }

  @Test
  void derivesApprovedForSubmittedPlanWithoutGeneratedProject() {
    mockCurrentUser();
    BizPlanEntity submitted = plan("9001", "PL-2026-001", "2026 annual control plan", "approved");
    when(planMapper.selectById(9001L)).thenReturn(submitted);
    when(planItemMapper.selectList(any())).thenReturn(List.of(item("9101", "9001")));

    PlanDto result = planService.get("9001");

    assertThat(result.status()).isEqualTo("approved");
  }

  @Test
  void derivesInProgressForSubmittedPlanWithGeneratedUnfinishedProject() {
    mockCurrentUser();
    BizPlanEntity submitted = plan("9001", "PL-2026-001", "2026 annual control plan", "approved");
    BizPlanItemEntity linkedItem = item("9101", "9001");
    linkedItem.setProjectId("9201");
    lenient().when(projectMapper.selectList(any())).thenReturn(List.of(project("9201", "in_progress", "none")));
    when(planMapper.selectById(9001L)).thenReturn(submitted);
    when(planItemMapper.selectList(any())).thenReturn(List.of(linkedItem));

    PlanDto result = planService.get("9001");

    assertThat(result.status()).isEqualTo("in_progress");
  }

  @Test
  void derivesCompletedForSubmittedPlanWithCompletedProject() {
    mockCurrentUser();
    BizPlanEntity submitted = plan("9001", "PL-2026-001", "2026 annual control plan", "approved");
    BizPlanItemEntity linkedItem = item("9101", "9001");
    linkedItem.setProjectId("9201");
    lenient().when(projectMapper.selectList(any())).thenReturn(List.of(project("9201", "completed", "none")));
    when(planMapper.selectById(9001L)).thenReturn(submitted);
    when(planItemMapper.selectList(any())).thenReturn(List.of(linkedItem));

    PlanDto result = planService.get("9001");

    assertThat(result.status()).isEqualTo("completed");
  }

  @Test
  void derivesArchivedForSubmittedPlanWithArchivedProject() {
    mockCurrentUser();
    BizPlanEntity submitted = plan("9001", "PL-2026-001", "2026 annual control plan", "approved");
    BizPlanItemEntity linkedItem = item("9101", "9001");
    linkedItem.setProjectId("9201");
    lenient().when(projectMapper.selectList(any())).thenReturn(List.of(project("9201", "completed", "completed")));
    when(planMapper.selectById(9001L)).thenReturn(submitted);
    when(planItemMapper.selectList(any())).thenReturn(List.of(linkedItem));

    PlanDto result = planService.get("9001");

    assertThat(result.status()).isEqualTo("archived");
  }

  @Test
  void derivesParentPlanStatusFromChildPlans() {
    mockCurrentUser();
    BizPlanEntity parent = plan("9001", "PL-2026-001", "2026 annual control plan", "approved");
    BizPlanEntity notStartedChild = childPlan("9002", "PL-2026-001-01", "Finance sub plan", "approved", "9001");
    BizPlanEntity inProgressChild = childPlan("9003", "PL-2026-001-02", "IT sub plan", "approved", "9001");
    BizPlanItemEntity linkedItem = item("9101", "9003");
    linkedItem.setProjectId("9201");
    when(planMapper.selectList(any())).thenReturn(List.of(parent, notStartedChild, inProgressChild));
    when(planItemMapper.selectList(any())).thenReturn(List.of(linkedItem));
    lenient().when(projectMapper.selectList(any())).thenReturn(List.of(project("9201", "in_progress", "none")));

    var page = planService.list(new PlanListQuery(null, 2026, null, 1L, 10L));

    assertThat(page.getRecords())
        .filteredOn(item -> item.id().equals("9001"))
        .singleElement()
        .satisfies(item -> assertThat(item.status()).isEqualTo("in_progress"));
  }

  @Test
  void derivesParentPlanAsCompletedWhenAllChildrenAreTerminalButNotAllArchived() {
    mockCurrentUser();
    BizPlanEntity parent = plan("9001", "PL-2026-001", "2026 annual control plan", "approved");
    BizPlanEntity completedChild = childPlan("9002", "PL-2026-001-01", "Finance sub plan", "approved", "9001");
    BizPlanEntity archivedChild = childPlan("9003", "PL-2026-001-02", "IT sub plan", "approved", "9001");
    BizPlanItemEntity completedItem = item("9101", "9002");
    completedItem.setProjectId("9201");
    BizPlanItemEntity archivedItem = item("9102", "9003");
    archivedItem.setProjectId("9202");
    when(planMapper.selectList(any())).thenReturn(List.of(parent, completedChild, archivedChild));
    when(planItemMapper.selectList(any())).thenReturn(List.of(completedItem, archivedItem));
    lenient().when(projectMapper.selectList(any())).thenReturn(List.of(
        project("9201", "completed", "none"),
        project("9202", "completed", "completed")
    ));

    var page = planService.list(new PlanListQuery(null, 2026, null, 1L, 10L));

    assertThat(page.getRecords())
        .filteredOn(item -> item.id().equals("9001"))
        .singleElement()
        .satisfies(item -> assertThat(item.status()).isEqualTo("completed"));
  }

  @Test
  void derivesParentPlanAsArchivedWhenAllChildrenAreArchived() {
    mockCurrentUser();
    BizPlanEntity parent = plan("9001", "PL-2026-001", "2026 annual control plan", "approved");
    BizPlanEntity firstChild = childPlan("9002", "PL-2026-001-01", "Finance sub plan", "approved", "9001");
    BizPlanEntity secondChild = childPlan("9003", "PL-2026-001-02", "IT sub plan", "approved", "9001");
    BizPlanItemEntity firstItem = item("9101", "9002");
    firstItem.setProjectId("9201");
    BizPlanItemEntity secondItem = item("9102", "9003");
    secondItem.setProjectId("9202");
    when(planMapper.selectList(any())).thenReturn(List.of(parent, firstChild, secondChild));
    when(planItemMapper.selectList(any())).thenReturn(List.of(firstItem, secondItem));
    lenient().when(projectMapper.selectList(any())).thenReturn(List.of(
        project("9201", "completed", "completed"),
        project("9202", "archived", "completed")
    ));

    var page = planService.list(new PlanListQuery(null, 2026, null, 1L, 10L));

    assertThat(page.getRecords())
        .filteredOn(item -> item.id().equals("9001"))
        .singleElement()
        .satisfies(item -> assertThat(item.status()).isEqualTo("archived"));
  }

  @Test
  void formatsPlanTimestampsWithoutIsoSeparator() {
    mockCurrentUser();
    BizPlanEntity plan = plan("9001", "PL-2026-001", "2026 annual control plan", "approved");
    plan.setCreatedAt(LocalDateTime.of(2026, 4, 29, 13, 6, 50));
    plan.setUpdatedAt(LocalDateTime.of(2026, 4, 29, 13, 7, 8));
    when(planMapper.selectById(9001L)).thenReturn(plan);
    when(planItemMapper.selectList(any())).thenReturn(List.of());

    PlanDto result = planService.get("9001");

    assertThat(result.createdAt()).isEqualTo("2026-04-29 13:06:50");
    assertThat(result.updatedAt()).isEqualTo("2026-04-29 13:07:08");
  }

  @Test
  void createPersistsPlanAndItemsForCurrentTenant() {
    mockCurrentUser();
    when(identifierGenerator.nextId(any()))
        .thenReturn(9001L)
        .thenReturn(9101L);

    PlanDto created = planService.create(new PlanUpsertRequest(
        "PL-2026-001",
        "2026 annual control plan",
        "yearly",
        2026,
        "full-year",
        "draft",
        "annual scope",
        "9001",
        List.of("9001", "9002"),
        null,
        List.of(new PlanItemUpsertRequest(
            null,
            "Finance",
            List.of("8801", "8802"),
            "2026-01-01",
            "2026-12-31",
            "2001",
            "focus on payments",
            null
        ))
    ));

    ArgumentCaptor<BizPlanEntity> planCaptor = ArgumentCaptor.forClass(BizPlanEntity.class);
    ArgumentCaptor<BizPlanItemEntity> itemCaptor = ArgumentCaptor.forClass(BizPlanItemEntity.class);
    verify(planMapper).insert(planCaptor.capture());
    verify(planItemMapper).insert(itemCaptor.capture());

    assertThat(created.id()).isEqualTo("9001");
    assertThat(created.status()).isEqualTo("draft");
    assertThat(planCaptor.getValue().getTenantId()).isEqualTo(1001L);
    assertThat(planCaptor.getValue().getOwnerScopeId()).isEqualTo(9001L);
    assertThat(planCaptor.getValue().getSharedScopeIds()).isEqualTo("9002");
    assertThat(planCaptor.getValue().getCreatedBy()).isEqualTo(2001L);
    assertThat(itemCaptor.getValue().getPlanId()).isEqualTo(9001L);
    assertThat(itemCaptor.getValue().getChecklistIds()).isEqualTo("8801,8802");
    assertThat(itemCaptor.getValue().getPlannedStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
  }

  @Test
  void updateRebuildsItemsAndSubmitApproveAdvanceStatus() {
    mockCurrentUser();
    BizPlanEntity existing = plan("9001", "PL-2026-001", "2026 annual control plan", "draft");
    when(planMapper.selectById(9001L)).thenReturn(existing);
    when(identifierGenerator.nextId(any()))
        .thenReturn(9102L)
        .thenReturn(9103L);

    planService.update("9001", new PlanUpsertRequest(
        "PL-2026-001",
        "2026 annual control plan updated",
        "yearly",
        2026,
        "full-year",
        "draft",
        "annual scope",
        "9001",
        List.of("9002"),
        null,
        List.of(
            new PlanItemUpsertRequest(null, "Finance", List.of("8801"), "2026-01-01", "2026-06-30", null, null, null),
            new PlanItemUpsertRequest(null, "IT", List.of("8802"), "2026-07-01", "2026-12-31", "2002", null, null)
        )
    ));

    ArgumentCaptor<BizPlanItemEntity> itemCaptor = ArgumentCaptor.forClass(BizPlanItemEntity.class);
    verify(planItemMapper, times(2)).insert(itemCaptor.capture());
    assertThat(itemCaptor.getAllValues())
        .extracting(BizPlanItemEntity::getSequenceNo)
        .containsExactly(1, 2);

    PlanDto submitted = planService.submit("9001");
    assertThat(submitted.status()).isEqualTo("approved");
    assertThat(existing.getApprovedBy()).isEqualTo(2001L);

    PlanDto approved = planService.approve("9001");
    assertThat(approved.status()).isEqualTo("approved");
    assertThat(existing.getApprovedBy()).isEqualTo(2001L);
  }

  private BizPlanEntity plan(String id, String code, String name, String status) {
    BizPlanEntity entity = new BizPlanEntity();
    entity.setId(Long.valueOf(id));
    entity.setTenantId(1001L);
    entity.setPlanCode(code);
    entity.setPlanName(name);
    entity.setCycle("yearly");
    entity.setPlanYear(2026);
    entity.setPeriod("full-year");
    entity.setStatus(status);
    entity.setDescription("annual scope");
    entity.setOwnerScopeId(9001L);
    entity.setSharedScopeIds("9002");
    entity.setCreatedBy(2001L);
    entity.setUpdatedBy(2001L);
    return entity;
  }

  private BizPlanEntity childPlan(String id, String code, String name, String status, String parentId) {
    BizPlanEntity entity = plan(id, code, name, status);
    entity.setParentId(Long.valueOf(parentId));
    return entity;
  }

  private BizPlanItemEntity item(String id, String planId) {
    BizPlanItemEntity entity = new BizPlanItemEntity();
    entity.setId(Long.valueOf(id));
    entity.setTenantId(1001L);
    entity.setPlanId(Long.valueOf(planId));
    entity.setSequenceNo(1);
    entity.setTargetScope("Finance");
    entity.setChecklistIds("8801,8802");
    entity.setPlannedStartDate(LocalDate.of(2026, 1, 1));
    entity.setPlannedEndDate(LocalDate.of(2026, 12, 31));
    entity.setAssignee("2001");
    return entity;
  }

  private BizProjectEntity project(String id, String status, String archiveStatus) {
    BizProjectEntity entity = new BizProjectEntity();
    entity.setId(Long.valueOf(id));
    entity.setTenantId(1001L);
    entity.setProjectCode("PRJ-" + id);
    entity.setProjectName("Project " + id);
    entity.setStatus(status);
    entity.setArchiveStatus(archiveStatus);
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
