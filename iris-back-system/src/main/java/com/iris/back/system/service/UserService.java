package com.iris.back.system.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.mapper.SysUserRoleMapper;
import com.iris.back.system.model.entity.SysRoleEntity;
import com.iris.back.system.model.dto.UserDto;
import com.iris.back.system.model.entity.SysUserEntity;
import com.iris.back.system.model.entity.SysUserRoleEntity;
import com.iris.back.system.model.request.UserUpsertRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {

  private static final long SYSTEM_USER_ID = 2001L;
  private static final String DEFAULT_PASSWORD = "jolywood";
  private static final String BOOTSTRAP_ADMIN_ACCOUNT = "admin";

  private final SysUserMapper userMapper;
  private final SysRoleMapper roleMapper;
  private final SysUserRoleMapper userRoleMapper;
  private final IdentifierGenerator identifierGenerator;
  private final PasswordEncoder passwordEncoder;

  public UserService(
      SysUserMapper userMapper,
      SysRoleMapper roleMapper,
      SysUserRoleMapper userRoleMapper,
      IdentifierGenerator identifierGenerator,
      PasswordEncoder passwordEncoder
  ) {
    this.userMapper = userMapper;
    this.roleMapper = roleMapper;
    this.userRoleMapper = userRoleMapper;
    this.identifierGenerator = identifierGenerator;
    this.passwordEncoder = passwordEncoder;
  }

  public List<UserDto> list() {
    return userMapper.selectList(null).stream()
        .sorted(Comparator.comparing(SysUserEntity::getId))
        .map(this::toDto)
        .toList();
  }

  public UserDto get(Long id) {
    return toDto(requireUser(id));
  }

  public UserDto create(UserUpsertRequest request) {
    SysUserEntity entity = new SysUserEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(request.tenantId());
    entity.setOrgId(request.orgId());
    entity.setAccount(request.account());
    entity.setUsername(request.username());
    entity.setEmail(request.email());
    entity.setMobile(request.mobile());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
    entity.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(SYSTEM_USER_ID);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    userMapper.insert(entity);
    List<Long> roleIds = normalizeRoleIds(request.roleIds(), request.tenantId());
    replaceUserRoles(entity, roleIds);
    return toDto(entity, roleIds);
  }

  public UserDto update(Long id, UserUpsertRequest request) {
    SysUserEntity entity = requireUser(id);
    entity.setTenantId(request.tenantId());
    entity.setOrgId(request.orgId());
    entity.setAccount(request.account());
    entity.setUsername(request.username());
    entity.setEmail(request.email());
    entity.setMobile(request.mobile());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
    entity.setUpdatedBy(SYSTEM_USER_ID);
    userMapper.updateById(entity);
    List<Long> roleIds = normalizeRoleIds(request.roleIds(), request.tenantId());
    replaceUserRoles(entity, roleIds);
    return toDto(entity, roleIds);
  }

  public void resetPassword(Long id) {
    SysUserEntity entity = requireUser(id);
    entity.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
    entity.setUpdatedBy(SYSTEM_USER_ID);
    userMapper.updateById(entity);
  }

  public void delete(Long id) {
    requireUserDeletable(id);
    userRoleMapper.deleteByUserIdHard(id);
    userMapper.deleteByIdHard(id);
  }

  private SysUserEntity requireUser(Long id) {
    SysUserEntity entity = userMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("USER_NOT_FOUND", "user not found: " + id);
    }
    return entity;
  }

  private void requireUserDeletable(Long id) {
    SysUserEntity entity = requireUser(id);
    if (BOOTSTRAP_ADMIN_ACCOUNT.equalsIgnoreCase(entity.getAccount())) {
      throw new BusinessException("USER_PROTECTED", "bootstrap admin user cannot be deleted: " + id);
    }
  }

  private long nextId(Object entity) {
    return ((Number) identifierGenerator.nextId(entity)).longValue();
  }

  private UserDto toDto(SysUserEntity entity) {
    List<Long> roleIds = userMapper.selectRoleIdsByUserId(entity.getTenantId(), entity.getId());
    return toDto(entity, roleIds);
  }

  private UserDto toDto(SysUserEntity entity, List<Long> roleIds) {
    return new UserDto(
        stringify(entity.getId()),
        stringify(entity.getTenantId()),
        stringify(entity.getOrgId()),
        entity.getAccount(),
        entity.getUsername(),
        entity.getEmail(),
        entity.getMobile(),
        entity.getStatus(),
        entity.getRemark(),
        roleIds.stream().map(this::stringify).toList(),
        userMapper.selectRoleCodesByUserId(entity.getTenantId(), entity.getId())
    );
  }

  private void replaceUserRoles(SysUserEntity user, List<Long> roleIds) {
    List<SysUserRoleEntity> entities = roleIds.stream()
        .map(roleId -> toUserRoleEntity(user, roleId))
        .toList();
    userRoleMapper.replaceForUser(user.getId(), entities);
  }

  private List<Long> normalizeRoleIds(List<Long> roleIds, Long tenantId) {
    return (roleIds == null ? List.<Long>of() : roleIds).stream()
        .filter(Objects::nonNull)
        .distinct()
        .map(roleId -> requireRole(roleId, tenantId).getId())
        .toList();
  }

  private SysRoleEntity requireRole(Long roleId, Long tenantId) {
    SysRoleEntity entity = roleMapper.selectById(roleId);
    if (entity == null) {
      throw new BusinessException("ROLE_NOT_FOUND", "role not found: " + roleId);
    }
    if (!Objects.equals(entity.getTenantId(), tenantId)) {
      throw new BusinessException("ROLE_TENANT_MISMATCH", "role tenant mismatch: " + roleId);
    }
    return entity;
  }

  private SysUserRoleEntity toUserRoleEntity(SysUserEntity user, Long roleId) {
    SysUserRoleEntity entity = new SysUserRoleEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(user.getTenantId());
    entity.setUserId(user.getId());
    entity.setRoleId(roleId);
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
