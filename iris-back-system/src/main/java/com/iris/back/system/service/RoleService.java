package com.iris.back.system.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysRoleMenuMapper;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.mapper.SysUserRoleMapper;
import com.iris.back.system.model.entity.SysRoleMenuEntity;
import com.iris.back.system.model.dto.RoleDto;
import com.iris.back.system.model.entity.SysRoleEntity;
import com.iris.back.system.model.request.RoleUpsertRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

  private static final long SYSTEM_USER_ID = 2001L;
  private static final String PLATFORM_ADMIN_ROLE_CODE = "PLATFORM_ADMIN";
  private static final String SUPER_ADMIN_ROLE_CODE = "SUPER_ADMIN";

  private final SysRoleMapper roleMapper;
  private final SysRoleMenuMapper roleMenuMapper;
  private final SysUserRoleMapper userRoleMapper;
  private final IdentifierGenerator identifierGenerator;

  public RoleService(
      SysRoleMapper roleMapper,
      SysRoleMenuMapper roleMenuMapper,
      SysUserRoleMapper userRoleMapper,
      IdentifierGenerator identifierGenerator
  ) {
    this.roleMapper = roleMapper;
    this.roleMenuMapper = roleMenuMapper;
    this.userRoleMapper = userRoleMapper;
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
    List<String> menuCodes = normalizeMenuCodes(request.menuCodes());
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
    replaceRoleMenus(entity, menuCodes);
    return toDto(entity, menuCodes);
  }

  public RoleDto update(Long id, RoleUpsertRequest request) {
    List<String> menuCodes = normalizeMenuCodes(request.menuCodes());
    SysRoleEntity entity = requireRole(id);
    entity.setTenantId(request.tenantId());
    entity.setRoleCode(request.roleCode());
    entity.setRoleName(request.roleName());
    entity.setScopeType(request.scopeType());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
    entity.setUpdatedBy(SYSTEM_USER_ID);
    roleMapper.updateById(entity);
    replaceRoleMenus(entity, menuCodes);
    return toDto(entity, menuCodes);
  }

  public void delete(Long id) {
    requireRoleDeletable(id);
    roleMenuMapper.deleteByRoleIdHard(id);
    roleMapper.deleteByIdHard(id);
  }

  private SysRoleEntity requireRole(Long id) {
    SysRoleEntity entity = roleMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("ROLE_NOT_FOUND", "role not found: " + id);
    }
    return entity;
  }

  private void requireRoleDeletable(Long id) {
    SysRoleEntity entity = requireRole(id);
    if (PLATFORM_ADMIN_ROLE_CODE.equalsIgnoreCase(entity.getRoleCode())
        || SUPER_ADMIN_ROLE_CODE.equalsIgnoreCase(entity.getRoleCode())) {
      throw new BusinessException("ROLE_PROTECTED", "protected role cannot be deleted: " + id);
    }
    if (userRoleMapper.countActiveAssignments(id) > 0) {
      throw new BusinessException("ROLE_IN_USE", "role is still assigned to users: " + id);
    }
  }

  private long nextId(Object entity) {
    return ((Number) identifierGenerator.nextId(entity)).longValue();
  }

  private RoleDto toDto(SysRoleEntity entity) {
    return toDto(entity, roleMenuMapper.selectMenuCodesByRoleId(entity.getId()));
  }

  private RoleDto toDto(SysRoleEntity entity, List<String> menuCodes) {
    return new RoleDto(
        stringify(entity.getId()),
        stringify(entity.getTenantId()),
        entity.getRoleCode(),
        entity.getRoleName(),
        entity.getScopeType(),
        entity.getStatus(),
        entity.getRemark(),
        menuCodes
    );
  }

  private void replaceRoleMenus(SysRoleEntity role, List<String> menuCodes) {
    List<SysRoleMenuEntity> entities = normalizeMenuCodes(menuCodes).stream()
        .map(menuCode -> toRoleMenuEntity(role, menuCode))
        .toList();
    roleMenuMapper.replaceForRole(role.getId(), entities);
  }

  private List<String> normalizeMenuCodes(List<String> menuCodes) {
    return (menuCodes == null ? List.<String>of() : menuCodes).stream()
        .distinct()
        .toList();
  }

  private SysRoleMenuEntity toRoleMenuEntity(SysRoleEntity role, String menuCode) {
    SysRoleMenuEntity entity = new SysRoleMenuEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(role.getTenantId());
    entity.setRoleId(role.getId());
    entity.setMenuCode(menuCode);
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(SYSTEM_USER_ID);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    return entity;
  }

  private String stringify(Long value) {
    return value == null ? null : String.valueOf(value);
  }
}
