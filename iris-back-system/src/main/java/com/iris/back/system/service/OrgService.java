package com.iris.back.system.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysOrgMapper;
import com.iris.back.system.model.dto.OrgDto;
import com.iris.back.system.model.entity.SysOrgEntity;
import com.iris.back.system.model.request.OrgUpsertRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrgService {

  private static final long SYSTEM_USER_ID = 2001L;

  private final SysOrgMapper orgMapper;
  private final IdentifierGenerator identifierGenerator;

  public OrgService(SysOrgMapper orgMapper, IdentifierGenerator identifierGenerator) {
    this.orgMapper = orgMapper;
    this.identifierGenerator = identifierGenerator;
  }

  public List<OrgDto> list() {
    return orgMapper.selectList(null).stream()
        .sorted(Comparator.comparing(SysOrgEntity::getId))
        .map(this::toDto)
        .toList();
  }

  public OrgDto get(Long id) {
    return toDto(requireOrg(id));
  }

  public OrgDto create(OrgUpsertRequest request) {
    SysOrgEntity entity = new SysOrgEntity();
    entity.setId(nextId(entity));
    entity.setTenantId(request.tenantId());
    entity.setParentId(request.parentId());
    entity.setOrgCode(request.orgCode());
    entity.setOrgName(request.orgName());
    entity.setOrgLevel(request.orgLevel());
    entity.setSortOrder(request.sortOrder());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(SYSTEM_USER_ID);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    orgMapper.insert(entity);
    return toDto(entity);
  }

  public OrgDto update(Long id, OrgUpsertRequest request) {
    SysOrgEntity entity = requireOrg(id);
    entity.setTenantId(request.tenantId());
    entity.setParentId(request.parentId());
    entity.setOrgCode(request.orgCode());
    entity.setOrgName(request.orgName());
    entity.setOrgLevel(request.orgLevel());
    entity.setSortOrder(request.sortOrder());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
    entity.setUpdatedBy(SYSTEM_USER_ID);
    orgMapper.updateById(entity);
    return toDto(entity);
  }

  private SysOrgEntity requireOrg(Long id) {
    SysOrgEntity entity = orgMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("ORG_NOT_FOUND", "org not found: " + id);
    }
    return entity;
  }

  private long nextId(Object entity) {
    return ((Number) identifierGenerator.nextId(entity)).longValue();
  }

  private OrgDto toDto(SysOrgEntity entity) {
    return new OrgDto(
        entity.getId(),
        entity.getTenantId(),
        entity.getParentId(),
        entity.getOrgCode(),
        entity.getOrgName(),
        entity.getOrgLevel(),
        entity.getSortOrder(),
        entity.getStatus(),
        entity.getRemark()
    );
  }
}
