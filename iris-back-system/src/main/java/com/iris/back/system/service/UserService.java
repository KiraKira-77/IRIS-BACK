package com.iris.back.system.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.model.dto.UserDto;
import com.iris.back.system.model.entity.SysUserEntity;
import com.iris.back.system.model.request.UserUpsertRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private static final long SYSTEM_USER_ID = 2001L;

  private final SysUserMapper userMapper;
  private final IdentifierGenerator identifierGenerator;

  public UserService(SysUserMapper userMapper, IdentifierGenerator identifierGenerator) {
    this.userMapper = userMapper;
    this.identifierGenerator = identifierGenerator;
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
    entity.setPasswordHash("$2a$10$7EqJtq98hPqEX7fNZaFWoO5a5y9N8MaqkOawx2UY8ailqXF/w3v9e");
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(SYSTEM_USER_ID);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    userMapper.insert(entity);
    return toDto(entity);
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
    return toDto(entity);
  }

  private SysUserEntity requireUser(Long id) {
    SysUserEntity entity = userMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("USER_NOT_FOUND", "user not found: " + id);
    }
    return entity;
  }

  private long nextId(Object entity) {
    return ((Number) identifierGenerator.nextId(entity)).longValue();
  }

  private UserDto toDto(SysUserEntity entity) {
    return new UserDto(
        entity.getId(),
        entity.getTenantId(),
        entity.getOrgId(),
        entity.getAccount(),
        entity.getUsername(),
        entity.getEmail(),
        entity.getMobile(),
        entity.getStatus(),
        entity.getRemark()
    );
  }
}
