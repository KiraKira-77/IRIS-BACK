package com.iris.back.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.model.entity.SysRoleEntity;
import com.iris.back.system.model.request.RoleUpsertRequest;
import com.iris.back.system.service.RoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceTests {

  @Mock
  private SysRoleMapper roleMapper;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @InjectMocks
  private RoleService roleService;

  @Test
  void createAssignsGeneratedIdBeforeInsert() {
    when(identifierGenerator.nextId(any())).thenReturn(9300001L);

    var created = roleService.create(new RoleUpsertRequest(
        1001L, "REVIEWER", "Reviewer", "TENANT", 1, "created in test"
    ));

    ArgumentCaptor<SysRoleEntity> captor = ArgumentCaptor.forClass(SysRoleEntity.class);
    verify(roleMapper).insert(captor.capture());

    assertThat(created.id()).isEqualTo(9300001L);
    assertThat(captor.getValue().getId()).isEqualTo(9300001L);
    assertThat(captor.getValue().getRoleCode()).isEqualTo("REVIEWER");
  }
}
