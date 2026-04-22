package com.iris.back.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.system.mapper.SysOrgMapper;
import com.iris.back.system.model.entity.SysOrgEntity;
import com.iris.back.system.model.request.OrgUpsertRequest;
import com.iris.back.system.service.OrgService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrgServiceTests {

  @Mock
  private SysOrgMapper orgMapper;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @InjectMocks
  private OrgService orgService;

  @Test
  void createAssignsGeneratedIdBeforeInsert() {
    when(identifierGenerator.nextId(any())).thenReturn(9200001L);

    var created = orgService.create(new OrgUpsertRequest(
        1001L, 1101L, "FIN", "Finance Department", 2, 10, 1, "created in test"
    ));

    ArgumentCaptor<SysOrgEntity> captor = ArgumentCaptor.forClass(SysOrgEntity.class);
    verify(orgMapper).insert(captor.capture());

    assertThat(created.id()).isEqualTo(9200001L);
    assertThat(captor.getValue().getId()).isEqualTo(9200001L);
    assertThat(captor.getValue().getOrgCode()).isEqualTo("FIN");
  }
}
