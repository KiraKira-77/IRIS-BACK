package com.iris.back.business.standard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.business.standard.mapper.BizStandardMapper;
import com.iris.back.business.standard.model.dto.StandardDto;
import com.iris.back.business.standard.model.entity.BizStandardEntity;
import com.iris.back.business.standard.model.request.StandardListQuery;
import com.iris.back.business.standard.model.request.StandardRollbackRequest;
import com.iris.back.business.standard.model.request.StandardUpgradeRequest;
import com.iris.back.business.standard.model.request.StandardUpsertRequest;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.common.model.PageResponse;
import com.iris.back.common.util.DateTimeFormatters;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import com.iris.back.system.model.dto.FileAttachmentDto;
import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.mapper.SysResourceScopeMapper;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.model.dto.ResourceScopeMemberDto;
import com.iris.back.system.model.entity.SysResourceScopeEntity;
import com.iris.back.system.model.entity.SysUserEntity;
import com.iris.back.system.service.FileService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StandardService {

  private static final String BIZ_TYPE_STANDARD = "STANDARD";

  private final BizStandardMapper standardMapper;
  private final SysResourceScopeMapper resourceScopeMapper;
  private final SysResourceScopeMemberMapper resourceScopeMemberMapper;
  private final SysUserMapper userMapper;
  private final FileService fileService;
  private final CurrentUserContext currentUserContext;
  private final IdentifierGenerator identifierGenerator;

  public StandardService(
      BizStandardMapper standardMapper,
      SysResourceScopeMapper resourceScopeMapper,
      SysResourceScopeMemberMapper resourceScopeMemberMapper,
      SysUserMapper userMapper,
      FileService fileService,
      CurrentUserContext currentUserContext,
      IdentifierGenerator identifierGenerator
  ) {
    this.standardMapper = standardMapper;
    this.resourceScopeMapper = resourceScopeMapper;
    this.resourceScopeMemberMapper = resourceScopeMemberMapper;
    this.userMapper = userMapper;
    this.fileService = fileService;
    this.currentUserContext = currentUserContext;
    this.identifierGenerator = identifierGenerator;
  }

  public List<StandardDto> list() {
    return listAllVisibleDtos();
  }

  public PageResponse<StandardDto> list(StandardListQuery query) {
    StandardListQuery safeQuery = query == null
        ? new StandardListQuery(null, null, null, 1L, 10L)
        : query;
    List<StandardDto> filtered = applyListFilters(listAllVisibleDtos(), safeQuery);
    filtered = withVersionCounts(filtered);
    if (normalizeFilterText(safeQuery.status()) == null) {
      filtered = pickLatestVersionDtos(filtered);
    }

    long pageNo = safeQuery.normalizedPage();
    long pageSize = safeQuery.normalizedPageSize();
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * pageSize);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + pageSize);

    return PageResponse.of(
        filtered.size(),
        pageNo,
        pageSize,
        filtered.subList(fromIndex, toIndex)
    );
  }

  private List<StandardDto> listAllVisibleDtos() {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    StandardPermissionContext permissionContext = buildPermissionContext(principal);

    return standardMapper.selectList(new LambdaQueryWrapper<BizStandardEntity>()
            .eq(BizStandardEntity::getTenantId, principal.tenantId())
        .orderByAsc(BizStandardEntity::getId))
        .stream()
        .filter(entity -> canView(entity, permissionContext))
        .sorted(Comparator.comparing(BizStandardEntity::getId))
        .collect(Collectors.collectingAndThen(Collectors.toList(), standards -> {
          Map<Long, String> operatorNames = loadOperatorNames(standards);
          Map<Long, List<FileAttachmentDto>> attachments = fileService.listByBizIds(
              principal.tenantId(),
              BIZ_TYPE_STANDARD,
              standards.stream().map(BizStandardEntity::getId).toList()
          );
          return standards.stream()
              .map(entity -> toDto(entity, operatorNames, attachments.getOrDefault(entity.getId(), List.of())))
              .toList();
        }));
  }

  private List<StandardDto> applyListFilters(List<StandardDto> standards, StandardListQuery query) {
    String keyword = normalizeFilterText(query.keyword());
    String category = normalizeFilterText(query.category());
    String status = normalizeFilterText(query.status());

    return standards.stream()
        .filter(item -> keyword == null
            || containsIgnoreCase(item.title(), keyword)
            || containsIgnoreCase(item.standardCode(), keyword))
        .filter(item -> category == null || category.equalsIgnoreCase(item.category()))
        .filter(item -> status == null || status.equalsIgnoreCase(item.status()))
        .toList();
  }

  private List<StandardDto> pickLatestVersionDtos(List<StandardDto> standards) {
    Map<String, StandardDto> latestByGroup = new LinkedHashMap<>();
    for (StandardDto standard : standards) {
      StandardDto existing = latestByGroup.get(standard.standardGroupId());
      if (existing == null || compareVersionOrder(standard, existing) > 0) {
        latestByGroup.put(standard.standardGroupId(), standard);
      }
    }
    return List.copyOf(latestByGroup.values());
  }

  private int compareVersionOrder(StandardDto left, StandardDto right) {
    int leftVersion = left.versionNumber() == null ? 0 : left.versionNumber();
    int rightVersion = right.versionNumber() == null ? 0 : right.versionNumber();
    if (leftVersion != rightVersion) {
      return Integer.compare(leftVersion, rightVersion);
    }
    return Long.compare(parseId(left.id()), parseId(right.id()));
  }

  private boolean containsIgnoreCase(String value, String keyword) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
  }

  private String normalizeFilterText(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public StandardDto get(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity entity = requireStandard(parseId(id), principal.tenantId());
    ensureCanView(entity, buildPermissionContext(principal));
    return toDto(
        entity,
        loadOperatorNames(List.of(entity)),
        fileService.listByBizId(principal.tenantId(), BIZ_TYPE_STANDARD, entity.getId())
    );
  }

  public StandardDto create(StandardUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ensureTenantAccess(request.tenantId(), principal.tenantId());
    StandardPermissionContext permissionContext = buildPermissionContext(principal);
    Long ownerScopeId = requireOwnerScope(parseId(request.ownerScopeId()), request.tenantId()).getId();
    ensureCanCreate(ownerScopeId, permissionContext);
    Long previousVersionId = parseOptionalId(request.previousVersionId());
    BizStandardEntity previousVersion = previousVersionId == null
        ? null
        : requireStandard(previousVersionId, principal.tenantId());
    BizStandardEntity entity = new BizStandardEntity();
    long nextId = nextId(entity);
    entity.setId(nextId);
    entity.setTenantId(request.tenantId());
    entity.setStandardGroupId(request.standardGroupId() == null || request.standardGroupId().isBlank()
        ? previousVersion == null ? String.valueOf(nextId) : previousVersion.getStandardGroupId()
        : request.standardGroupId());
    applyFields(entity, request, ownerScopeId);
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(principal.userId());
    entity.setUpdatedBy(principal.userId());
    standardMapper.insert(entity);
    if (previousVersion != null) {
      fileService.copyBindings(BIZ_TYPE_STANDARD, request.tenantId(), previousVersion.getId(), entity.getId());
    }
    return toDto(
        entity,
        loadOperatorNames(List.of(entity)),
        fileService.listByBizId(principal.tenantId(), BIZ_TYPE_STANDARD, entity.getId())
    );
  }

  public StandardDto upgrade(String id, StandardUpgradeRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity source = requireStandard(parseId(id), principal.tenantId());
    StandardPermissionContext permissionContext = buildPermissionContext(principal);
    ensureCanEdit(source, permissionContext);
    ensureCanCreate(source.getOwnerScopeId(), permissionContext);

    List<BizStandardEntity> versions = listGroupStandards(principal.tenantId(), source.getStandardGroupId());
    ensureUpgradeSourceIsLatest(source, versions);
    String requestedVersion = normalizeRequiredText(request.version());
    ensureVersionNotDuplicated(source.getStandardGroupId(), requestedVersion, versions);

    BizStandardEntity entity = new BizStandardEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(source.getTenantId());
    entity.setStandardGroupId(source.getStandardGroupId());
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(principal.userId());
    entity.setUpdatedBy(principal.userId());
    entity.setStandardCode(source.getStandardCode());
    entity.setTitle(source.getTitle());
    entity.setCategory(source.getCategory());
    entity.setStandardVersion(requestedVersion);
    entity.setStatus("draft");
    entity.setPublishDate(null);
    entity.setDescription(source.getDescription());
    entity.setVersionNumber(nextVersionNumber(versions));
    entity.setPreviousVersionId(source.getId());
    entity.setVisibilityLevel(source.getVisibilityLevel());
    entity.setOwnerScopeId(requireOwnerScope(source.getOwnerScopeId(), source.getTenantId()).getId());
    entity.setSharedScopeIds(String.join(",", normalizeScopeIds(
        splitCommaValues(source.getSharedScopeIds()),
        source.getOwnerScopeId(),
        source.getTenantId()
    )));
    entity.setChangeLog(normalizeRequiredText(request.changeLog()));
    standardMapper.insert(entity);
    return toDto(entity, loadOperatorNames(List.of(entity)), List.of());
  }

  @Transactional
  public StandardDto publish(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity draft = requireStandard(parseId(id), principal.tenantId());
    StandardPermissionContext permissionContext = buildPermissionContext(principal);
    ensureCanEdit(draft, permissionContext);
    ensureDraftPublishOnly(draft);

    List<BizStandardEntity> versions = listGroupStandards(principal.tenantId(), draft.getStandardGroupId());
    for (BizStandardEntity version : versions) {
      if (!Objects.equals(version.getId(), draft.getId()) && "active".equalsIgnoreCase(version.getStatus())) {
        version.setStatus("archived");
        version.setUpdatedBy(principal.userId());
        standardMapper.updateById(version);
      }
    }

    draft.setStatus("active");
    draft.setPublishDate(LocalDate.now());
    draft.setUpdatedBy(principal.userId());
    standardMapper.updateById(draft);

    return toDto(
        draft,
        loadOperatorNames(List.of(draft)),
        fileService.listByBizId(principal.tenantId(), BIZ_TYPE_STANDARD, draft.getId())
    );
  }

  @Transactional
  public StandardDto rollback(String id, StandardRollbackRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity target = requireStandard(parseId(id), principal.tenantId());
    StandardPermissionContext permissionContext = buildPermissionContext(principal);
    ensureCanView(target, permissionContext);
    ensureCanCreate(target.getOwnerScopeId(), permissionContext);

    List<BizStandardEntity> versions = listGroupStandards(principal.tenantId(), target.getStandardGroupId());
    String requestedVersion = normalizeRequiredText(request.version());
    ensureVersionNotDuplicated(target.getStandardGroupId(), requestedVersion, versions);

    BizStandardEntity entity = new BizStandardEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(target.getTenantId());
    entity.setStandardGroupId(target.getStandardGroupId());
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(principal.userId());
    entity.setUpdatedBy(principal.userId());
    entity.setStandardCode(target.getStandardCode());
    entity.setTitle(target.getTitle());
    entity.setCategory(target.getCategory());
    entity.setStandardVersion(requestedVersion);
    entity.setStatus("draft");
    entity.setPublishDate(null);
    entity.setDescription(target.getDescription());
    entity.setVersionNumber(nextVersionNumber(versions));
    entity.setPreviousVersionId(target.getId());
    entity.setVisibilityLevel(target.getVisibilityLevel());
    entity.setOwnerScopeId(requireOwnerScope(target.getOwnerScopeId(), target.getTenantId()).getId());
    entity.setSharedScopeIds(String.join(",", normalizeScopeIds(
        splitCommaValues(target.getSharedScopeIds()),
        target.getOwnerScopeId(),
        target.getTenantId()
    )));
    entity.setChangeLog("[回退] 回退至 " + target.getStandardVersion() + "：" + normalizeRequiredText(request.reason()));
    standardMapper.insert(entity);
    fileService.copyBindings(BIZ_TYPE_STANDARD, target.getTenantId(), target.getId(), entity.getId());

    return toDto(
        entity,
        loadOperatorNames(List.of(entity)),
        fileService.listByBizId(principal.tenantId(), BIZ_TYPE_STANDARD, entity.getId())
    );
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
    entity.setUpdatedBy(principal.userId());
    standardMapper.updateById(entity);
    return toDto(
        entity,
        loadOperatorNames(List.of(entity)),
        fileService.listByBizId(principal.tenantId(), BIZ_TYPE_STANDARD, entity.getId())
    );
  }

  public void delete(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity entity = requireStandard(parseId(id), principal.tenantId());
    ensureDraftDeleteOnly(entity);
    ensureCanDelete(entity, buildPermissionContext(principal));
    standardMapper.deleteById(entity.getId());
  }

  public FileAttachmentDto uploadAttachment(String id, MultipartFile file) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity entity = requireStandard(parseId(id), principal.tenantId());
    ensureCanEdit(entity, buildPermissionContext(principal));
    return fileService.upload(BIZ_TYPE_STANDARD, principal.tenantId(), entity.getId(), file);
  }

  public void deleteAttachment(String id, String fileId) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity entity = requireStandard(parseId(id), principal.tenantId());
    ensureCanEdit(entity, buildPermissionContext(principal));
    fileService.delete(BIZ_TYPE_STANDARD, principal.tenantId(), entity.getId(), parseId(fileId));
  }

  public List<StandardDto> versions(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizStandardEntity current = requireStandard(parseId(id), principal.tenantId());
    StandardPermissionContext permissionContext = buildPermissionContext(principal);
    ensureCanView(current, permissionContext);
    List<BizStandardEntity> versions = listGroupStandards(principal.tenantId(), current.getStandardGroupId())
        .stream()
        .filter(entity -> canView(entity, permissionContext))
        .sorted(Comparator.comparing(
            (BizStandardEntity entity) -> entity.getVersionNumber() == null ? 0 : entity.getVersionNumber()
        ).reversed())
        .toList();
    Map<Long, String> operatorNames = loadOperatorNames(versions);
    Map<Long, List<FileAttachmentDto>> attachments = fileService.listByBizIds(
        principal.tenantId(),
        BIZ_TYPE_STANDARD,
        versions.stream().map(BizStandardEntity::getId).toList()
    );
    return versions.stream()
        .map(entity -> toDto(entity, operatorNames, attachments.getOrDefault(entity.getId(), List.of())))
        .map(dto -> withVersionCount(dto, versions.size()))
        .toList();
  }

  private List<BizStandardEntity> listGroupStandards(Long tenantId, String standardGroupId) {
    return standardMapper.selectList(new LambdaQueryWrapper<BizStandardEntity>()
        .eq(BizStandardEntity::getTenantId, tenantId)
        .eq(BizStandardEntity::getStandardGroupId, standardGroupId));
  }

  private void ensureUpgradeSourceIsLatest(BizStandardEntity source, List<BizStandardEntity> versions) {
    BizStandardEntity latest = versions.stream()
        .max(Comparator
            .comparing((BizStandardEntity item) -> item.getVersionNumber() == null ? 0 : item.getVersionNumber())
            .thenComparing(BizStandardEntity::getId))
        .orElse(source);
    if (!Objects.equals(latest.getId(), source.getId())) {
      throw new BusinessException(
          "STANDARD_UPGRADE_SOURCE_NOT_LATEST",
          "standard upgrade source is not latest: " + source.getId()
      );
    }
  }

  private void ensureVersionNotDuplicated(
      String standardGroupId,
      String requestedVersion,
      List<BizStandardEntity> versions
  ) {
    boolean duplicated = versions.stream()
        .map(BizStandardEntity::getStandardVersion)
        .filter(Objects::nonNull)
        .map(value -> value.trim().toUpperCase(Locale.ROOT))
        .anyMatch(value -> value.equals(requestedVersion.toUpperCase(Locale.ROOT)));
    if (duplicated) {
      throw new BusinessException(
          "STANDARD_VERSION_DUPLICATE",
          "standard version already exists in group: " + standardGroupId + ", version: " + requestedVersion
      );
    }
  }

  private int nextVersionNumber(List<BizStandardEntity> versions) {
    return versions.stream()
        .map(BizStandardEntity::getVersionNumber)
        .filter(Objects::nonNull)
        .max(Integer::compareTo)
        .orElse(0) + 1;
  }

  private void applyFields(BizStandardEntity entity, StandardUpsertRequest request, Long ownerScopeId) {
    entity.setStandardCode(normalizeRequiredText(request.standardCode()));
    entity.setTitle(request.title());
    entity.setCategory(request.category());
    entity.setStandardVersion(request.version());
    entity.setStatus(request.status());
    entity.setPublishDate(parseDate(request.publishDate()));
    entity.setDescription(request.description());
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
    return requireScope(scopeId, tenantId);
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

  private Map<Long, String> loadOperatorNames(List<BizStandardEntity> standards) {
    List<Long> operatorIds = standards.stream()
        .flatMap(entity -> java.util.stream.Stream.of(entity.getUpdatedBy(), entity.getCreatedBy()))
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (operatorIds.isEmpty()) {
      return Map.of();
    }

    return userMapper.selectBatchIds(operatorIds).stream()
        .collect(Collectors.toMap(SysUserEntity::getId, SysUserEntity::getUsername, (left, right) -> left));
  }

  private StandardDto toDto(
      BizStandardEntity entity,
      Map<Long, String> operatorNames,
      List<FileAttachmentDto> attachments
  ) {
    return new StandardDto(
        String.valueOf(entity.getId()),
        entity.getStandardGroupId(),
        entity.getStandardCode(),
        entity.getTitle(),
        entity.getCategory(),
        entity.getStandardVersion(),
        entity.getPublishDate() == null ? null : entity.getPublishDate().toString(),
        entity.getStatus(),
        attachments,
        entity.getDescription(),
        DateTimeFormatters.formatDateTime(entity.getCreatedAt()),
        DateTimeFormatters.formatDateTime(entity.getUpdatedAt()),
        entity.getVersionNumber(),
        null,
        entity.getPreviousVersionId() == null ? null : String.valueOf(entity.getPreviousVersionId()),
        entity.getVisibilityLevel(),
        entity.getOwnerScopeId() == null ? null : String.valueOf(entity.getOwnerScopeId()),
        splitCommaValues(entity.getSharedScopeIds()).stream()
            .map(scopeId -> new StandardDto.ScopeGrantDto(scopeId, List.of("view")))
            .toList(),
        entity.getChangeLog(),
        resolveOperatorName(entity, operatorNames)
    );
  }

  private List<StandardDto> withVersionCounts(List<StandardDto> standards) {
    Map<String, Long> counts = standards.stream()
        .collect(Collectors.groupingBy(StandardDto::standardGroupId, Collectors.counting()));
    return standards.stream()
        .map(dto -> withVersionCount(dto, counts.getOrDefault(dto.standardGroupId(), 1L).intValue()))
        .toList();
  }

  private StandardDto withVersionCount(StandardDto dto, Integer versionCount) {
    return new StandardDto(
        dto.id(),
        dto.standardGroupId(),
        dto.standardCode(),
        dto.title(),
        dto.category(),
        dto.version(),
        dto.publishDate(),
        dto.status(),
        dto.attachments(),
        dto.description(),
        dto.createdAt(),
        dto.updatedAt(),
        dto.versionNumber(),
        versionCount,
        dto.previousVersionId(),
        dto.visibilityLevel(),
        dto.ownerScopeId(),
        dto.grants(),
        dto.changeLog(),
        dto.operatorName()
    );
  }

  private String resolveOperatorName(BizStandardEntity entity, Map<Long, String> operatorNames) {
    Long operatorId = entity.getUpdatedBy() != null ? entity.getUpdatedBy() : entity.getCreatedBy();
    if (operatorId == null) {
      return null;
    }
    return operatorNames.getOrDefault(operatorId, String.valueOf(operatorId));
  }

  private String normalizeRequiredText(String value) {
    return value == null ? null : value.trim();
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
      return new StandardPermissionContext(true, principal.userId(), Map.of());
    }

    Map<String, ResourceScopeMemberDto> memberships = resourceScopeMemberMapper
        .selectByTenantIdAndUserId(principal.tenantId(), principal.userId())
        .stream()
        .collect(Collectors.toMap(ResourceScopeMemberDto::scopeId, member -> member, (left, right) -> left));

    return new StandardPermissionContext(false, principal.userId(), memberships);
  }

  private boolean canView(BizStandardEntity entity, StandardPermissionContext permissionContext) {
    if (permissionContext.superAdmin()) {
      return true;
    }
    if (isDraft(entity) && !Objects.equals(entity.getCreatedBy(), permissionContext.userId())) {
      return false;
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

  private void ensureDraftDeleteOnly(BizStandardEntity entity) {
    if (!isDraft(entity)) {
      throw new BusinessException(
          "STANDARD_DELETE_ONLY_DRAFT",
          "only draft standard can be deleted: " + entity.getId()
      );
    }
  }

  private void ensureDraftPublishOnly(BizStandardEntity entity) {
    if (!isDraft(entity)) {
      throw new BusinessException(
          "STANDARD_PUBLISH_ONLY_DRAFT",
          "only draft standard can be published: " + entity.getId()
      );
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

  private boolean isDraft(BizStandardEntity entity) {
    return "draft".equalsIgnoreCase(entity.getStatus());
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
      Long userId,
      Map<String, ResourceScopeMemberDto> memberships
  ) {
  }
}
