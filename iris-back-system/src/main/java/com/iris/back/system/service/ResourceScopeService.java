package com.iris.back.system.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysResourceScopeMapper;
import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.mapper.SysResourceScopeUsageMapper;
import com.iris.back.system.model.dto.ResourceScopeDto;
import com.iris.back.system.model.dto.ResourceScopeMemberDto;
import com.iris.back.system.model.entity.SysResourceScopeEntity;
import com.iris.back.system.model.entity.SysResourceScopeMemberEntity;
import com.iris.back.system.model.request.ResourceScopeMemberReplaceRequest;
import com.iris.back.system.model.request.ResourceScopeMemberUpsertRequest;
import com.iris.back.system.model.request.ResourceScopeUpsertRequest;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResourceScopeService {

  private static final long SYSTEM_USER_ID = 2001L;

  private final SysResourceScopeMapper resourceScopeMapper;
  private final SysResourceScopeMemberMapper resourceScopeMemberMapper;
  private final SysResourceScopeUsageMapper resourceScopeUsageMapper;
  private final IdentifierGenerator identifierGenerator;

  public ResourceScopeService(
      SysResourceScopeMapper resourceScopeMapper,
      SysResourceScopeMemberMapper resourceScopeMemberMapper,
      SysResourceScopeUsageMapper resourceScopeUsageMapper,
      IdentifierGenerator identifierGenerator
  ) {
    this.resourceScopeMapper = resourceScopeMapper;
    this.resourceScopeMemberMapper = resourceScopeMemberMapper;
    this.resourceScopeUsageMapper = resourceScopeUsageMapper;
    this.identifierGenerator = identifierGenerator;
  }

  public List<ResourceScopeDto> list() {
    return resourceScopeMapper.selectList(null).stream()
        .sorted(Comparator.comparing(SysResourceScopeEntity::getId))
        .map(this::toDto)
        .toList();
  }

  public ResourceScopeDto get(Long id) {
    return toDto(requireScope(id));
  }

  public ResourceScopeDto create(ResourceScopeUpsertRequest request) {
    SysResourceScopeEntity entity = new SysResourceScopeEntity();
    entity.setId(nextId(entity));
    applyScopeFields(entity, request);
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(SYSTEM_USER_ID);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    resourceScopeMapper.insert(entity);
    return toDto(entity);
  }

  public ResourceScopeDto update(Long id, ResourceScopeUpsertRequest request) {
    SysResourceScopeEntity entity = requireScope(id);
    applyScopeFields(entity, request);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    resourceScopeMapper.updateById(entity);
    return toDto(entity);
  }

  public List<ResourceScopeMemberDto> listMembers(Long scopeId) {
    requireScope(scopeId);
    return resourceScopeMemberMapper.selectMembersByScopeId(scopeId);
  }

  public void replaceMembers(Long scopeId, ResourceScopeMemberReplaceRequest request) {
    SysResourceScopeEntity scope = requireScope(scopeId);
    List<SysResourceScopeMemberEntity> members = normalizeMembers(request.members()).stream()
        .map(member -> toMemberEntity(scope, member))
        .toList();
    resourceScopeMemberMapper.replaceForScope(scopeId, members);
  }

  public void delete(Long id) {
    requireScopeDeletable(id);
    resourceScopeMemberMapper.deleteByScopeIdHard(id);
    resourceScopeMapper.deleteByIdHard(id);
  }

  private List<ResourceScopeMemberUpsertRequest> normalizeMembers(List<ResourceScopeMemberUpsertRequest> members) {
    LinkedHashMap<Long, ResourceScopeMemberUpsertRequest> deduplicated = new LinkedHashMap<>();
    for (ResourceScopeMemberUpsertRequest member : members) {
      deduplicated.put(member.userId(), member);
    }
    return List.copyOf(deduplicated.values());
  }

  private void applyScopeFields(SysResourceScopeEntity entity, ResourceScopeUpsertRequest request) {
    entity.setTenantId(request.tenantId());
    entity.setScopeCode(request.scopeCode());
    entity.setScopeName(request.scopeName());
    entity.setScopeType(request.scopeType());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
  }

  private SysResourceScopeEntity requireScope(Long id) {
    SysResourceScopeEntity entity = resourceScopeMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("RESOURCE_SCOPE_NOT_FOUND", "resource scope not found: " + id);
    }
    return entity;
  }

  private void requireScopeDeletable(Long id) {
    requireScope(id);
    long ownerReferences = resourceScopeUsageMapper.countOwnerReferences(id);
    long sharedReferences = resourceScopeUsageMapper.countSharedReferences(id);
    if (ownerReferences > 0 || sharedReferences > 0) {
      throw new BusinessException("RESOURCE_SCOPE_IN_USE", "resource scope is still referenced: " + id);
    }
  }

  private SysResourceScopeMemberEntity toMemberEntity(
      SysResourceScopeEntity scope,
      ResourceScopeMemberUpsertRequest request
  ) {
    SysResourceScopeMemberEntity entity = new SysResourceScopeMemberEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(scope.getTenantId());
    entity.setScopeId(scope.getId());
    entity.setUserId(request.userId());
    entity.setCanView(flag(request.canView()));
    entity.setCanCreate(flag(request.canCreate()));
    entity.setCanEdit(flag(request.canEdit()));
    entity.setCanDelete(flag(request.canDelete()));
    entity.setCanManage(flag(request.canManage()));
    entity.setRemark(request.remark());
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(SYSTEM_USER_ID);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    return entity;
  }

  private int flag(Boolean value) {
    return Boolean.TRUE.equals(value) ? 1 : 0;
  }

  private long nextId(Object entity) {
    return ((Number) identifierGenerator.nextId(entity)).longValue();
  }

  private ResourceScopeDto toDto(SysResourceScopeEntity entity) {
    return new ResourceScopeDto(
        stringify(entity.getId()),
        stringify(entity.getTenantId()),
        entity.getScopeCode(),
        entity.getScopeName(),
        entity.getScopeType(),
        entity.getStatus(),
        entity.getRemark()
    );
  }

  private String stringify(Long value) {
    return value == null ? null : String.valueOf(value);
  }
}
