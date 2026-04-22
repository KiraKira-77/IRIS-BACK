package com.iris.back.system.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.model.dto.RoleDto;
import com.iris.back.system.model.entity.SysRoleEntity;
import com.iris.back.system.model.request.RoleUpsertRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

  private static final long SYSTEM_USER_ID = 2001L;

  private final SysRoleMapper roleMapper;
  private final IdentifierGenerator identifierGenerator;

  public RoleService(SysRoleMapper roleMapper, IdentifierGenerator identifierGenerator) {
    this.roleMapper = roleMapper;
    this.identifierGenerator = identifierGenerator;
  }

  public List<RoleDto> list() {
    return roleMapper.selectList(null).stream()
        .sorted(Comparator.comparing(SysRoleEntity::getId))
        .map(this::toDto)
        .toList();
  }

  public RoleDto get(Long id) {
    return toDto(requireRole(id));
  }

  public RoleDto create(RoleUpsertRequest request) {
    SysRoleEntity entity = new SysRoleEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(request.tenantId());
    entity.setRoleCode(request.roleCode());
    entity.setRoleName(request.roleName());
    entity.setScopeType(request.scopeType());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(SYSTEM_USER_ID);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    roleMapper.insert(entity);
    return toDto(entity);
  }

  public RoleDto update(Long id, RoleUpsertRequest request) {
    SysRoleEntity entity = requireRole(id);
    entity.setTenantId(request.tenantId());
    entity.setRoleCode(request.roleCode());
    entity.setRoleName(request.roleName());
    entity.setScopeType(request.scopeType());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
    entity.setUpdatedBy(SYSTEM_USER_ID);
    roleMapper.updateById(entity);
    return toDto(entity);
  }

  private SysRoleEntity requireRole(Long id) {
    SysRoleEntity entity = roleMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("ROLE_NOT_FOUND", "role not found: " + id);
    }
    return entity;
  }

  private long nextId(Object entity) {
    return ((Number) identifierGenerator.nextId(entity)).longValue();
  }

  private RoleDto toDto(SysRoleEntity entity) {
    return new RoleDto(
        entity.getId(),
        entity.getTenantId(),
        entity.getRoleCode(),
        entity.getRoleName(),
        entity.getScopeType(),
        entity.getStatus(),
        entity.getRemark()
    );
  }
}
