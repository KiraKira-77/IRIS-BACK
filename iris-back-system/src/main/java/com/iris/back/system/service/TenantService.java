package com.iris.back.system.service;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysTenantMapper;
import com.iris.back.system.model.dto.TenantDto;
import com.iris.back.system.model.entity.SysTenantEntity;
import com.iris.back.system.model.request.TenantUpsertRequest;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

  private static final long SYSTEM_USER_ID = 2001L;

  private final SysTenantMapper tenantMapper;
  private final IdentifierGenerator identifierGenerator;

  public TenantService(SysTenantMapper tenantMapper, IdentifierGenerator identifierGenerator) {
    this.tenantMapper = tenantMapper;
    this.identifierGenerator = identifierGenerator;
  }

  public List<TenantDto> list() {
    return tenantMapper.selectList(null).stream()
        .sorted(Comparator.comparing(SysTenantEntity::getId))
        .map(this::toDto)
        .toList();
  }

  public TenantDto get(Long id) {
    return toDto(requireTenant(id));
  }

  public TenantDto create(TenantUpsertRequest request) {
    SysTenantEntity entity = new SysTenantEntity();
    long generatedId = nextId(entity);
    entity.setId(generatedId);
    entity.setTenantId(generatedId);
    entity.setTenantCode(request.tenantCode());
    entity.setTenantName(request.tenantName());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
    entity.setDeleted(0);
    entity.setVersion(0L);
    entity.setCreatedBy(SYSTEM_USER_ID);
    entity.setUpdatedBy(SYSTEM_USER_ID);
    tenantMapper.insert(entity);
    return toDto(entity);
  }

  public TenantDto update(Long id, TenantUpsertRequest request) {
    SysTenantEntity entity = requireTenant(id);
    entity.setTenantCode(request.tenantCode());
    entity.setTenantName(request.tenantName());
    entity.setStatus(request.status());
    entity.setRemark(request.remark());
    entity.setUpdatedBy(SYSTEM_USER_ID);
    tenantMapper.updateById(entity);
    return toDto(entity);
  }

  private SysTenantEntity requireTenant(Long id) {
    SysTenantEntity entity = tenantMapper.selectById(id);
    if (entity == null) {
      throw new BusinessException("TENANT_NOT_FOUND", "tenant not found: " + id);
    }
    return entity;
  }

  private long nextId(Object entity) {
    return ((Number) identifierGenerator.nextId(entity)).longValue();
  }

  private TenantDto toDto(SysTenantEntity entity) {
    return new TenantDto(
        entity.getId(),
        entity.getTenantId(),
        entity.getTenantCode(),
        entity.getTenantName(),
        entity.getStatus(),
        entity.getRemark()
    );
  }
}
