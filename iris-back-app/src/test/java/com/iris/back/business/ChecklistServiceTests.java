package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
import com.iris.back.business.checklist.model.dto.ChecklistDto;
import com.iris.back.business.checklist.model.entity.BizChecklistEntity;
import com.iris.back.business.checklist.model.entity.BizChecklistItemEntity;
import com.iris.back.business.checklist.model.request.ChecklistItemUpsertRequest;
import com.iris.back.business.checklist.model.request.ChecklistListQuery;
import com.iris.back.business.checklist.model.request.ChecklistUpsertRequest;
import com.iris.back.business.checklist.service.ChecklistService;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.model.dto.ResourceScopeMemberDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ChecklistServiceTests {

  @Mock
  private BizChecklistMapper checklistMapper;

  @Mock
  private BizChecklistItemMapper checklistItemMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  @Mock
  private SysResourceScopeMemberMapper resourceScopeMemberMapper;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @InjectMocks
  private ChecklistService checklistService;

  @Test
  void listFiltersByKeywordStatusAndScopeAndIncludesItems() {
    mockCurrentUser();
    BizChecklistEntity checklist = checklist("8801", "CL-2026-001", "资金活动内控检查清单");
    checklist.setOwnerScopeId(9001L);
    checklist.setSharedScopeIds("9002");
    checklist.setStatus("active");
    BizChecklistEntity hidden = checklist("8802", "CL-2026-002", "采购业务内控检查清单");
    hidden.setOwnerScopeId(9003L);
    hidden.setStatus("draft");
    when(checklistMapper.selectList(any())).thenReturn(List.of(checklist, hidden));
    when(checklistItemMapper.selectList(any())).thenReturn(List.of(item("9901", "8801")));

    var page = checklistService.list(new ChecklistListQuery("资金", "active", "9001", 1L, 10L));

    assertThat(page.getTotal()).isEqualTo(1);
    assertThat(page.getRecords()).singleElement().satisfies(result -> {
      assertThat(result.id()).isEqualTo("8801");
      assertThat(result.code()).isEqualTo("CL-2026-001");
      assertThat(result.ownerScopeId()).isEqualTo("9001");
      assertThat(result.grants()).containsExactly(new ChecklistDto.ScopeGrantDto("9002", List.of("view")));
      assertThat(result.items()).singleElement().satisfies(item -> {
        assertThat(item.controlFrequency()).isEqualTo("monthly");
        assertThat(item.organizationIds()).containsExactly("org-finance");
      });
    });
  }

  @Test
  void listScopeFilterOnlyMatchesOwnerScope() {
    mockCurrentUser();
    BizChecklistEntity sharedOnly = checklist("8801", "CL-2026-001", "Shared only checklist");
    sharedOnly.setOwnerScopeId(9001L);
    sharedOnly.setSharedScopeIds("9002");
    sharedOnly.setStatus("active");
    BizChecklistEntity ownerMatched = checklist("8802", "CL-2026-002", "Owner matched checklist");
    ownerMatched.setOwnerScopeId(9002L);
    ownerMatched.setSharedScopeIds("");
    ownerMatched.setStatus("active");
    when(checklistMapper.selectList(any())).thenReturn(List.of(sharedOnly, ownerMatched));
    when(checklistItemMapper.selectList(any())).thenReturn(List.of());

    var page = checklistService.list(new ChecklistListQuery(null, null, "9002", 1L, 10L));

    assertThat(page.getRecords())
        .extracting(ChecklistDto::id)
        .containsExactly("8802");
  }

  @Test
  void listReturnsOnlyChecklistsVisibleToCurrentUserResourceScopes() {
    mockCurrentUser(2004L, List.of("AUDITOR"));
    BizChecklistEntity ownerVisible = checklist("8801", "CL-2026-001", "Owner visible checklist");
    ownerVisible.setOwnerScopeId(9001L);
    ownerVisible.setStatus("active");
    BizChecklistEntity sharedVisible = checklist("8802", "CL-2026-002", "Shared visible checklist");
    sharedVisible.setOwnerScopeId(9008L);
    sharedVisible.setSharedScopeIds("9002");
    sharedVisible.setStatus("active");
    BizChecklistEntity hidden = checklist("8803", "CL-2026-003", "Hidden checklist");
    hidden.setOwnerScopeId(9010L);
    hidden.setSharedScopeIds("9011");
    hidden.setStatus("active");
    when(checklistMapper.selectList(any())).thenReturn(List.of(ownerVisible, sharedVisible, hidden));
    when(checklistItemMapper.selectList(any())).thenReturn(List.of());
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2004L)).thenReturn(List.of(
        scopeMember(9001L, 2004L, 1, 0, 0, 0, 0),
        scopeMember(9002L, 2004L, 1, 0, 0, 0, 0)
    ));

    var page = checklistService.list(new ChecklistListQuery(null, null, null, 1L, 10L));

    assertThat(page.getRecords())
        .extracting(ChecklistDto::id)
        .containsExactly("8801", "8802");
  }

  @Test
  void createRequiresCreatePermissionInOwnerScope() {
    mockCurrentUser(2004L, List.of("AUDITOR"));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2004L)).thenReturn(List.of(
        scopeMember(9001L, 2004L, 1, 0, 0, 0, 0)
    ));

    assertThatThrownBy(() -> checklistService.create(request("9001", List.of())))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("create checklist");

    verify(checklistMapper, never()).insert(any(BizChecklistEntity.class));
  }

  @Test
  void updateRequiresEditPermissionInOwnerScope() {
    mockCurrentUser(2004L, List.of("AUDITOR"));
    BizChecklistEntity existing = checklist("8801", "CL-2026-001", "Owner visible checklist");
    existing.setOwnerScopeId(9001L);
    existing.setStatus("active");
    when(checklistMapper.selectById(8801L)).thenReturn(existing);
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2004L)).thenReturn(List.of(
        scopeMember(9001L, 2004L, 1, 0, 0, 0, 0)
    ));

    assertThatThrownBy(() -> checklistService.update("8801", request("9001", List.of())))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("edit checklist");

    verify(checklistMapper, never()).updateById(any(BizChecklistEntity.class));
  }

  @Test
  void deleteRequiresDeletePermissionInOwnerScope() {
    mockCurrentUser(2004L, List.of("AUDITOR"));
    BizChecklistEntity existing = checklist("8801", "CL-2026-001", "Owner visible checklist");
    existing.setOwnerScopeId(9001L);
    existing.setStatus("active");
    when(checklistMapper.selectById(8801L)).thenReturn(existing);
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2004L)).thenReturn(List.of(
        scopeMember(9001L, 2004L, 1, 0, 0, 0, 0)
    ));

    assertThatThrownBy(() -> checklistService.delete("8801"))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("delete checklist");

    verify(checklistMapper, never()).deleteById(any());
  }

  @Test
  void formatsTimestampsWithoutIsoSeparator() {
    mockCurrentUser();
    BizChecklistEntity checklist = checklist("8801", "CL-2026-001", "Finance checklist");
    checklist.setOwnerScopeId(9001L);
    checklist.setStatus("active");
    checklist.setCreatedAt(LocalDateTime.of(2026, 4, 29, 13, 6, 50));
    checklist.setUpdatedAt(LocalDateTime.of(2026, 4, 29, 13, 7, 8));
    when(checklistMapper.selectList(any())).thenReturn(List.of(checklist));
    when(checklistItemMapper.selectList(any())).thenReturn(List.of());

    var page = checklistService.list(new ChecklistListQuery(null, null, null, 1L, 10L));

    assertThat(page.getRecords()).singleElement().satisfies(item -> {
      assertThat(item.createdAt()).isEqualTo("2026-04-29 13:06:50");
      assertThat(item.updatedAt()).isEqualTo("2026-04-29 13:07:08");
    });
  }

  @Test
  void createPersistsChecklistAndItemsForCurrentTenant() {
    mockCurrentUser();
    when(identifierGenerator.nextId(any()))
        .thenReturn(8801L)
        .thenReturn(9901L);

    var created = checklistService.create(new ChecklistUpsertRequest(
        "CL-2026-001",
        "资金活动内控检查清单",
        "资金活动控制点",
        "V1.0",
        "9001",
        List.of("9002"),
        "active",
        "2026-04-27",
        List.of(new ChecklistItemUpsertRequest(
            null,
            "银行对账是否及时完成",
            "每月5日前完成上月银行余额调节表",
            "monthly",
            "operation",
            List.of("org-finance")
        ))
    ));

    ArgumentCaptor<BizChecklistEntity> checklistCaptor = ArgumentCaptor.forClass(BizChecklistEntity.class);
    ArgumentCaptor<BizChecklistItemEntity> itemCaptor = ArgumentCaptor.forClass(BizChecklistItemEntity.class);
    verify(checklistMapper).insert(checklistCaptor.capture());
    verify(checklistItemMapper).insert(itemCaptor.capture());

    assertThat(created.id()).isEqualTo("8801");
    assertThat(created.uploadDate()).isEqualTo("2026-04-27");
    assertThat(created.items()).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo("9901"));
    assertThat(checklistCaptor.getValue().getTenantId()).isEqualTo(1001L);
    assertThat(checklistCaptor.getValue().getOwnerScopeId()).isEqualTo(9001L);
    assertThat(checklistCaptor.getValue().getSharedScopeIds()).isEqualTo("9002");
    assertThat(checklistCaptor.getValue().getCreatedBy()).isEqualTo(2001L);
    assertThat(itemCaptor.getValue().getChecklistId()).isEqualTo(8801L);
    assertThat(itemCaptor.getValue().getSequenceNo()).isEqualTo(1);
    assertThat(itemCaptor.getValue().getOrganizationIds()).isEqualTo("org-finance");
  }

  @Test
  void updatePreservesExistingItemIdsAndOnlyGeneratesIdsForNewItems() {
    mockCurrentUser();
    BizChecklistEntity existing = checklist("8801", "CL-2026-001", "资金活动内控检查清单");
    existing.setOwnerScopeId(9001L);
    existing.setStatus("active");
    when(checklistMapper.selectById(8801L)).thenReturn(existing);
    when(checklistItemMapper.selectList(any())).thenReturn(List.of(item("9901", "8801")));
    when(identifierGenerator.nextId(any()))
        .thenReturn(9902L);

    var updated = checklistService.update("8801", new ChecklistUpsertRequest(
        "CL-2026-001",
        "资金活动内控检查清单",
        "资金活动控制点",
        "V1.0",
        "9001",
        List.of("9002"),
        "active",
        "2026-04-27",
        List.of(
            new ChecklistItemUpsertRequest(
                "9901",
                "银行对账是否及时完成",
                "每月5日前完成上月银行余额调节表",
                "monthly",
                "operation",
                List.of("org-finance")
            ),
            new ChecklistItemUpsertRequest(
                null,
                "付款审批是否完整",
                "付款前完成经办、复核、审批",
                "per-occurrence",
                "compliance",
                List.of("org-finance", "org-audit")
            )
        )
    ));

    ArgumentCaptor<BizChecklistItemEntity> itemCaptor = ArgumentCaptor.forClass(BizChecklistItemEntity.class);
    verify(checklistItemMapper).updateById(itemCaptor.capture());

    assertThat(itemCaptor.getValue().getId()).isEqualTo(9901L);
    assertThat(itemCaptor.getValue().getContent()).isEqualTo("银行对账是否及时完成");
    assertThat(updated.items())
        .extracting(item -> item.id())
        .containsExactly("9901", "9902");
  }

  private BizChecklistEntity checklist(String id, String code, String name) {
    BizChecklistEntity entity = new BizChecklistEntity();
    entity.setId(Long.valueOf(id));
    entity.setTenantId(1001L);
    entity.setChecklistCode(code);
    entity.setChecklistName(name);
    entity.setDescription("desc");
    entity.setChecklistVersion("V1.0");
    entity.setSharedScopeIds("");
    entity.setUploadDate(LocalDate.of(2026, 4, 27));
    entity.setCreatedBy(2001L);
    entity.setUpdatedBy(2001L);
    return entity;
  }

  private BizChecklistItemEntity item(String id, String checklistId) {
    BizChecklistItemEntity entity = new BizChecklistItemEntity();
    entity.setId(Long.valueOf(id));
    entity.setTenantId(1001L);
    entity.setChecklistId(Long.valueOf(checklistId));
    entity.setSequenceNo(1);
    entity.setContent("银行对账是否及时完成");
    entity.setCriterion("每月5日前完成上月银行余额调节表");
    entity.setControlFrequency("monthly");
    entity.setEvaluationType("operation");
    entity.setOrganizationIds("org-finance");
    return entity;
  }

  private ChecklistUpsertRequest request(String ownerScopeId, List<String> grantScopeIds) {
    return new ChecklistUpsertRequest(
        "CL-2026-001",
        "Finance checklist",
        "desc",
        "V1.0",
        ownerScopeId,
        grantScopeIds,
        "active",
        "2026-04-27",
        List.of()
    );
  }

  private ResourceScopeMemberDto scopeMember(
      Long scopeId,
      Long userId,
      Integer canView,
      Integer canCreate,
      Integer canEdit,
      Integer canDelete,
      Integer canManage
  ) {
    return new ResourceScopeMemberDto(
        "1",
        String.valueOf(scopeId),
        String.valueOf(userId),
        "user",
        "User",
        canView,
        canCreate,
        canEdit,
        canDelete,
        canManage,
        "test"
    );
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

  private void mockCurrentUser(Long userId, List<String> roles) {
    when(currentUserContext.requireCurrentUser()).thenReturn(new CurrentUserPrincipal(
        "token",
        userId,
        1001L,
        "user",
        "User",
        "IRIS",
        roles
    ));
  }
}
