package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.project.mapper.BizProjectMemberMapper;
import com.iris.back.business.project.mapper.BizProjectRectificationMapper;
import com.iris.back.business.project.model.dto.RectificationDto;
import com.iris.back.business.project.model.entity.BizProjectMemberEntity;
import com.iris.back.business.project.model.entity.BizProjectRectificationEntity;
import com.iris.back.business.project.model.request.ProjectWorkOrderReturnRequest;
import com.iris.back.business.project.model.request.RectificationCreateRequest;
import com.iris.back.business.project.model.request.RectificationListQuery;
import com.iris.back.business.project.model.request.RectificationReviewRequest;
import com.iris.back.business.project.service.OmsClient;
import com.iris.back.business.project.service.RectificationService;
import com.iris.back.common.exception.BusinessException;
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
class RectificationServiceTests {

  @Mock
  private BizProjectRectificationMapper rectificationMapper;

  @Mock
  private BizProjectMemberMapper projectMemberMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @Mock
  private OmsClient omsClient;

  private RectificationService rectificationService;

  @BeforeEach
  void setUp() {
    rectificationService = new RectificationService(
        rectificationMapper,
        projectMemberMapper,
        currentUserContext,
        identifierGenerator,
        omsClient
    );
  }

  @Test
  void listReturnsPagedRectificationsForCurrentTenant() {
    mockCurrentUser();
    when(rectificationMapper.selectList(any())).thenReturn(List.of(rectification(9001L, "pending")));

    var page = rectificationService.list(new RectificationListQuery(null, "pending", null, null, 1L, 10L));

    assertThat(page.getTotal()).isEqualTo(1);
    assertThat(page.getRecords()).singleElement().satisfies(item -> {
      assertThat(item.id()).isEqualTo("9001");
      assertThat(item.code()).isEqualTo("RECT-9001");
      assertThat(item.source()).isEqualTo("task");
      assertThat(item.status()).isEqualTo("pending");
    });
  }

  @Test
  void createManualRectificationStoresMinimalFields() {
    mockCurrentUser();
    when(identifierGenerator.nextId(any())).thenReturn(9101L);

    RectificationDto created = rectificationService.create(new RectificationCreateRequest(
        "Manual issue",
        "Missing evidence",
        "7001",
        "Finance project",
        "7201",
        "2002",
        "Auditor",
        "2003",
        "Reviewer",
        "2026-05-20"
    ));

    ArgumentCaptor<BizProjectRectificationEntity> captor =
        ArgumentCaptor.forClass(BizProjectRectificationEntity.class);
    verify(rectificationMapper).insert(captor.capture());
    assertThat(created.id()).isEqualTo("9101");
    assertThat(created.source()).isEqualTo("manual");
    assertThat(created.status()).isEqualTo("pending");
    assertThat(captor.getValue().getSourceWorkOrderRecordId()).isNull();
    assertThat(captor.getValue().getContactId()).isEqualTo(2003L);
  }

  @Test
  void submitMovesPendingRectificationToSubmitted() {
    mockCurrentUser();
    BizProjectRectificationEntity entity = rectification(9001L, "pending");
    when(rectificationMapper.selectById(9001L)).thenReturn(entity);

    RectificationDto submitted = rectificationService.submit("9001");

    verify(rectificationMapper).updateById(entity);
    assertThat(submitted.status()).isEqualTo("submitted");
  }

  @Test
  void createWorkOrderCreatesOneOmsTicketForPendingRectification() {
    mockCurrentUser();
    BizProjectRectificationEntity entity = rectification(9001L, "pending");
    when(rectificationMapper.selectById(9001L)).thenReturn(entity);
    when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(2002L, "EMP002", "Auditor")));
    when(omsClient.createWorkOrders(any(), any())).thenReturn(List.of(
        new OmsClient.OmsCreateResult("2002", "OMS-RECT-001", "created", null, "{}")
    ));

    RectificationDto created = rectificationService.createWorkOrder("9001");

    verify(rectificationMapper).updateById(entity);
    assertThat(created.status()).isEqualTo("in_progress");
    assertThat(created.rectificationOmsWorkOrderId()).isEqualTo("OMS-RECT-001");
    assertThat(entity.getRectificationOmsWorkOrderId()).isEqualTo("OMS-RECT-001");
    assertThat(entity.getRectificationWorkOrderCreatedAt()).isNotNull();
    ArgumentCaptor<List<OmsClient.OmsCreateCommand>> commandCaptor = ArgumentCaptor.forClass(List.class);
    verify(omsClient).createWorkOrders(any(), commandCaptor.capture());
    assertThat(commandCaptor.getValue()).singleElement().satisfies(command -> {
      assertThat(command.handlerId()).isEqualTo("2002");
      assertThat(command.handlerEmployeeNo()).isEqualTo("EMP002");
      assertThat(command.idempotencyKey()).isEqualTo("rectification:9001");
      assertThat(command.localWorkOrderId()).isEqualTo(9001L);
    });
  }

  @Test
  void getRefreshesInProgressRectificationOmsStatus() {
    mockCurrentUser();
    BizProjectRectificationEntity entity = rectification(9001L, "in_progress");
    entity.setRectificationOmsWorkOrderId("OMS-RECT-001");
    entity.setRectificationOmsStatus("10");
    entity.setRectificationOmsStatusName("pending");
    when(rectificationMapper.selectById(9001L)).thenReturn(entity);
    when(omsClient.getWorkOrder("OMS-RECT-001")).thenReturn(new OmsClient.OmsWorkOrderSnapshot(
        "OMS-RECT-001",
        "20",
        "completed",
        true,
        "Done",
        "{\"status\":\"20\"}"
    ));

    RectificationDto detail = rectificationService.get("9001");

    verify(rectificationMapper).updateById(entity);
    assertThat(detail.rectificationOmsStatus()).isEqualTo("20");
    assertThat(detail.rectificationOmsStatusName()).isEqualTo("completed");
    assertThat(detail.rectificationWorkOrderCompletedAt()).isNotNull();
  }

  @Test
  void returnWorkOrderSendsCompletedRectificationOmsTicketBackForRework() {
    mockCurrentUser();
    BizProjectRectificationEntity entity = rectification(9001L, "in_progress");
    entity.setRectificationOmsWorkOrderId("OMS-RECT-001");
    entity.setRectificationOmsStatus("20");
    entity.setRectificationOmsStatusName("已完成");
    entity.setRectificationWorkOrderCompletedAt(LocalDateTime.of(2026, 5, 8, 10, 30));
    when(rectificationMapper.selectById(9001L)).thenReturn(entity);
    when(omsClient.getWorkOrder("OMS-RECT-001")).thenReturn(new OmsClient.OmsWorkOrderSnapshot(
        "OMS-RECT-001",
        "10",
        "待领取",
        false,
        "Returned for rework",
        "{\"status\":\"10\"}"
    ));

    RectificationDto returned = rectificationService.returnWorkOrder(
        "9001",
        new ProjectWorkOrderReturnRequest("整改证据不足")
    );

    verify(omsClient).returnWorkOrder("OMS-RECT-001", "整改证据不足");
    verify(rectificationMapper).updateById(entity);
    assertThat(returned.status()).isEqualTo("in_progress");
    assertThat(returned.rectificationOmsStatusName()).isEqualTo("待领取");
    assertThat(entity.getRectificationWorkOrderCompletedAt()).isNull();
  }

  @Test
  void reviewRefreshesOmsStatusBeforeCheckingCompletion() {
    mockCurrentUser();
    BizProjectRectificationEntity entity = rectification(9001L, "in_progress");
    entity.setRectificationOmsWorkOrderId("OMS-RECT-001");
    entity.setRectificationOmsStatus("10");
    entity.setRectificationOmsStatusName("pending");
    when(rectificationMapper.selectById(9001L)).thenReturn(entity);
    when(omsClient.getWorkOrder("OMS-RECT-001")).thenReturn(new OmsClient.OmsWorkOrderSnapshot(
        "OMS-RECT-001",
        "20",
        "completed",
        true,
        "Done",
        "{\"status\":\"20\"}"
    ));

    RectificationDto reviewed = rectificationService.review(
        "9001",
        new RectificationReviewRequest("approve", "Accepted")
    );

    verify(rectificationMapper).updateById(entity);
    assertThat(reviewed.status()).isEqualTo("approved");
    assertThat(reviewed.reviewResult()).isEqualTo("approve");
    assertThat(entity.getRectificationWorkOrderCompletedAt()).isNotNull();
  }

  @Test
  void reviewRejectsOrApprovesCompletedRectificationWorkOrderAndLocksOrder() {
    mockCurrentUser();
    BizProjectRectificationEntity entity = rectification(9001L, "in_progress");
    entity.setRectificationOmsWorkOrderId("OMS-RECT-001");
    entity.setRectificationOmsStatus("20");
    entity.setRectificationOmsStatusName("已完成");
    when(rectificationMapper.selectById(9001L)).thenReturn(entity);

    RectificationDto reviewed = rectificationService.review(
        "9001",
        new RectificationReviewRequest("reject", "仍不满足要求")
    );

    verify(omsClient, never()).returnWorkOrder(any(), any());
    verify(rectificationMapper).updateById(entity);
    assertThat(reviewed.status()).isEqualTo("approved");
    assertThat(reviewed.reviewResult()).isEqualTo("reject");
    assertThat(reviewed.reviewComment()).isEqualTo("仍不满足要求");
    assertThat(entity.getCompletedAt()).isNotNull();
  }

  @Test
  void reviewRejectsAlreadyReviewedRectification() {
    mockCurrentUser();
    BizProjectRectificationEntity entity = rectification(9001L, "approved");
    when(rectificationMapper.selectById(9001L)).thenReturn(entity);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> rectificationService.review(
        "9001",
        new RectificationReviewRequest("approve", "Accepted")
    ))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("RECTIFICATION_REVIEW_STATUS_INVALID");

    verify(rectificationMapper, never()).updateById(any(BizProjectRectificationEntity.class));
  }

  private BizProjectRectificationEntity rectification(Long id, String status) {
    BizProjectRectificationEntity entity = new BizProjectRectificationEntity();
    entity.setId(id);
    entity.setTenantId(1001L);
    entity.setRectificationCode("RECT-" + id);
    entity.setTitle("Rectification " + id);
    entity.setDescription("Fix issue");
    entity.setProjectId(7001L);
    entity.setProjectName("Finance project");
    entity.setTaskId(7201L);
    entity.setChecklistItemId(9901L);
    entity.setCheckContent("Check content");
    entity.setSourceWorkOrderRecordId(8001L);
    entity.setAssigneeId(2002L);
    entity.setAssigneeName("Auditor");
    entity.setContactId(2003L);
    entity.setContactName("Reviewer");
    entity.setIssuedAt(LocalDateTime.of(2026, 5, 6, 9, 0));
    entity.setDeadline(LocalDateTime.of(2026, 5, 20, 18, 0));
    entity.setStatus(status);
    entity.setCreatedAt(LocalDateTime.of(2026, 5, 6, 9, 0));
    entity.setUpdatedAt(LocalDateTime.of(2026, 5, 6, 9, 0));
    return entity;
  }

  private BizProjectMemberEntity member(Long personnelId, String employeeNo, String personnelName) {
    BizProjectMemberEntity member = new BizProjectMemberEntity();
    member.setTenantId(1001L);
    member.setProjectId(7001L);
    member.setPersonnelId(personnelId);
    member.setEmployeeNo(employeeNo);
    member.setPersonnelName(personnelName);
    member.setRole("auditor");
    return member;
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
