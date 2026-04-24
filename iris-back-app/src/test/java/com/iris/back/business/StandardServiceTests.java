package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.standard.mapper.BizStandardMapper;
import com.iris.back.business.standard.model.entity.BizStandardEntity;
import com.iris.back.business.standard.model.request.StandardUpgradeRequest;
import com.iris.back.business.standard.model.request.StandardUpsertRequest;
import com.iris.back.business.standard.service.StandardService;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.mapper.SysResourceScopeMapper;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.model.dto.ResourceScopeMemberDto;
import com.iris.back.system.model.entity.SysResourceScopeEntity;
import com.iris.back.system.model.entity.SysUserEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StandardServiceTests {

  @Mock
  private BizStandardMapper standardMapper;

  @Mock
  private SysResourceScopeMapper resourceScopeMapper;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @Mock
  private SysResourceScopeMemberMapper resourceScopeMemberMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  @Mock
  private SysUserMapper userMapper;

  @InjectMocks
  private StandardService standardService;

  @Test
  void listReturnsOnlyStandardsVisibleToCurrentUser() {
    mockCurrentUser(2004L, List.of("AUDITOR"));

    BizStandardEntity entity = new BizStandardEntity();
    entity.setId(9901L);
    entity.setTenantId(1001L);
    entity.setStandardGroupId("std-001");
    entity.setStandardCode("STD-FIN-001");
    entity.setTitle("Finance Baseline");
    entity.setCategory("internal");
    entity.setStandardVersion("V1.0");
    entity.setVersionNumber(1);
    entity.setPublishDate(LocalDate.of(2026, 4, 23));
    entity.setStatus("active");
    entity.setDescription("baseline");
    entity.setVisibilityLevel("SCOPED");
    entity.setOwnerScopeId(9001L);
    entity.setSharedScopeIds("9002,9003");
    BizStandardEntity hidden = new BizStandardEntity();
    hidden.setId(9902L);
    hidden.setTenantId(1001L);
    hidden.setStandardGroupId("std-002");
    hidden.setStandardCode("STD-IT-002");
    hidden.setTitle("IT Restricted");
    hidden.setCategory("internal");
    hidden.setStandardVersion("V1.0");
    hidden.setVersionNumber(1);
    hidden.setPublishDate(LocalDate.of(2026, 4, 24));
    hidden.setStatus("active");
    hidden.setDescription("hidden");
    hidden.setVisibilityLevel("SCOPED");
    hidden.setOwnerScopeId(9010L);
    hidden.setSharedScopeIds("9011");
    entity.setUpdatedBy(2004L);
    when(userMapper.selectBatchIds(List.of(2004L))).thenReturn(List.of(user(2004L, "Senior Auditor")));
    when(standardMapper.selectList(any())).thenReturn(List.of(entity, hidden));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2004L)).thenReturn(List.of(
        scopeMember(9001L, 2004L, 1, 0, 0, 0, 0),
        scopeMember(9002L, 2004L, 1, 0, 0, 0, 0)
    ));

    var result = standardService.list();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo("9901");
    assertThat(result.getFirst().standardCode()).isEqualTo("STD-FIN-001");
    assertThat(result.getFirst().ownerScopeId()).isEqualTo("9001");
    assertThat(result.getFirst().operatorName()).isEqualTo("Senior Auditor");
    assertThat(result.getFirst().grants()).extracting(grant -> grant.scopeId() + ":" + grant.actions())
        .containsExactly("9002:[view]", "9003:[view]");
  }

  @Test
  void listHidesDraftCreatedByAnotherUser() {
    mockCurrentUser(2004L, List.of("AUDITOR"));

    BizStandardEntity ownVisible = standard(9901L, "group-1", "STD-FIN-001", "V1.0", 1, null);
    ownVisible.setTitle("Visible Active");
    ownVisible.setCategory("internal");
    ownVisible.setStatus("active");
    ownVisible.setVisibilityLevel("SCOPED");
    ownVisible.setOwnerScopeId(9001L);
    ownVisible.setUpdatedBy(2004L);

    BizStandardEntity othersDraft = standard(9902L, "group-2", "STD-FIN-002", "V1.0", 1, null);
    othersDraft.setTitle("Others Draft");
    othersDraft.setCategory("internal");
    othersDraft.setStatus("draft");
    othersDraft.setVisibilityLevel("SCOPED");
    othersDraft.setOwnerScopeId(9001L);
    othersDraft.setCreatedBy(2002L);
    othersDraft.setUpdatedBy(2002L);

    when(standardMapper.selectList(any())).thenReturn(List.of(ownVisible, othersDraft));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2004L)).thenReturn(List.of(
        scopeMember(9001L, 2004L, 1, 0, 0, 0, 0)
    ));
    when(userMapper.selectBatchIds(List.of(2004L))).thenReturn(List.of(user(2004L, "Senior Auditor")));

    var result = standardService.list();

    assertThat(result).extracting(item -> item.id()).containsExactly("9901");
  }

  @Test
  void getRejectsDraftCreatedByAnotherUser() {
    mockCurrentUser(2004L, List.of("AUDITOR"));

    BizStandardEntity othersDraft = standard(9902L, "group-2", "STD-FIN-002", "V1.0", 1, null);
    othersDraft.setTitle("Others Draft");
    othersDraft.setCategory("internal");
    othersDraft.setStatus("draft");
    othersDraft.setVisibilityLevel("SCOPED");
    othersDraft.setOwnerScopeId(9001L);
    othersDraft.setCreatedBy(2002L);
    othersDraft.setUpdatedBy(2002L);
    when(standardMapper.selectById(9902L)).thenReturn(othersDraft);
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2004L)).thenReturn(List.of(
        scopeMember(9001L, 2004L, 1, 0, 0, 0, 0)
    ));

    assertThatThrownBy(() -> standardService.get("9902"))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }

  @Test
  void superAdminCanViewDraftCreatedByAnotherUser() {
    mockCurrentUser(2001L, List.of("SUPER_ADMIN"));

    BizStandardEntity othersDraft = standard(9902L, "group-2", "STD-FIN-002", "V1.0", 1, null);
    othersDraft.setTitle("Others Draft");
    othersDraft.setCategory("internal");
    othersDraft.setStatus("draft");
    othersDraft.setVisibilityLevel("SCOPED");
    othersDraft.setOwnerScopeId(9001L);
    othersDraft.setCreatedBy(2002L);
    othersDraft.setUpdatedBy(2002L);
    when(standardMapper.selectList(any())).thenReturn(List.of(othersDraft));
    when(userMapper.selectBatchIds(List.of(2002L))).thenReturn(List.of(user(2002L, "Finance Manager")));

    var result = standardService.list();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().id()).isEqualTo("9902");
  }

  @Test
  void createAssignsGeneratedIdAndPersistsPermissionFields() {
    mockCurrentUser(2002L, List.of("AUDITOR"));
    when(resourceScopeMapper.selectById(9001L)).thenReturn(scope(9001L, 1001L));
    when(resourceScopeMapper.selectById(9002L)).thenReturn(scope(9002L, 1001L));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2002L)).thenReturn(List.of(
        scopeMember(9001L, 2002L, 1, 1, 1, 0, 0)
    ));
    when(identifierGenerator.nextId(any())).thenReturn(9902L);
    when(userMapper.selectBatchIds(List.of(2002L))).thenReturn(List.of(user(2002L, "Finance Manager")));

    var created = standardService.create(new StandardUpsertRequest(
        1001L,
        "Finance Standard",
        "internal",
        "V1.0",
        "draft",
        "2026-04-23",
        "standard description",
        "STD-FIN-002",
        null,
        null,
        null,
        "PUBLIC",
        "9001",
        List.of("9002"),
        "initial draft"
    ));

    ArgumentCaptor<BizStandardEntity> captor = ArgumentCaptor.forClass(BizStandardEntity.class);
    verify(standardMapper).insert(captor.capture());

    assertThat(created.id()).isEqualTo("9902");
    assertThat(created.standardGroupId()).isEqualTo("9902");
    assertThat(created.standardCode()).isEqualTo("STD-FIN-002");
    assertThat(created.ownerScopeId()).isEqualTo("9001");
    assertThat(created.visibilityLevel()).isEqualTo("PUBLIC");
    assertThat(created.operatorName()).isEqualTo("Finance Manager");
    assertThat(created.grants()).extracting(grant -> grant.scopeId()).containsExactly("9002");
    assertThat(captor.getValue().getCreatedBy()).isEqualTo(2002L);
    assertThat(captor.getValue().getUpdatedBy()).isEqualTo(2002L);
    assertThat(captor.getValue().getStandardCode()).isEqualTo("STD-FIN-002");
    assertThat(captor.getValue().getSharedScopeIds()).isEqualTo("9002");
  }

  @Test
  void createNormalizesGrantedScopesAgainstOwnerAndDuplicates() {
    mockCurrentUser(2002L, List.of("AUDITOR"));
    when(resourceScopeMapper.selectById(9001L)).thenReturn(scope(9001L, 1001L));
    when(resourceScopeMapper.selectById(9002L)).thenReturn(scope(9002L, 1001L));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2002L)).thenReturn(List.of(
        scopeMember(9001L, 2002L, 1, 1, 1, 0, 0)
    ));
    when(identifierGenerator.nextId(any())).thenReturn(9903L);

    standardService.create(new StandardUpsertRequest(
        1001L,
        "Finance Standard",
        "internal",
        "V1.0",
        "draft",
        null,
        "standard description",
        "STD-FIN-003",
        null,
        null,
        null,
        "SCOPED",
        "9001",
        List.of("9001", "9002", "9002"),
        "initial draft"
    ));

    ArgumentCaptor<BizStandardEntity> captor = ArgumentCaptor.forClass(BizStandardEntity.class);
    verify(standardMapper).insert(captor.capture());
    assertThat(captor.getValue().getSharedScopeIds()).isEqualTo("9002");
  }

  @Test
  void createRejectsUnknownGrantedScope() {
    mockCurrentUser(2002L, List.of("AUDITOR"));
    when(resourceScopeMapper.selectById(9001L)).thenReturn(scope(9001L, 1001L));
    when(resourceScopeMapper.selectById(9009L)).thenReturn(null);
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2002L)).thenReturn(List.of(
        scopeMember(9001L, 2002L, 1, 1, 1, 0, 0)
    ));
    when(identifierGenerator.nextId(any())).thenReturn(9904L);

    assertThatThrownBy(() -> standardService.create(new StandardUpsertRequest(
        1001L,
        "Finance Standard",
        "internal",
        "V1.0",
        "draft",
        null,
        "standard description",
        "STD-FIN-004",
        null,
        null,
        null,
        "SCOPED",
        "9001",
        List.of("9009"),
        "initial draft"
    )))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("RESOURCE_SCOPE_NOT_FOUND");

    verify(standardMapper, never()).insert(any(BizStandardEntity.class));
  }

  @Test
  void createRejectsGrantedScopeFromAnotherTenant() {
    mockCurrentUser(2002L, List.of("AUDITOR"));
    when(resourceScopeMapper.selectById(9001L)).thenReturn(scope(9001L, 1001L));
    when(resourceScopeMapper.selectById(9002L)).thenReturn(scope(9002L, 2002L));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2002L)).thenReturn(List.of(
        scopeMember(9001L, 2002L, 1, 1, 1, 0, 0)
    ));
    when(identifierGenerator.nextId(any())).thenReturn(9905L);

    assertThatThrownBy(() -> standardService.create(new StandardUpsertRequest(
        1001L,
        "Finance Standard",
        "internal",
        "V1.0",
        "draft",
        null,
        "standard description",
        "STD-FIN-005",
        null,
        null,
        null,
        "SCOPED",
        "9001",
        List.of("9002"),
        "initial draft"
    )))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("RESOURCE_SCOPE_TENANT_MISMATCH");

    verify(standardMapper, never()).insert(any(BizStandardEntity.class));
  }

  @Test
  void createRejectsUserWithoutCreatePermissionOnOwnerScope() {
    mockCurrentUser(2004L, List.of("AUDITOR"));
    when(resourceScopeMapper.selectById(9001L)).thenReturn(scope(9001L, 1001L));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2004L)).thenReturn(List.of(
        scopeMember(9001L, 2004L, 1, 0, 0, 0, 0)
    ));

    assertThatThrownBy(() -> standardService.create(new StandardUpsertRequest(
        1001L,
        "Finance Standard",
        "internal",
        "V1.0",
        "draft",
        null,
        "standard description",
        "STD-FIN-006",
        null,
        null,
        null,
        "SCOPED",
        "9001",
        List.of(),
        "initial draft"
    ))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

    verify(standardMapper, never()).insert(any(BizStandardEntity.class));
  }

  @Test
  void createRejectsSharedOnlyScopeAsOwnerScope() {
    mockCurrentUser(2002L, List.of("AUDITOR"));
    when(resourceScopeMapper.selectById(9008L)).thenReturn(scope(9008L, 1001L, "STANDARD"));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2002L)).thenReturn(List.of(
        scopeMember(9008L, 2002L, 1, 1, 1, 0, 0)
    ));

    assertThatThrownBy(() -> standardService.create(new StandardUpsertRequest(
        1001L,
        "Shared Scope Standard",
        "internal",
        "V1.0",
        "draft",
        null,
        "standard description",
        "STD-SHARED-001",
        null,
        null,
        null,
        "SCOPED",
        "9008",
        List.of(),
        "initial draft"
    )))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("RESOURCE_SCOPE_OWNER_TYPE_INVALID");

    verify(standardMapper, never()).insert(any(BizStandardEntity.class));
  }

  @Test
  void upgradeCreatesNextDraftFromLatestVersion() {
    mockCurrentUser(2002L, List.of("AUDITOR"));
    BizStandardEntity source = standard(9902L, "group-1", "STD-FIN-001", "V2.0", 2, null);
    source.setTenantId(1001L);
    source.setTitle("Finance Standard");
    source.setCategory("internal");
    source.setStatus("active");
    source.setDescription("standard description");
    source.setVisibilityLevel("SCOPED");
    source.setOwnerScopeId(9001L);
    source.setSharedScopeIds("9002");
    when(standardMapper.selectById(9902L)).thenReturn(source);
    when(standardMapper.selectList(any())).thenReturn(List.of(
        standard(9901L, "group-1", "STD-FIN-001", "V1.0", 1, 9902L),
        source
    ));
    when(resourceScopeMapper.selectById(9001L)).thenReturn(scope(9001L, 1001L));
    when(resourceScopeMapper.selectById(9002L)).thenReturn(scope(9002L, 1001L));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2002L)).thenReturn(List.of(
        scopeMember(9001L, 2002L, 1, 1, 1, 0, 0),
        scopeMember(9002L, 2002L, 1, 0, 0, 0, 0)
    ));
    when(identifierGenerator.nextId(any())).thenReturn(9903L);
    when(userMapper.selectBatchIds(List.of(2002L))).thenReturn(List.of(user(2002L, "Finance Manager")));

    var created = standardService.upgrade("9902", new StandardUpgradeRequest("V3.0", "upgrade draft"));

    ArgumentCaptor<BizStandardEntity> captor = ArgumentCaptor.forClass(BizStandardEntity.class);
    verify(standardMapper).insert(captor.capture());

    assertThat(created.id()).isEqualTo("9903");
    assertThat(created.standardGroupId()).isEqualTo("group-1");
    assertThat(created.version()).isEqualTo("V3.0");
    assertThat(created.status()).isEqualTo("draft");
    assertThat(created.publishDate()).isNull();
    assertThat(created.versionNumber()).isEqualTo(3);
    assertThat(created.previousVersionId()).isEqualTo("9902");
    assertThat(created.changeLog()).isEqualTo("upgrade draft");
    assertThat(created.operatorName()).isEqualTo("Finance Manager");
    assertThat(captor.getValue().getCreatedBy()).isEqualTo(2002L);
    assertThat(captor.getValue().getUpdatedBy()).isEqualTo(2002L);
    assertThat(captor.getValue().getVersionNumber()).isEqualTo(3);
    assertThat(captor.getValue().getPreviousVersionId()).isEqualTo(9902L);
    assertThat(captor.getValue().getStandardVersion()).isEqualTo("V3.0");
  }

  @Test
  void upgradeRejectsNonLatestSourceVersion() {
    mockCurrentUser(2002L, List.of("AUDITOR"));
    BizStandardEntity source = standard(9901L, "group-1", "STD-FIN-001", "V1.0", 1, null);
    source.setTenantId(1001L);
    source.setTitle("Finance Standard");
    source.setCategory("internal");
    source.setStatus("archived");
    source.setDescription("standard description");
    source.setVisibilityLevel("SCOPED");
    source.setOwnerScopeId(9001L);
    when(standardMapper.selectById(9901L)).thenReturn(source);
    when(standardMapper.selectList(any())).thenReturn(List.of(
        source,
        standard(9902L, "group-1", "STD-FIN-001", "V2.0", 2, 9901L)
    ));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2002L)).thenReturn(List.of(
        scopeMember(9001L, 2002L, 1, 1, 1, 0, 0)
    ));

    assertThatThrownBy(() -> standardService.upgrade("9901", new StandardUpgradeRequest("V3.0", "upgrade draft")))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("STANDARD_UPGRADE_SOURCE_NOT_LATEST");

    verify(standardMapper, never()).insert(any(BizStandardEntity.class));
  }

  @Test
  void upgradeRejectsDuplicateVersionLabelInSameGroup() {
    mockCurrentUser(2002L, List.of("AUDITOR"));
    BizStandardEntity source = standard(9902L, "group-1", "STD-FIN-001", "V2.0", 2, 9901L);
    source.setTenantId(1001L);
    source.setTitle("Finance Standard");
    source.setCategory("internal");
    source.setStatus("active");
    source.setDescription("standard description");
    source.setVisibilityLevel("SCOPED");
    source.setOwnerScopeId(9001L);
    when(standardMapper.selectById(9902L)).thenReturn(source);
    when(standardMapper.selectList(any())).thenReturn(List.of(
        standard(9901L, "group-1", "STD-FIN-001", "V3.0", 1, null),
        source,
        standard(9900L, "group-1", "STD-FIN-001", "V0.9", 0, null)
    ));
    when(resourceScopeMemberMapper.selectByTenantIdAndUserId(1001L, 2002L)).thenReturn(List.of(
        scopeMember(9001L, 2002L, 1, 1, 1, 0, 0)
    ));

    assertThatThrownBy(() -> standardService.upgrade("9902", new StandardUpgradeRequest("V3.0", "upgrade draft")))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("STANDARD_VERSION_DUPLICATE");

    verify(standardMapper, never()).insert(any(BizStandardEntity.class));
  }

  private BizStandardEntity standard(
      Long id,
      String standardGroupId,
      String standardCode,
      String version,
      Integer versionNumber,
      Long previousVersionId
  ) {
    BizStandardEntity entity = new BizStandardEntity();
    entity.setId(id);
    entity.setTenantId(1001L);
    entity.setStandardGroupId(standardGroupId);
    entity.setStandardCode(standardCode);
    entity.setStandardVersion(version);
    entity.setVersionNumber(versionNumber);
    entity.setPreviousVersionId(previousVersionId);
    return entity;
  }

  private SysUserEntity user(Long id, String username) {
    SysUserEntity entity = new SysUserEntity();
    entity.setId(id);
    entity.setUsername(username);
    return entity;
  }

  private SysResourceScopeEntity scope(Long id, Long tenantId) {
    return scope(id, tenantId, "RESOURCE");
  }

  private SysResourceScopeEntity scope(Long id, Long tenantId, String scopeType) {
    SysResourceScopeEntity entity = new SysResourceScopeEntity();
    entity.setId(id);
    entity.setTenantId(tenantId);
    entity.setScopeType(scopeType);
    return entity;
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
