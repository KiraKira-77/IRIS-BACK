package com.iris.back.business.standard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.standard.mapper.BizStandardMapper;
import com.iris.back.business.standard.model.dto.StandardDto;
import com.iris.back.business.standard.model.entity.BizStandardEntity;
import com.iris.back.business.standard.model.request.StandardUpsertRequest;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.mapper.SysResourceScopeMapper;
import com.iris.back.system.model.dto.ResourceScopeMemberDto;
import com.iris.back.system.model.entity.SysResourceScopeEntity;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class StandardService {

  private static final long SYSTEM_USER_ID = 2001L;

  private final BizStandardMapper standardMapper;
  private final SysResourceScopeMapper resourceScopeMapper;
  private final SysResourceScopeMemberMapper resourceScopeMemberMapper;
  private final CurrentUserContext currentUserContext;
  private final IdentifierGenerator identifierGenerator;

  public StandardService(
      BizStandardMapper standardMapper,
      SysResourceScopeMapper resourceScopeMapper,
      SysResourceScopeMemberMapper resourceScopeMemberMapper,
      CurrentUserContext currentUserContext,
      IdentifierGenerator identifierGenerator
  ) {
    this.standardMapper = standardMapper;
    this.resourceScopeMapper = resourceScopeMapper;
    this.resourceScopeMemberMapper = resourceScopeMemberMapper;
    this.currentUserContext = currentUserContext;
    this.identifierGenerator = identifierGenerator;
  }

  public List<StandardDto> list() {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    StandardPermissionContext permissionContext = buildPermissionContext(principal);

    return standardMapper.selectList(new LambdaQueryWrapper<BizStandardEntity>()
            .eq(BizStandardEntity::getTenantId, principal.tenantId())
            .orderByAsc(BizStandardEntity::getId))
        .stream()
        .filter(entity -> canView(entity, permissionContext))
        .sorted(Comparator.comparing(BizStandardEntity::getId))
        .map(this::toDto)
        .toList();
  }

  public StandardDto get(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity entity = requireStandard(parseId(id), principal.tenantId());
    ensureCanView(entity, buildPermissionContext(principal));
    return toDto(entity);
  }

  public StandardDto create(StandardUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ensureTenantAccess(request.tenantId(), principal.tenantId());
    StandardPermissionContext permissionContext = buildPermissionContext(principal);
    Long ownerScopeId = requireOwnerScope(parseId(request.ownerScopeId()), request.tenantId()).getId();
    ensureCanCreate(ownerScopeId, permissionContext);
    BizStandardEntity entity = new BizStandardEntity();
    long nextId = nextId(entity);
    entity.setId(nextId);
    entity.setTenantId(request.tenantId());
    entity.setStandardGroupId(request.standardGroupId() == null || request.standardGroupId().isBlank()
        ? String.valueOf(nextId)
        : request.standardGroupId());
    applyFields(entity, request, ownerScopeId);
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(SYSTEM_USER_ID);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    standardMapper.insert(entity);
    return toDto(entity);
  }

  public StandardDto update(String id, StandardUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ensureTenantAccess(request.tenantId(), principal.tenantId());
    StandardPermissionContext permissionContext = buildPermissionContext(principal);
    Long ownerScopeId = requireOwnerScope(parseId(request.ownerScopeId()), request.tenantId()).getId();
    BizStandardEntity entity = requireStandard(parseId(id), principal.tenantId());
    ensureCanEdit(entity, permissionContext);
    ensureOwnerScopeChangeAllowed(entity, ownerScopeId, permissionContext);
    entity.setTenantId(request.tenantId());
    entity.setStandardGroupId(request.standardGroupId() == null || request.standardGroupId().isBlank()
        ? entity.getStandardGroupId()
        : request.standardGroupId());
    applyFields(entity, request, ownerScopeId);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    standardMapper.updateById(entity);
    return toDto(entity);
  }

  public void delete(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity entity = requireStandard(parseId(id), principal.tenantId());
    ensureCanDelete(entity, buildPermissionContext(principal));
    standardMapper.deleteById(entity.getId());
  }

  private void applyFields(BizStandardEntity entity, StandardUpsertRequest request, Long ownerScopeId) {
    entity.setTitle(request.title());
    entity.setCategory(request.category());
    entity.setStandardVersion(request.version());
    entity.setStatus(request.status());
    entity.setPublishDate(parseDate(request.publishDate()));
    entity.setDescription(request.description());
    entity.setTags(String.join(",", normalizeTags(request.tags())));
    entity.setVersionNumber(request.versionNumber() == null ? 1 : request.versionNumber());
    entity.setPreviousVersionId(parseOptionalId(request.previousVersionId()));
    entity.setVisibilityLevel(request.visibilityLevel());
    entity.setOwnerScopeId(ownerScopeId);
    entity.setSharedScopeIds(String.join(",", normalizeScopeIds(
        request.grantScopeIds(),
        ownerScopeId,
        request.tenantId()
    )));
    entity.setChangeLog(request.changeLog());
  }

  private BizStandardEntity requireStandard(Long id, Long tenantId) {
    BizStandardEntity entity = standardMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BusinessException("STANDARD_NOT_FOUND", "standard not found: " + id);
    }
    return entity;
  }

  private SysResourceScopeEntity requireScope(Long scopeId, Long tenantId) {
    SysResourceScopeEntity entity = resourceScopeMapper.selectById(scopeId);
    if (entity == null) {
      throw new BusinessException("RESOURCE_SCOPE_NOT_FOUND", "resource scope not found: " + scopeId);
    }
    if (!Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BusinessException(
          "RESOURCE_SCOPE_TENANT_MISMATCH",
          "resource scope tenant mismatch: " + scopeId
      );
    }
    return entity;
  }

  private SysResourceScopeEntity requireOwnerScope(Long scopeId, Long tenantId) {
    SysResourceScopeEntity entity = requireScope(scopeId, tenantId);
    if (!"RESOURCE".equalsIgnoreCase(entity.getScopeType())) {
      throw new BusinessException(
          "RESOURCE_SCOPE_OWNER_TYPE_INVALID",
          "resource scope cannot be used as owner scope: " + scopeId
      );
    }
    return entity;
  }

  private Long parseId(String value) {
    return Long.valueOf(value);
  }

  private Long parseOptionalId(String value) {
    return value == null || value.isBlank() ? null : parseId(value);
  }

  private LocalDate parseDate(String value) {
    if (value == null || value.isBlank() || "-".equals(value)) {
      return null;
    }
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException exception) {
      throw new BusinessException("STANDARD_DATE_INVALID", "invalid publish date: " + value);
    }
  }

  private List<String> normalizeTags(List<String> tags) {
    return tags == null ? List.of() : tags.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(tag -> !tag.isEmpty())
        .distinct()
        .toList();
  }

  private List<String> normalizeScopeIds(List<String> scopeIds, Long ownerScopeId, Long tenantId) {
    return scopeIds == null ? List.of() : scopeIds.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(scopeId -> !scopeId.isEmpty())
        .map(this::parseId)
        .filter(scopeId -> !scopeId.equals(ownerScopeId))
        .distinct()
        .map(scopeId -> requireScope(scopeId, tenantId).getId())
        .map(String::valueOf)
        .toList();
  }

  private long nextId(Object entity) {
    return ((Number) identifierGenerator.nextId(entity)).longValue();
  }

  private StandardDto toDto(BizStandardEntity entity) {
    return new StandardDto(
        String.valueOf(entity.getId()),
        entity.getStandardGroupId(),
        entity.getTitle(),
        entity.getCategory(),
        entity.getStandardVersion(),
        entity.getPublishDate() == null ? null : entity.getPublishDate().toString(),
        entity.getStatus(),
        List.of(),
        splitCommaValues(entity.getTags()),
        entity.getDescription(),
        entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString(),
        entity.getUpdatedAt() == null ? null : entity.getUpdatedAt().toString(),
        entity.getVersionNumber(),
        entity.getPreviousVersionId() == null ? null : String.valueOf(entity.getPreviousVersionId()),
        entity.getVisibilityLevel(),
        entity.getOwnerScopeId() == null ? null : String.valueOf(entity.getOwnerScopeId()),
        splitCommaValues(entity.getSharedScopeIds()).stream()
            .map(scopeId -> new StandardDto.ScopeGrantDto(scopeId, List.of("view")))
            .toList(),
        entity.getChangeLog()
    );
  }

  private List<String> splitCommaValues(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }

  private StandardPermissionContext buildPermissionContext(CurrentUserPrincipal principal) {
    if (isSuperAdmin(principal)) {
      return new StandardPermissionContext(true, Map.of());
    }

    Map<String, ResourceScopeMemberDto> memberships = resourceScopeMemberMapper
        .selectByTenantIdAndUserId(principal.tenantId(), principal.userId())
        .stream()
        .collect(Collectors.toMap(ResourceScopeMemberDto::scopeId, member -> member, (left, right) -> left));

    return new StandardPermissionContext(false, memberships);
  }

  private boolean canView(BizStandardEntity entity, StandardPermissionContext permissionContext) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    if ("PUBLIC".equalsIgnoreCase(entity.getVisibilityLevel())) {
      return true;
    }
    if (hasAnyOwnerAccess(entity.getOwnerScopeId(), permissionContext)) {
      return true;
    }
    return splitCommaValues(entity.getSharedScopeIds()).stream()
        .anyMatch(scopeId -> hasViewAccess(scopeId, permissionContext));
  }

  private void ensureCanView(BizStandardEntity entity, StandardPermissionContext permissionContext) {
    if (!canView(entity, permissionContext)) {
      throw new AccessDeniedException("no permission to view standard");
    }
  }

  private void ensureCanCreate(Long ownerScopeId, StandardPermissionContext permissionContext) {
    if (!matchesScopeAction(ownerScopeId, permissionContext, this::hasCreateAccess)) {
      throw new AccessDeniedException("no permission to create standard in selected scope");
    }
  }

  private void ensureCanEdit(BizStandardEntity entity, StandardPermissionContext permissionContext) {
    if (!matchesScopeAction(entity.getOwnerScopeId(), permissionContext, this::hasEditAccess)) {
      throw new AccessDeniedException("no permission to edit standard");
    }
  }

  private void ensureCanDelete(BizStandardEntity entity, StandardPermissionContext permissionContext) {
    if (!matchesScopeAction(entity.getOwnerScopeId(), permissionContext, this::hasDeleteAccess)) {
      throw new AccessDeniedException("no permission to delete standard");
    }
  }

  private void ensureOwnerScopeChangeAllowed(
      BizStandardEntity entity,
      Long nextOwnerScopeId,
      StandardPermissionContext permissionContext
  ) {
    if (Objects.equals(entity.getOwnerScopeId(), nextOwnerScopeId)) {
      return;
    }
    if (!matchesScopeAction(entity.getOwnerScopeId(), permissionContext, this::hasManageAccess)) {
      throw new AccessDeniedException("no permission to move standard to another owner scope");
    }
    ensureCanCreate(nextOwnerScopeId, permissionContext);
  }

  private boolean matchesScopeAction(
      Long scopeId,
      StandardPermissionContext permissionContext,
      Predicate<ResourceScopeMemberDto> predicate
  ) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    ResourceScopeMemberDto member = permissionContext.memberships().get(String.valueOf(scopeId));
    return member != null && predicate.test(member);
  }

  private boolean hasAnyOwnerAccess(Long scopeId, StandardPermissionContext permissionContext) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    ResourceScopeMemberDto member = permissionContext.memberships().get(String.valueOf(scopeId));
    return member != null && (
        flag(member.canView()) ||
        flag(member.canCreate()) ||
        flag(member.canEdit()) ||
        flag(member.canDelete()) ||
        flag(member.canManage())
    );
  }

  private boolean hasViewAccess(String scopeId, StandardPermissionContext permissionContext) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    ResourceScopeMemberDto member = permissionContext.memberships().get(scopeId);
    return member != null && (flag(member.canView()) || flag(member.canManage()));
  }

  private boolean hasCreateAccess(ResourceScopeMemberDto member) {
    return flag(member.canCreate()) || flag(member.canManage());
  }

  private boolean hasEditAccess(ResourceScopeMemberDto member) {
    return flag(member.canEdit()) || flag(member.canManage());
  }

  private boolean hasDeleteAccess(ResourceScopeMemberDto member) {
    return flag(member.canDelete()) || flag(member.canManage());
  }

  private boolean hasManageAccess(ResourceScopeMemberDto member) {
    return flag(member.canManage());
  }

  private boolean flag(Integer value) {
    return Integer.valueOf(1).equals(value);
  }

  private boolean isSuperAdmin(CurrentUserPrincipal principal) {
    return principal.roles().stream()
        .map(String::toUpperCase)
        .anyMatch(role -> "PLATFORM_ADMIN".equals(role) || "SUPER_ADMIN".equals(role));
  }

  private void ensureTenantAccess(Long requestTenantId, Long currentTenantId) {
    if (!Objects.equals(requestTenantId, currentTenantId)) {
      throw new AccessDeniedException("tenant mismatch");
    }
  }

  private record StandardPermissionContext(
      boolean superAdmin,
      Map<String, ResourceScopeMemberDto> memberships
  ) {
  }
}
