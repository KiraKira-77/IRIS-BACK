package com.iris.back.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysTenantMapper;
import com.iris.back.system.model.entity.SysTenantEntity;
import com.iris.back.system.model.request.TenantUpsertRequest;
import com.iris.back.system.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantServiceTests {

  @Mock
  private SysTenantMapper tenantMapper;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @InjectMocks
  private TenantService tenantService;

  @Test
  void createAssignsSnowflakeIdAndPersistsTenant() {
    when(identifierGenerator.nextId(any())).thenReturn(9001001L);

    var created = tenantService.create(new TenantUpsertRequest("ACME", "Acme Tenant", 1, "created in test"));

    ArgumentCaptor<SysTenantEntity> captor = ArgumentCaptor.forClass(SysTenantEntity.class);
    verify(tenantMapper).insert(captor.capture());
    SysTenantEntity entity = captor.getValue();

    assertThat(created.id()).isEqualTo(9001001L);
    assertThat(created.tenantId()).isEqualTo(9001001L);
    assertThat(entity.getId()).isEqualTo(9001001L);
    assertThat(entity.getTenantId()).isEqualTo(9001001L);
    assertThat(entity.getTenantCode()).isEqualTo("ACME");
  }

  @Test
  void listMapsEntitiesFromDatabase() {
    SysTenantEntity entity = new SysTenantEntity();
    entity.setId(1001L);
    entity.setTenantId(1001L);
    entity.setTenantCode("DEFAULT");
    entity.setTenantName("Default Tenant");
    entity.setStatus(1);
    when(tenantMapper.selectList(null)).thenReturn(List.of(entity));

    var result = tenantService.list();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().tenantCode()).isEqualTo("DEFAULT");
  }

  @Test
  void updateFailsWhenTenantDoesNotExist() {
    when(tenantMapper.selectById(999L)).thenReturn(null);

    assertThatThrownBy(() -> tenantService.update(999L, new TenantUpsertRequest("X", "X", 1, null)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("tenant not found");
  }
}
