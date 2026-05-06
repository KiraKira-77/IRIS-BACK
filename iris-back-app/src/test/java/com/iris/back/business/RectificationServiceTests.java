package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.project.mapper.BizProjectRectificationMapper;
import com.iris.back.business.project.model.dto.RectificationDto;
import com.iris.back.business.project.model.entity.BizProjectRectificationEntity;
import com.iris.back.business.project.model.request.RectificationCreateRequest;
import com.iris.back.business.project.model.request.RectificationListQuery;
import com.iris.back.business.project.model.request.RectificationReviewRequest;
import com.iris.back.business.project.service.RectificationService;
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
  private CurrentUserContext currentUserContext;

  @Mock
  private IdentifierGenerator identifierGenerator;

  private RectificationService rectificationService;

  @BeforeEach
  void setUp() {
    rectificationService = new RectificationService(
        rectificationMapper,
        currentUserContext,
        identifierGenerator
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
  void reviewCanApproveSubmittedRectification() {
    mockCurrentUser();
    BizProjectRectificationEntity entity = rectification(9001L, "submitted");
    when(rectificationMapper.selectById(9001L)).thenReturn(entity);

    RectificationDto reviewed = rectificationService.review(
        "9001",
        new RectificationReviewRequest("approve", "Accepted")
    );

    verify(rectificationMapper).updateById(entity);
    assertThat(reviewed.status()).isEqualTo("approved");
    assertThat(reviewed.reviewComment()).isEqualTo("Accepted");
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
