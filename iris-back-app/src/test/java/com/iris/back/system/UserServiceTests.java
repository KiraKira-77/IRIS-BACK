package com.iris.back.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.model.entity.SysUserEntity;
import com.iris.back.system.model.request.UserUpsertRequest;
import com.iris.back.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

  @Mock
  private SysUserMapper userMapper;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @InjectMocks
  private UserService userService;

  @Test
  void createAssignsGeneratedIdBeforeInsert() {
    when(identifierGenerator.nextId(any())).thenReturn(9100001L);

    var created = userService.create(new UserUpsertRequest(
        1001L, 1101L, "auditor", "Auditor", "auditor@iris.local", "13900000000", 1, "created in test"
    ));

    ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
    verify(userMapper).insert(captor.capture());

    assertThat(created.id()).isEqualTo(9100001L);
    assertThat(captor.getValue().getId()).isEqualTo(9100001L);
    assertThat(captor.getValue().getAccount()).isEqualTo("auditor");
  }
}
