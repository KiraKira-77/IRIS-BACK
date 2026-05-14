package com.iris.back.business.plan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.plan.mapper.BizPlanItemMapper;
import com.iris.back.business.plan.mapper.BizPlanMapper;
import com.iris.back.business.plan.model.dto.PlanDto;
import com.iris.back.business.plan.model.dto.PlanItemDto;
import com.iris.back.business.plan.model.entity.BizPlanEntity;
import com.iris.back.business.plan.model.entity.BizPlanItemEntity;
import com.iris.back.business.plan.model.request.PlanItemUpsertRequest;
import com.iris.back.business.plan.model.request.PlanListQuery;
import com.iris.back.business.plan.model.request.PlanUpsertRequest;
import com.iris.back.business.project.mapper.BizProjectMapper;
import com.iris.back.business.project.model.entity.BizProjectEntity;
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
public class PlanService {

  private final BizPlanMapper planMapper;
  private final BizPlanItemMapper planItemMapper;
  private final BizProjectMapper projectMapper;
  private final CurrentUserContext currentUserContext;
  private final SysResourceScopeMemberMapper resourceScopeMemberMapper;
  private final IdentifierGenerator identifierGenerator;

  public PlanService(
      BizPlanMapper planMapper,
      BizPlanItemMapper planItemMapper,
      BizProjectMapper projectMapper,
      CurrentUserContext currentUserContext,
      SysResourceScopeMemberMapper resourceScopeMemberMapper,
      IdentifierGenerator identifierGenerator
  ) {
    this.planMapper = planMapper;
    this.planItemMapper = planItemMapper;
    this.projectMapper = projectMapper;
    this.currentUserContext = currentUserContext;
    this.resourceScopeMemberMapper = resourceScopeMemberMapper;
    this.identifierGenerator = identifierGenerator;
  }

  public PageResponse<PlanDto> list(PlanListQuery query) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    PlanListQuery safeQuery = query == null
        ? new PlanListQuery(null, null, null, 1L, 10L)
        : query;
    List<BizPlanEntity> entities = planMapper.selectList(new LambdaQueryWrapper<BizPlanEntity>()
        .eq(BizPlanEntity::getTenantId, principal.tenantId())
        .orderByDesc(BizPlanEntity::getUpdatedAt)
        .orderByDesc(BizPlanEntity::getId));
    Map<Long, List<BizPlanItemEntity>> itemsByPlanId = loadItems(principal.tenantId(), entities);
    Map<Long, BizProjectEntity> projectById = loadProjects(
        principal.tenantId(),
        itemsByPlanId.values().stream().flatMap(List::stream).toList()
    );
    Map<Long, String> statusByPlanId = deriveStatuses(entities, itemsByPlanId, projectById);
    PlanPermissionContext permissionContext = buildPermissionContext(principal);
    List<PlanDto> filtered = entities.stream()
        .map(entity -> toDto(
            entity,
            itemsByPlanId.getOrDefault(entity.getId(), List.of()),
            statusByPlanId.getOrDefault(entity.getId(), normalizeStatus(entity.getStatus(), null))
        ))
        .filter(item -> canView(item, permissionContext))
        .filter(item -> matches(item, safeQuery))
        .toList();

    long pageNo = safeQuery.normalizedPage();
    long pageSize = safeQuery.normalizedPageSize();
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * pageSize);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + pageSize);
    return PageResponse.of(filtered.size(), pageNo, pageSize, filtered.subList(fromIndex, toIndex));
  }

  public PlanDto get(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizPlanEntity entity = requirePlan(parseId(id, "PLAN_ID_INVALID"), principal.tenantId());
    ensureCanView(entity, buildPermissionContext(principal));
    return toDto(entity, buildPlanStatusContext(principal.tenantId(), entity));
  }

  @Transactional
  public PlanDto create(PlanUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizPlanEntity entity = new BizPlanEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(principal.tenantId());
    applyFields(entity, request, true);
    PlanPermissionContext permissionContext = buildPermissionContext(principal);
    ensureCanEditOwnerScope(entity.getOwnerScopeId(), permissionContext, "create plan in selected scope");
    if (entity.getParentId() != null) {
      BizPlanEntity parent = requirePlan(entity.getParentId(), principal.tenantId());
      ensureCanEditOwnerScope(parent.getOwnerScopeId(), permissionContext, "create child plan");
    }
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(principal.userId());
    entity.setUpdatedBy(principal.userId());
    planMapper.insert(entity);
    List<BizPlanItemEntity> items = replaceItems(entity.getId(), principal, request.items());
    return toDto(entity, items, deriveLeafStatus(entity, items, loadProjects(principal.tenantId(), items)));
  }

  @Transactional
  public PlanDto update(String id, PlanUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizPlanEntity entity = requirePlan(parseId(id, "PLAN_ID_INVALID"), principal.tenantId());
    PlanPermissionContext permissionContext = buildPermissionContext(principal);
    ensureCanEdit(entity, permissionContext);
    Long nextOwnerScopeId = parseId(normalizeRequiredText(request.ownerScopeId(), "PLAN_OWNER_SCOPE_REQUIRED"),
        "PLAN_OWNER_SCOPE_INVALID");
    ensureOwnerScopeChangeAllowed(entity, nextOwnerScopeId, permissionContext);
    applyFields(entity, request, false);
    entity.setUpdatedBy(principal.userId());
    planMapper.updateById(entity);
    List<BizPlanItemEntity> items = replaceItems(entity.getId(), principal, request.items());
    return toDto(entity, buildPlanStatusContext(principal.tenantId(), entity));
  }

  @Transactional
  public void delete(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizPlanEntity entity = requirePlan(parseId(id, "PLAN_ID_INVALID"), principal.tenantId());
    List<BizPlanEntity> plans = nullToList(planMapper.selectList(new LambdaQueryWrapper<BizPlanEntity>()
        .eq(BizPlanEntity::getTenantId, principal.tenantId())));
    if (plans.stream().noneMatch(plan -> Objects.equals(plan.getId(), entity.getId()))) {
      plans = java.util.stream.Stream.concat(plans.stream(), java.util.stream.Stream.of(entity)).toList();
    }
    Set<Long> planIds = collectPlanSubtreeIds(entity.getId(), plans);
    PlanPermissionContext permissionContext = buildPermissionContext(principal);
    plans.stream()
        .filter(plan -> planIds.contains(plan.getId()))
        .forEach(plan -> ensureCanEdit(plan, permissionContext));
    List<BizPlanItemEntity> items = nullToList(planItemMapper.selectList(new LambdaQueryWrapper<BizPlanItemEntity>()
        .eq(BizPlanItemEntity::getTenantId, principal.tenantId())
        .in(BizPlanItemEntity::getPlanId, planIds)));
    if (items.stream().anyMatch(item -> trimToNull(item.getProjectId()) != null)) {
      throw new BusinessException("PLAN_LINKED_PROJECT_DELETE_FORBIDDEN", "PLAN_LINKED_PROJECT_DELETE_FORBIDDEN");
    }
    planItemMapper.delete(new LambdaQueryWrapper<BizPlanItemEntity>()
        .eq(BizPlanItemEntity::getTenantId, principal.tenantId())
        .in(BizPlanItemEntity::getPlanId, planIds));
    planIds.forEach(planMapper::deleteById);
  }

  @Transactional
  public PlanDto submit(String id) {
    return updateStatus(id, "approved", true);
  }

  @Transactional
  public PlanDto approve(String id) {
    return updateStatus(id, "approved", true);
  }

  private PlanDto updateStatus(String id, String status, boolean approve) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizPlanEntity entity = requirePlan(parseId(id, "PLAN_ID_INVALID"), principal.tenantId());
    ensureCanEdit(entity, buildPermissionContext(principal));
    entity.setStatus(status);
    entity.setUpdatedBy(principal.userId());
    if (approve) {
      entity.setApprovedBy(principal.userId());
    }
    planMapper.updateById(entity);
    return toDto(entity, buildPlanStatusContext(principal.tenantId(), entity));
  }

  private void applyFields(BizPlanEntity entity, PlanUpsertRequest request, boolean create) {
    entity.setPlanName(normalizeRequiredText(request.name(), "PLAN_NAME_REQUIRED"));
    entity.setCycle(normalizeRequiredText(request.cycle(), "PLAN_CYCLE_REQUIRED"));
    if (request.year() == null) {
      throw new BusinessException("PLAN_YEAR_REQUIRED", "PLAN_YEAR_REQUIRED");
    }
    entity.setPlanYear(request.year());
    entity.setPeriod(normalizeRequiredText(request.period(), "PLAN_PERIOD_REQUIRED"));
    entity.setStatus(normalizeStatus(request.status(), entity.getStatus()));
    entity.setDescription(trimToNull(request.description()));
    Long ownerScopeId = parseId(normalizeRequiredText(request.ownerScopeId(), "PLAN_OWNER_SCOPE_REQUIRED"),
        "PLAN_OWNER_SCOPE_INVALID");
    entity.setOwnerScopeId(ownerScopeId);
    entity.setSharedScopeIds(joinScopeIds(request.grantScopeIds(), ownerScopeId));
    entity.setParentId(parseNullableId(request.parentId(), "PLAN_PARENT_ID_INVALID"));

    String code = trimToNull(request.code());
    if (code != null) {
      entity.setPlanCode(code);
    } else if (create || trimToNull(entity.getPlanCode()) == null) {
      entity.setPlanCode("PL-" + request.year() + "-" + entity.getId());
    }
  }

  private String normalizeStatus(String status, String currentStatus) {
    String normalized = trimToNull(status);
    if (normalized != null) {
      return normalized;
    }
    return trimToNull(currentStatus) == null ? "draft" : currentStatus;
  }

  private List<BizPlanItemEntity> replaceItems(
      Long planId,
      CurrentUserPrincipal principal,
      List<PlanItemUpsertRequest> requests
  ) {
    planItemMapper.delete(new LambdaQueryWrapper<BizPlanItemEntity>()
        .eq(BizPlanItemEntity::getTenantId, principal.tenantId())
        .eq(BizPlanItemEntity::getPlanId, planId));
    if (requests == null || requests.isEmpty()) {
      return List.of();
    }

    List<BizPlanItemEntity> items = new java.util.ArrayList<>();
    for (int index = 0; index < requests.size(); index++) {
      PlanItemUpsertRequest request = requests.get(index);
      BizPlanItemEntity item = new BizPlanItemEntity();
      item.setId(nextId(item));
      item.setTenantId(principal.tenantId());
      item.setPlanId(planId);
      item.setSequenceNo(index + 1);
      item.setTargetScope(normalizeRequiredText(request.targetScope(), "PLAN_ITEM_TARGET_SCOPE_REQUIRED"));
      item.setChecklistIds(joinCsv(request.checklistIds()));
      item.setPlannedStartDate(parseRequiredDate(request.plannedStartDate(), "PLAN_ITEM_START_DATE_INVALID"));
      item.setPlannedEndDate(parseRequiredDate(request.plannedEndDate(), "PLAN_ITEM_END_DATE_INVALID"));
      item.setAssignee(trimToNull(request.assignee()));
      item.setProjectId(trimToNull(request.projectId()));
      item.setRemark(trimToNull(request.remark()));
      item.setDeleted(0);
      item.setVersion(0L);
      item.setCreatedBy(principal.userId());
      item.setUpdatedBy(principal.userId());
      planItemMapper.insert(item);
      items.add(item);
    }
    return items;
  }

  private Map<Long, List<BizPlanItemEntity>> loadItems(Long tenantId, List<BizPlanEntity> plans) {
    if (plans.isEmpty()) {
      return Map.of();
    }
    List<Long> planIds = plans.stream().map(BizPlanEntity::getId).toList();
    return planItemMapper.selectList(new LambdaQueryWrapper<BizPlanItemEntity>()
            .eq(BizPlanItemEntity::getTenantId, tenantId)
            .in(BizPlanItemEntity::getPlanId, planIds)
            .orderByAsc(BizPlanItemEntity::getSequenceNo)
            .orderByAsc(BizPlanItemEntity::getId))
        .stream()
        .collect(Collectors.groupingBy(BizPlanItemEntity::getPlanId));
  }

  private Map<Long, BizProjectEntity> loadProjects(Long tenantId, List<BizPlanItemEntity> items) {
    Set<Long> projectIds = linkedProjectIds(items);
    if (projectIds.isEmpty()) {
      return Map.of();
    }
    List<BizProjectEntity> projects = projectMapper.selectList(new LambdaQueryWrapper<BizProjectEntity>()
        .eq(BizProjectEntity::getTenantId, tenantId)
        .in(BizProjectEntity::getId, projectIds));
    if (projects == null || projects.isEmpty()) {
      return Map.of();
    }
    return projects.stream()
        .collect(Collectors.toMap(BizProjectEntity::getId, Function.identity(), (left, right) -> left));
  }

  private boolean matches(PlanDto item, PlanListQuery query) {
    String keyword = trimToNull(query.keyword());
    String status = trimToNull(query.status());
    return (keyword == null
        || containsIgnoreCase(item.name(), keyword)
        || containsIgnoreCase(item.code(), keyword)
        || containsIgnoreCase(item.description(), keyword))
        && (query.year() == null || Objects.equals(item.year(), query.year()))
        && (status == null || status.equalsIgnoreCase(item.status()));
  }

  private BizPlanEntity requirePlan(Long id, Long tenantId) {
    BizPlanEntity entity = planMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BusinessException("PLAN_NOT_FOUND", "plan not found: " + id);
    }
    return entity;
  }

  private PlanPermissionContext buildPermissionContext(CurrentUserPrincipal principal) {
    if (isSuperAdmin(principal)) {
      return new PlanPermissionContext(true, Map.of());
    }

    Map<String, ResourceScopeMemberDto> memberships = nullToList(resourceScopeMemberMapper
            .selectByTenantIdAndUserId(principal.tenantId(), principal.userId()))
        .stream()
        .collect(Collectors.toMap(ResourceScopeMemberDto::scopeId, member -> member, (left, right) -> left));

    return new PlanPermissionContext(false, memberships);
  }

  private boolean canView(BizPlanEntity entity, PlanPermissionContext permissionContext) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    if (hasAnyOwnerAccess(entity.getOwnerScopeId(), permissionContext)) {
      return true;
    }
    return splitCsv(entity.getSharedScopeIds()).stream()
        .anyMatch(scopeId -> hasViewAccess(scopeId, permissionContext));
  }

  private boolean canView(PlanDto dto, PlanPermissionContext permissionContext) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    if (hasAnyOwnerAccess(parseId(dto.ownerScopeId(), "PLAN_OWNER_SCOPE_INVALID"), permissionContext)) {
      return true;
    }
    return dto.grants().stream()
        .map(PlanDto.ScopeGrantDto::scopeId)
        .anyMatch(scopeId -> hasViewAccess(scopeId, permissionContext));
  }

  private void ensureCanView(BizPlanEntity entity, PlanPermissionContext permissionContext) {
    if (!canView(entity, permissionContext)) {
      throw new AccessDeniedException("no permission to view plan");
    }
  }

  private void ensureCanEdit(BizPlanEntity entity, PlanPermissionContext permissionContext) {
    ensureCanEditOwnerScope(entity.getOwnerScopeId(), permissionContext, "edit plan");
  }

  private void ensureCanEditOwnerScope(
      Long ownerScopeId,
      PlanPermissionContext permissionContext,
      String action
  ) {
    if (!matchesScopeAction(ownerScopeId, permissionContext, this::hasEditAccess)) {
      throw new AccessDeniedException("no permission to " + action);
    }
  }

  private void ensureOwnerScopeChangeAllowed(
      BizPlanEntity entity,
      Long nextOwnerScopeId,
      PlanPermissionContext permissionContext
  ) {
    if (Objects.equals(entity.getOwnerScopeId(), nextOwnerScopeId)) {
      return;
    }
    if (!matchesScopeAction(entity.getOwnerScopeId(), permissionContext, this::hasManageAccess)) {
      throw new AccessDeniedException("no permission to move plan to another owner scope");
    }
    ensureCanEditOwnerScope(nextOwnerScopeId, permissionContext, "move plan to selected scope");
  }

  private boolean matchesScopeAction(
      Long scopeId,
      PlanPermissionContext permissionContext,
      Predicate<ResourceScopeMemberDto> predicate
  ) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    ResourceScopeMemberDto member = permissionContext.memberships().get(String.valueOf(scopeId));
    return member != null && predicate.test(member);
  }

  private boolean hasAnyOwnerAccess(Long scopeId, PlanPermissionContext permissionContext) {
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

  private boolean hasViewAccess(String scopeId, PlanPermissionContext permissionContext) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    ResourceScopeMemberDto member = permissionContext.memberships().get(scopeId);
    return member != null && (flag(member.canView()) || flag(member.canManage()));
  }

  private boolean hasEditAccess(ResourceScopeMemberDto member) {
    return flag(member.canEdit()) || flag(member.canManage());
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

  private PlanDto toDto(
      BizPlanEntity entity,
      PlanStatusContext statusContext
  ) {
    return toDto(
        entity,
        statusContext.itemsByPlanId().getOrDefault(entity.getId(), List.of()),
        statusContext.statusByPlanId().getOrDefault(entity.getId(), normalizeStatus(entity.getStatus(), null))
    );
  }

  private PlanDto toDto(
      BizPlanEntity entity,
      List<BizPlanItemEntity> items,
      String status
  ) {
    List<BizPlanItemEntity> sortedItems = items.stream()
        .sorted(Comparator.comparing(BizPlanItemEntity::getSequenceNo, Comparator.nullsLast(Integer::compareTo))
            .thenComparing(BizPlanItemEntity::getId))
        .toList();
    return new PlanDto(
        String.valueOf(entity.getId()),
        entity.getPlanCode(),
        entity.getPlanName(),
        entity.getCycle(),
        entity.getPlanYear(),
        entity.getPeriod(),
        status,
        entity.getDescription(),
        entity.getOwnerScopeId() == null ? null : String.valueOf(entity.getOwnerScopeId()),
        splitCsv(entity.getSharedScopeIds()).stream()
            .map(scopeId -> new PlanDto.ScopeGrantDto(scopeId, List.of("view")))
            .toList(),
        sortedItems.stream().map(this::toItemDto).toList(),
        entity.getParentId() == null ? null : String.valueOf(entity.getParentId()),
        List.of(),
        entity.getCreatedBy() == null ? null : String.valueOf(entity.getCreatedBy()),
        entity.getApprovedBy() == null ? null : String.valueOf(entity.getApprovedBy()),
        DateTimeFormatters.formatDateTime(entity.getCreatedAt()),
        DateTimeFormatters.formatDateTime(entity.getUpdatedAt())
    );
  }

  private PlanStatusContext buildPlanStatusContext(Long tenantId, BizPlanEntity currentPlan) {
    List<BizPlanEntity> plans = nullToList(planMapper.selectList(new LambdaQueryWrapper<BizPlanEntity>()
        .eq(BizPlanEntity::getTenantId, tenantId)));
    if (plans.stream().noneMatch(plan -> Objects.equals(plan.getId(), currentPlan.getId()))) {
      plans = java.util.stream.Stream.concat(plans.stream(), java.util.stream.Stream.of(currentPlan)).toList();
    }
    Map<Long, List<BizPlanItemEntity>> itemsByPlanId = loadItems(tenantId, plans);
    Map<Long, BizProjectEntity> projectById = loadProjects(
        tenantId,
        itemsByPlanId.values().stream().flatMap(List::stream).toList()
    );
    return new PlanStatusContext(itemsByPlanId, deriveStatuses(plans, itemsByPlanId, projectById));
  }

  private Set<Long> collectPlanSubtreeIds(Long rootPlanId, List<BizPlanEntity> plans) {
    Map<Long, List<BizPlanEntity>> childrenByParentId = plans.stream()
        .filter(plan -> plan.getParentId() != null)
        .collect(Collectors.groupingBy(BizPlanEntity::getParentId));
    Set<Long> planIds = new java.util.LinkedHashSet<>();
    java.util.ArrayDeque<Long> pendingIds = new java.util.ArrayDeque<>();
    pendingIds.add(rootPlanId);
    while (!pendingIds.isEmpty()) {
      Long currentId = pendingIds.removeFirst();
      if (!planIds.add(currentId)) {
        continue;
      }
      childrenByParentId.getOrDefault(currentId, List.of()).stream()
          .map(BizPlanEntity::getId)
          .forEach(pendingIds::addLast);
    }
    return planIds;
  }

  private Map<Long, String> deriveStatuses(
      List<BizPlanEntity> plans,
      Map<Long, List<BizPlanItemEntity>> itemsByPlanId,
      Map<Long, BizProjectEntity> projectById
  ) {
    Map<Long, List<BizPlanEntity>> childrenByParentId = plans.stream()
        .filter(plan -> plan.getParentId() != null)
        .collect(Collectors.groupingBy(BizPlanEntity::getParentId));
    Map<Long, String> statusByPlanId = new java.util.HashMap<>();
    for (BizPlanEntity plan : plans) {
      deriveStatus(plan, itemsByPlanId, projectById, childrenByParentId, statusByPlanId);
    }
    return statusByPlanId;
  }

  private String deriveStatus(
      BizPlanEntity entity,
      Map<Long, List<BizPlanItemEntity>> itemsByPlanId,
      Map<Long, BizProjectEntity> projectById,
      Map<Long, List<BizPlanEntity>> childrenByParentId,
      Map<Long, String> statusByPlanId
  ) {
    String existing = statusByPlanId.get(entity.getId());
    if (existing != null) {
      return existing;
    }
    if ("draft".equalsIgnoreCase(trimToNull(entity.getStatus()))) {
      statusByPlanId.put(entity.getId(), "draft");
      return "draft";
    }
    List<BizPlanEntity> children = childrenByParentId.getOrDefault(entity.getId(), List.of());
    if (!children.isEmpty()) {
      String status = deriveParentStatus(children.stream()
          .map(child -> deriveStatus(child, itemsByPlanId, projectById, childrenByParentId, statusByPlanId))
          .toList());
      statusByPlanId.put(entity.getId(), status);
      return status;
    }
    String status = deriveLeafStatus(entity, itemsByPlanId.getOrDefault(entity.getId(), List.of()), projectById);
    statusByPlanId.put(entity.getId(), status);
    return status;
  }

  private String deriveLeafStatus(
      BizPlanEntity entity,
      List<BizPlanItemEntity> items,
      Map<Long, BizProjectEntity> projectById
  ) {
    if ("draft".equalsIgnoreCase(trimToNull(entity.getStatus()))) {
      return "draft";
    }
    Set<Long> projectIds = linkedProjectIds(items);
    if (projectIds.isEmpty()) {
      return "approved";
    }
    boolean hasArchivedProject = projectIds.stream()
        .map(projectById::get)
        .filter(Objects::nonNull)
        .anyMatch(this::isArchivedProject);
    if (hasArchivedProject) {
      return "archived";
    }
    boolean allKnownProjectsCompleted = projectIds.stream()
        .map(projectById::get)
        .allMatch(project -> project != null && isCompletedProject(project));
    if (allKnownProjectsCompleted) {
      return "completed";
    }
    return "in_progress";
  }

  private String deriveParentStatus(List<String> childStatuses) {
    List<String> effectiveStatuses = childStatuses.stream()
        .map(status -> "draft".equalsIgnoreCase(status) ? "approved" : status)
        .toList();
    if (effectiveStatuses.stream().allMatch(status -> "archived".equalsIgnoreCase(status))) {
      return "archived";
    }
    if (effectiveStatuses.stream().allMatch(status -> "approved".equalsIgnoreCase(status))) {
      return "approved";
    }
    if (effectiveStatuses.stream()
        .allMatch(status -> "completed".equalsIgnoreCase(status) || "archived".equalsIgnoreCase(status))) {
      return "completed";
    }
    return "in_progress";
  }

  private Set<Long> linkedProjectIds(List<BizPlanItemEntity> items) {
    return items.stream()
        .map(BizPlanItemEntity::getProjectId)
        .map(this::parseLinkedProjectId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private Long parseLinkedProjectId(String projectId) {
    String normalized = trimToNull(projectId);
    if (normalized == null) {
      return null;
    }
    try {
      return Long.valueOf(normalized);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  private boolean isArchivedProject(BizProjectEntity project) {
    return "archived".equalsIgnoreCase(trimToNull(project.getStatus()))
        || "completed".equalsIgnoreCase(trimToNull(project.getArchiveStatus()))
        || "archived".equalsIgnoreCase(trimToNull(project.getArchiveStatus()));
  }

  private boolean isCompletedProject(BizProjectEntity project) {
    return "completed".equalsIgnoreCase(trimToNull(project.getStatus()));
  }

  private PlanItemDto toItemDto(BizPlanItemEntity entity) {
    return new PlanItemDto(
        String.valueOf(entity.getId()),
        String.valueOf(entity.getPlanId()),
        entity.getSequenceNo(),
        entity.getTargetScope(),
        splitCsv(entity.getChecklistIds()),
        entity.getPlannedStartDate() == null ? null : entity.getPlannedStartDate().toString(),
        entity.getPlannedEndDate() == null ? null : entity.getPlannedEndDate().toString(),
        entity.getAssignee(),
        entity.getRemark(),
        entity.getProjectId()
    );
  }

  private Long nextId(Object entity) {
    Object nextId = identifierGenerator.nextId(entity);
    if (nextId instanceof Number number) {
      return number.longValue();
    }
    return Long.valueOf(String.valueOf(nextId));
  }

  private LocalDate parseRequiredDate(String date, String code) {
    String normalized = normalizeRequiredText(date, code);
    try {
      return LocalDate.parse(normalized);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(code, code);
    }
  }

  private Long parseNullableId(String id, String code) {
    String normalized = trimToNull(id);
    return normalized == null ? null : parseId(normalized, code);
  }

  private Long parseId(String id, String code) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException exception) {
      throw new BusinessException(code, "invalid plan id: " + id);
    }
  }

  private String normalizeRequiredText(String value, String code) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw new BusinessException(code, code);
    }
    return normalized;
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
        .map(value -> parseId(value, "PLAN_SHARED_SCOPE_INVALID"))
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

  private <T> List<T> nullToList(List<T> values) {
    return values == null ? List.of() : values;
  }

  private record PlanStatusContext(
      Map<Long, List<BizPlanItemEntity>> itemsByPlanId,
      Map<Long, String> statusByPlanId
  ) {
  }

  private record PlanPermissionContext(
      boolean superAdmin,
      Map<String, ResourceScopeMemberDto> memberships
  ) {
  }
}
