package com.iris.back.business.checklist.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
import com.iris.back.business.checklist.model.dto.ChecklistDto;
import com.iris.back.business.checklist.model.dto.ChecklistItemDto;
import com.iris.back.business.checklist.model.entity.BizChecklistEntity;
import com.iris.back.business.checklist.model.entity.BizChecklistItemEntity;
import com.iris.back.business.checklist.model.request.ChecklistItemUpsertRequest;
import com.iris.back.business.checklist.model.request.ChecklistListQuery;
import com.iris.back.business.checklist.model.request.ChecklistUpsertRequest;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.common.model.PageResponse;
import com.iris.back.common.util.DateTimeFormatters;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.model.dto.ResourceScopeMemberDto;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChecklistService {

  private final BizChecklistMapper checklistMapper;
  private final BizChecklistItemMapper checklistItemMapper;
  private final CurrentUserContext currentUserContext;
  private final SysResourceScopeMemberMapper resourceScopeMemberMapper;
  private final IdentifierGenerator identifierGenerator;

  public ChecklistService(
      BizChecklistMapper checklistMapper,
      BizChecklistItemMapper checklistItemMapper,
      CurrentUserContext currentUserContext,
      SysResourceScopeMemberMapper resourceScopeMemberMapper,
      IdentifierGenerator identifierGenerator
  ) {
    this.checklistMapper = checklistMapper;
    this.checklistItemMapper = checklistItemMapper;
    this.currentUserContext = currentUserContext;
    this.resourceScopeMemberMapper = resourceScopeMemberMapper;
    this.identifierGenerator = identifierGenerator;
  }

  public PageResponse<ChecklistDto> list(ChecklistListQuery query) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ChecklistListQuery safeQuery = query == null
        ? new ChecklistListQuery(null, null, null, 1L, 10L)
        : query;
    List<BizChecklistEntity> entities = checklistMapper.selectList(
        new LambdaQueryWrapper<BizChecklistEntity>()
            .eq(BizChecklistEntity::getTenantId, principal.tenantId())
        .orderByDesc(BizChecklistEntity::getUpdatedAt)
            .orderByDesc(BizChecklistEntity::getId)
    );
    ChecklistPermissionContext permissionContext = buildPermissionContext(principal);
    List<BizChecklistEntity> visibleEntities = entities.stream()
        .filter(entity -> canView(entity, permissionContext))
        .toList();
    Map<Long, List<BizChecklistItemEntity>> itemsByChecklistId = loadItems(principal.tenantId(), visibleEntities);
    List<ChecklistDto> filtered = visibleEntities.stream()
        .map(entity -> toDto(entity, itemsByChecklistId.getOrDefault(entity.getId(), List.of())))
        .filter(item -> matches(item, safeQuery))
        .toList();

    long pageNo = safeQuery.normalizedPage();
    long pageSize = safeQuery.normalizedPageSize();
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * pageSize);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + pageSize);
    return PageResponse.of(filtered.size(), pageNo, pageSize, filtered.subList(fromIndex, toIndex));
  }

  public ChecklistDto get(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizChecklistEntity entity = requireChecklist(parseId(id), principal.tenantId());
    ensureCanView(entity, buildPermissionContext(principal));
    return toDto(entity, listItems(principal.tenantId(), entity.getId()));
  }

  @Transactional
  public ChecklistDto create(ChecklistUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Long ownerScopeId = parseId(normalizeRequiredText(request.ownerScopeId(), "CHECKLIST_OWNER_SCOPE_REQUIRED"));
    ensureCanCreate(ownerScopeId, buildPermissionContext(principal));
    BizChecklistEntity entity = new BizChecklistEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(principal.tenantId());
    applyFields(entity, request);
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(principal.userId());
    entity.setUpdatedBy(principal.userId());
    checklistMapper.insert(entity);
    List<BizChecklistItemEntity> items = replaceItems(entity.getId(), principal, request.items());
    return toDto(entity, items);
  }

  @Transactional
  public ChecklistDto update(String id, ChecklistUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizChecklistEntity entity = requireChecklist(parseId(id), principal.tenantId());
    ChecklistPermissionContext permissionContext = buildPermissionContext(principal);
    Long nextOwnerScopeId = parseId(normalizeRequiredText(request.ownerScopeId(), "CHECKLIST_OWNER_SCOPE_REQUIRED"));
    ensureCanEdit(entity, permissionContext);
    ensureOwnerScopeChangeAllowed(entity, nextOwnerScopeId, permissionContext);
    applyFields(entity, request);
    entity.setUpdatedBy(principal.userId());
    checklistMapper.updateById(entity);
    List<BizChecklistItemEntity> items = replaceItems(entity.getId(), principal, request.items());
    return toDto(entity, items);
  }

  @Transactional
  public void delete(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizChecklistEntity entity = requireChecklist(parseId(id), principal.tenantId());
    ensureCanDelete(entity, buildPermissionContext(principal));
    checklistItemMapper.delete(new LambdaQueryWrapper<BizChecklistItemEntity>()
        .eq(BizChecklistItemEntity::getTenantId, principal.tenantId())
        .eq(BizChecklistItemEntity::getChecklistId, entity.getId()));
    checklistMapper.deleteById(entity.getId());
  }

  private Map<Long, List<BizChecklistItemEntity>> loadItems(
      Long tenantId,
      List<BizChecklistEntity> checklists
  ) {
    if (checklists.isEmpty()) {
      return Map.of();
    }
    List<Long> checklistIds = checklists.stream().map(BizChecklistEntity::getId).toList();
    return checklistItemMapper.selectList(new LambdaQueryWrapper<BizChecklistItemEntity>()
            .eq(BizChecklistItemEntity::getTenantId, tenantId)
            .in(BizChecklistItemEntity::getChecklistId, checklistIds)
            .orderByAsc(BizChecklistItemEntity::getSequenceNo)
            .orderByAsc(BizChecklistItemEntity::getId))
        .stream()
        .collect(Collectors.groupingBy(BizChecklistItemEntity::getChecklistId));
  }

  private List<BizChecklistItemEntity> listItems(Long tenantId, Long checklistId) {
    return checklistItemMapper.selectList(new LambdaQueryWrapper<BizChecklistItemEntity>()
        .eq(BizChecklistItemEntity::getTenantId, tenantId)
        .eq(BizChecklistItemEntity::getChecklistId, checklistId)
        .orderByAsc(BizChecklistItemEntity::getSequenceNo)
        .orderByAsc(BizChecklistItemEntity::getId));
  }

  private boolean matches(ChecklistDto item, ChecklistListQuery query) {
    String keyword = normalizeFilterText(query.keyword());
    String status = normalizeFilterText(query.status());
    String scopeId = normalizeFilterText(query.scopeId());
    return (keyword == null
        || containsIgnoreCase(item.name(), keyword)
        || containsIgnoreCase(item.code(), keyword)
        || containsIgnoreCase(item.description(), keyword))
        && (status == null || status.equalsIgnoreCase(item.status()))
        && (scopeId == null || scopeId.equalsIgnoreCase(item.ownerScopeId()));
  }

  private List<BizChecklistItemEntity> replaceItems(
      Long checklistId,
      CurrentUserPrincipal principal,
      List<ChecklistItemUpsertRequest> requests
  ) {
    Map<Long, BizChecklistItemEntity> existingItemsById = listItems(principal.tenantId(), checklistId)
        .stream()
        .collect(Collectors.toMap(BizChecklistItemEntity::getId, Function.identity(), (left, right) -> left));
    if (requests == null || requests.isEmpty()) {
      if (!existingItemsById.isEmpty()) {
        checklistItemMapper.delete(new LambdaQueryWrapper<BizChecklistItemEntity>()
            .eq(BizChecklistItemEntity::getTenantId, principal.tenantId())
            .eq(BizChecklistItemEntity::getChecklistId, checklistId));
      }
      return List.of();
    }

    List<BizChecklistItemEntity> items = new java.util.ArrayList<>();
    Set<Long> submittedExistingIds = new java.util.HashSet<>();
    for (int index = 0; index < requests.size(); index++) {
      ChecklistItemUpsertRequest request = requests.get(index);
      Long requestItemId = parseNullableItemId(request.id());
      BizChecklistItemEntity item = requestItemId == null ? null : existingItemsById.get(requestItemId);
      boolean existingItem = item != null;

      if (!existingItem) {
        item = new BizChecklistItemEntity();
        item.setId(nextId(item));
        item.setTenantId(principal.tenantId());
        item.setChecklistId(checklistId);
        item.setDeleted(0);
        item.setVersion(0L);
        item.setCreatedBy(principal.userId());
      } else {
        submittedExistingIds.add(item.getId());
      }

      item.setSequenceNo(index + 1);
      item.setContent(normalizeRequiredText(request.content(), "CHECKLIST_ITEM_CONTENT_REQUIRED"));
      item.setCriterion(normalizeRequiredText(request.criterion(), "CHECKLIST_ITEM_CRITERION_REQUIRED"));
      item.setControlFrequency(normalizeRequiredText(
          request.controlFrequency(),
          "CHECKLIST_ITEM_CONTROL_FREQUENCY_REQUIRED"
      ));
      item.setEvaluationType(normalizeRequiredText(request.evaluationType(), "CHECKLIST_ITEM_EVALUATION_TYPE_REQUIRED"));
      item.setOrganizationIds(joinCsv(request.organizationIds()));
      item.setUpdatedBy(principal.userId());
      if (existingItem) {
        checklistItemMapper.updateById(item);
      } else {
        checklistItemMapper.insert(item);
      }
      items.add(item);
    }

    List<Long> removedItemIds = existingItemsById.keySet()
        .stream()
        .filter(id -> !submittedExistingIds.contains(id))
        .toList();
    if (!removedItemIds.isEmpty()) {
      checklistItemMapper.delete(new LambdaQueryWrapper<BizChecklistItemEntity>()
          .eq(BizChecklistItemEntity::getTenantId, principal.tenantId())
          .eq(BizChecklistItemEntity::getChecklistId, checklistId)
          .in(BizChecklistItemEntity::getId, removedItemIds));
    }
    return items;
  }

  private void applyFields(BizChecklistEntity entity, ChecklistUpsertRequest request) {
    entity.setChecklistCode(normalizeRequiredText(request.code(), "CHECKLIST_CODE_REQUIRED"));
    entity.setChecklistName(normalizeRequiredText(request.name(), "CHECKLIST_NAME_REQUIRED"));
    entity.setDescription(trimToNull(request.description()));
    entity.setChecklistVersion(normalizeRequiredText(request.version(), "CHECKLIST_VERSION_REQUIRED"));
    Long ownerScopeId = parseId(normalizeRequiredText(request.ownerScopeId(), "CHECKLIST_OWNER_SCOPE_REQUIRED"));
    entity.setOwnerScopeId(ownerScopeId);
    entity.setSharedScopeIds(joinScopeIds(request.grantScopeIds(), ownerScopeId));
    entity.setStatus(normalizeRequiredText(request.status(), "CHECKLIST_STATUS_REQUIRED"));
    entity.setUploadDate(parseDateOrToday(request.uploadDate()));
  }

  private BizChecklistEntity requireChecklist(Long id, Long tenantId) {
    BizChecklistEntity entity = checklistMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BusinessException("CHECKLIST_NOT_FOUND", "checklist not found: " + id);
    }
    return entity;
  }

  private ChecklistPermissionContext buildPermissionContext(CurrentUserPrincipal principal) {
    if (isSuperAdmin(principal)) {
      return new ChecklistPermissionContext(true, Map.of());
    }

    Map<String, ResourceScopeMemberDto> memberships = resourceScopeMemberMapper
        .selectByTenantIdAndUserId(principal.tenantId(), principal.userId())
        .stream()
        .collect(Collectors.toMap(ResourceScopeMemberDto::scopeId, member -> member, (left, right) -> left));

    return new ChecklistPermissionContext(false, memberships);
  }

  private boolean canView(BizChecklistEntity entity, ChecklistPermissionContext permissionContext) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    if (hasAnyOwnerAccess(entity.getOwnerScopeId(), permissionContext)) {
      return true;
    }
    return splitCsv(entity.getSharedScopeIds()).stream()
        .anyMatch(scopeId -> hasViewAccess(scopeId, permissionContext));
  }

  private void ensureCanView(
      BizChecklistEntity entity,
      ChecklistPermissionContext permissionContext
  ) {
    if (!canView(entity, permissionContext)) {
      throw new AccessDeniedException("no permission to view checklist");
    }
  }

  private void ensureCanCreate(Long ownerScopeId, ChecklistPermissionContext permissionContext) {
    if (!matchesScopeAction(ownerScopeId, permissionContext, this::hasCreateAccess)) {
      throw new AccessDeniedException("no permission to create checklist in selected scope");
    }
  }

  private void ensureCanEdit(
      BizChecklistEntity entity,
      ChecklistPermissionContext permissionContext
  ) {
    if (!matchesScopeAction(entity.getOwnerScopeId(), permissionContext, this::hasEditAccess)) {
      throw new AccessDeniedException("no permission to edit checklist");
    }
  }

  private void ensureCanDelete(
      BizChecklistEntity entity,
      ChecklistPermissionContext permissionContext
  ) {
    if (!matchesScopeAction(entity.getOwnerScopeId(), permissionContext, this::hasDeleteAccess)) {
      throw new AccessDeniedException("no permission to delete checklist");
    }
  }

  private void ensureOwnerScopeChangeAllowed(
      BizChecklistEntity entity,
      Long nextOwnerScopeId,
      ChecklistPermissionContext permissionContext
  ) {
    if (Objects.equals(entity.getOwnerScopeId(), nextOwnerScopeId)) {
      return;
    }
    if (!matchesScopeAction(entity.getOwnerScopeId(), permissionContext, this::hasManageAccess)) {
      throw new AccessDeniedException("no permission to move checklist to another owner scope");
    }
    ensureCanCreate(nextOwnerScopeId, permissionContext);
  }

  private boolean matchesScopeAction(
      Long scopeId,
      ChecklistPermissionContext permissionContext,
      Predicate<ResourceScopeMemberDto> predicate
  ) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    ResourceScopeMemberDto member = permissionContext.memberships().get(String.valueOf(scopeId));
    return member != null && predicate.test(member);
  }

  private boolean hasAnyOwnerAccess(Long scopeId, ChecklistPermissionContext permissionContext) {
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

  private boolean hasViewAccess(String scopeId, ChecklistPermissionContext permissionContext) {
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

  private record ChecklistPermissionContext(
      boolean superAdmin,
      Map<String, ResourceScopeMemberDto> memberships
  ) {
  }

  private ChecklistDto toDto(BizChecklistEntity entity, List<BizChecklistItemEntity> items) {
    List<BizChecklistItemEntity> sortedItems = items.stream()
        .sorted(Comparator.comparing(BizChecklistItemEntity::getSequenceNo, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(BizChecklistItemEntity::getId))
        .toList();
    return new ChecklistDto(
        String.valueOf(entity.getId()),
        entity.getChecklistCode(),
        entity.getChecklistName(),
        entity.getDescription(),
        entity.getChecklistVersion(),
        entity.getOwnerScopeId() == null ? null : String.valueOf(entity.getOwnerScopeId()),
        splitCsv(entity.getSharedScopeIds()).stream()
            .map(scopeId -> new ChecklistDto.ScopeGrantDto(scopeId, List.of("view")))
            .toList(),
        sortedItems.stream().map(this::toItemDto).toList(),
        entity.getStatus(),
        entity.getUploadDate() == null ? null : entity.getUploadDate().toString(),
        DateTimeFormatters.formatDateTime(entity.getCreatedAt()),
        DateTimeFormatters.formatDateTime(entity.getUpdatedAt())
    );
  }

  private ChecklistItemDto toItemDto(BizChecklistItemEntity entity) {
    return new ChecklistItemDto(
        String.valueOf(entity.getId()),
        String.valueOf(entity.getChecklistId()),
        entity.getSequenceNo(),
        entity.getContent(),
        entity.getCriterion(),
        entity.getControlFrequency(),
        entity.getEvaluationType(),
        splitCsv(entity.getOrganizationIds())
    );
  }

  private Long nextId(Object entity) {
    Object nextId = identifierGenerator.nextId(entity);
    if (nextId instanceof Number number) {
      return number.longValue();
    }
    return Long.valueOf(String.valueOf(nextId));
  }

  private Long parseId(String id) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException exception) {
      throw new BusinessException("CHECKLIST_ID_INVALID", "invalid checklist id: " + id);
    }
  }

  private Long parseNullableItemId(String id) {
    String normalized = trimToNull(id);
    return normalized == null ? null : parseId(normalized);
  }

  private LocalDate parseDateOrToday(String date) {
    if (date == null || date.isBlank()) {
      return LocalDate.now();
    }
    try {
      return LocalDate.parse(date.trim());
    } catch (DateTimeParseException exception) {
      throw new BusinessException("CHECKLIST_UPLOAD_DATE_INVALID", "invalid checklist upload date: " + date);
    }
  }

  private String normalizeRequiredText(String value, String code) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw new BusinessException(code, code);
    }
    return normalized;
  }

  private String normalizeFilterText(String value) {
    return trimToNull(value);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private boolean containsIgnoreCase(String value, String keyword) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
  }

  private String joinCsv(List<String> values) {
    if (values == null) {
      return "";
    }
    return values.stream()
        .map(this::trimToNull)
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.joining(","));
  }

  private String joinScopeIds(List<String> values, Long ownerScopeId) {
    if (values == null) {
      return "";
    }
    return values.stream()
        .map(this::trimToNull)
        .filter(Objects::nonNull)
        .map(this::parseId)
        .filter(scopeId -> !scopeId.equals(ownerScopeId))
        .distinct()
        .map(String::valueOf)
        .collect(Collectors.joining(","));
  }

  private List<String> splitCsv(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .toList();
  }
}
