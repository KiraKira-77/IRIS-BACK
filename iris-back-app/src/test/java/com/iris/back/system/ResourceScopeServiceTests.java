package com.iris.back.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import com.iris.back.system.mapper.SysResourceScopeMapper;
import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.mapper.SysResourceScopeUsageMapper;
import com.iris.back.system.model.dto.ResourceScopeMemberDto;
import com.iris.back.system.model.entity.SysResourceScopeEntity;
import com.iris.back.system.model.entity.SysResourceScopeMemberEntity;
import com.iris.back.system.model.request.ResourceScopeMemberReplaceRequest;
import com.iris.back.system.model.request.ResourceScopeMemberUpsertRequest;
import com.iris.back.system.model.request.ResourceScopeUpsertRequest;
import com.iris.back.system.service.ResourceScopeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceScopeServiceTests {

  @Mock
  private SysResourceScopeMapper resourceScopeMapper;

  @Mock
  private SysResourceScopeMemberMapper resourceScopeMemberMapper;

  @Mock
  private SysResourceScopeUsageMapper resourceScopeUsageMapper;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @Mock
  private CurrentUserContext currentUserContext;

  @InjectMocks
  private ResourceScopeService resourceScopeService;

  @Test
  void createAssignsGeneratedIdBeforeInsert() {
    when(identifierGenerator.nextId(any())).thenReturn(9101001L);

    var created = resourceScopeService.create(new ResourceScopeUpsertRequest(
        1001L, "FINANCE", "Finance Scope", "RESOURCE", 1, "created in test"
    ));

    ArgumentCaptor<SysResourceScopeEntity> captor = ArgumentCaptor.forClass(SysResourceScopeEntity.class);
    verify(resourceScopeMapper).insert(captor.capture());

    assertThat(created.id()).isEqualTo("9101001");
    assertThat(captor.getValue().getId()).isEqualTo(9101001L);
    assertThat(captor.getValue().getScopeCode()).isEqualTo("FINANCE");
  }

  @Test
  void replaceMembersRewritesScopeMembersWithPermissionFlags() {
    SysResourceScopeEntity scope = new SysResourceScopeEntity();
    scope.setId(9101L);
    scope.setTenantId(1001L);
    scope.setScopeCode("FINANCE");
    scope.setScopeName("Finance Scope");
    scope.setScopeType("RESOURCE");
    scope.setStatus(1);
    when(resourceScopeMapper.selectById(9101L)).thenReturn(scope);
    when(identifierGenerator.nextId(any()))
        .thenReturn(9201001L)
        .thenReturn(9201002L);

    resourceScopeService.replaceMembers(9101L, new ResourceScopeMemberReplaceRequest(List.of(
        new ResourceScopeMemberUpsertRequest(2001L, true, true, true, false, true, "admin member"),
        new ResourceScopeMemberUpsertRequest(2002L, true, false, false, false, false, "readonly member")
    )));

    ArgumentCaptor<List<SysResourceScopeMemberEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(resourceScopeMemberMapper).replaceForScope(eq(9101L), captor.capture());

    assertThat(captor.getValue()).hasSize(2);
    assertThat(captor.getValue().getFirst().getCanManage()).isEqualTo(1);
    assertThat(captor.getValue().get(1).getCanEdit()).isEqualTo(0);
  }

  @Test
  void listCurrentUserMembershipsUsesCurrentUserContext() {
    when(currentUserContext.requireCurrentUser()).thenReturn(new CurrentUserPrincipal(
        "token-1",
        2047157959175438300L,
        1001L,
        "00320283",
        "Finance User",
        "Default Tenant",
        List.of("AUDITOR")
    ));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2047157959175438300L)).thenReturn(List.of(
        new ResourceScopeMemberDto(
            "9104",
            "9001",
            "2047157959175438300",
            "00320283",
            "Finance User",
            1,
            1,
            1,
            0,
            0,
            "finance member"
        )
    ));

    var memberships = resourceScopeService.listCurrentUserMemberships();

    assertThat(memberships).hasSize(1);
    assertThat(memberships.getFirst().scopeId()).isEqualTo("9001");
    verify(resourceScopeMemberMapper).selectByTenantIdAndUserId(1001L, 2047157959175438300L);
  }

  @Test
  void replaceMembersDeduplicatesUsersWithinSingleRequest() {
    SysResourceScopeEntity scope = new SysResourceScopeEntity();
    scope.setId(9101L);
    scope.setTenantId(1001L);
    scope.setScopeCode("FINANCE");
    scope.setScopeName("Finance Scope");
    scope.setScopeType("RESOURCE");
    scope.setStatus(1);
    when(resourceScopeMapper.selectById(9101L)).thenReturn(scope);
    when(identifierGenerator.nextId(any()))
        .thenReturn(9201001L)
        .thenReturn(9201002L);

    resourceScopeService.replaceMembers(9101L, new ResourceScopeMemberReplaceRequest(List.of(
        new ResourceScopeMemberUpsertRequest(2001L, true, false, false, false, false, "first"),
        new ResourceScopeMemberUpsertRequest(2001L, true, true, true, false, true, "second")
    )));

    ArgumentCaptor<List<SysResourceScopeMemberEntity>> captor = ArgumentCaptor.forClass(List.class);
    verify(resourceScopeMemberMapper).replaceForScope(eq(9101L), captor.capture());

    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().getFirst().getUserId()).isEqualTo(2001L);
    assertThat(captor.getValue().getFirst().getCanManage()).isEqualTo(1);
    assertThat(captor.getValue().getFirst().getRemark()).isEqualTo("second");
  }

  @Test
  void deleteRemovesUnusedScopeAndMembers() {
    SysResourceScopeEntity scope = new SysResourceScopeEntity();
    scope.setId(9101L);
    scope.setTenantId(1001L);
    scope.setScopeCode("FINANCE");
    scope.setScopeName("Finance Scope");
    scope.setScopeType("RESOURCE");
    scope.setStatus(1);
    when(resourceScopeMapper.selectById(9101L)).thenReturn(scope);
    when(resourceScopeUsageMapper.countOwnerReferences(9101L)).thenReturn(0L);
    when(resourceScopeUsageMapper.countSharedReferences(9101L)).thenReturn(0L);

    resourceScopeService.delete(9101L);

    verify(resourceScopeMemberMapper).deleteByScopeIdHard(9101L);
    verify(resourceScopeMapper).deleteByIdHard(9101L);
  }

  @Test
  void deleteRejectsScopeUsedByStandards() {
    SysResourceScopeEntity scope = new SysResourceScopeEntity();
    scope.setId(9101L);
    scope.setTenantId(1001L);
    scope.setScopeCode("FINANCE");
    scope.setScopeName("Finance Scope");
    scope.setScopeType("RESOURCE");
    scope.setStatus(1);
    when(resourceScopeMapper.selectById(9101L)).thenReturn(scope);
    when(resourceScopeUsageMapper.countOwnerReferences(9101L)).thenReturn(1L);
    when(resourceScopeUsageMapper.countSharedReferences(9101L)).thenReturn(0L);

    assertThatThrownBy(() -> resourceScopeService.delete(9101L))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("RESOURCE_SCOPE_IN_USE");

    verify(resourceScopeMemberMapper, never()).deleteByScopeIdHard(9101L);
    verify(resourceScopeMapper, never()).deleteByIdHard(9101L);
  }
}
