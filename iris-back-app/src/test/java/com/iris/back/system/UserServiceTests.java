package com.iris.back.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.mapper.SysUserRoleMapper;
import com.iris.back.system.model.entity.SysUserRoleEntity;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.model.entity.SysRoleEntity;
import com.iris.back.system.model.entity.SysUserEntity;
import com.iris.back.system.model.request.UserUpsertRequest;
import com.iris.back.system.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

  @Mock
  private SysUserMapper userMapper;

  @Mock
  private IdentifierGenerator identifierGenerator;

  @Mock
  private SysUserRoleMapper userRoleMapper;

  @Mock
  private SysRoleMapper roleMapper;

  @Spy
  private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @InjectMocks
  private UserService userService;

  @Test
  void createAssignsGeneratedIdBeforeInsert() {
    when(identifierGenerator.nextId(any())).thenReturn(9100001L, 9500001L, 9500002L);
    when(roleMapper.selectById(3002L)).thenReturn(role(3002L));
    when(roleMapper.selectById(3003L)).thenReturn(role(3003L));
    when(userMapper.selectRoleCodesByUserId(1001L, 9100001L)).thenReturn(List.of("TENANT_ADMIN", "AUDITOR"));

    var created = userService.create(new UserUpsertRequest(
        1001L,
        1101L,
        "auditor",
        "Auditor",
        "auditor@iris.local",
        "13900000000",
        1,
        "created in test",
        List.of(3002L, 3003L)
    ));

    ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
    verify(userMapper).insert(captor.capture());
    ArgumentCaptor<List<SysUserRoleEntity>> userRoleCaptor = ArgumentCaptor.forClass(List.class);
    verify(userRoleMapper).replaceForUser(any(), userRoleCaptor.capture());

    assertThat(created.id()).isEqualTo("9100001");
    assertThat(captor.getValue().getId()).isEqualTo(9100001L);
    assertThat(captor.getValue().getAccount()).isEqualTo("auditor");
    assertThat(created.roleIds()).containsExactly("3002", "3003");
    assertThat(userRoleCaptor.getValue()).extracting(SysUserRoleEntity::getRoleId).containsExactly(3002L, 3003L);
    assertThat(new BCryptPasswordEncoder().matches("jolywood", captor.getValue().getPasswordHash())).isTrue();
  }

  @Test
  void resetPasswordRewritesHashToDefaultPassword() {
    SysUserEntity entity = new SysUserEntity();
    entity.setId(9100001L);
    entity.setTenantId(1001L);
    entity.setAccount("auditor");
    entity.setUsername("Auditor");
    entity.setPasswordHash(passwordEncoder.encode("old-password"));
    when(userMapper.selectById(9100001L)).thenReturn(entity);

    userService.resetPassword(9100001L);

    ArgumentCaptor<SysUserEntity> captor = ArgumentCaptor.forClass(SysUserEntity.class);
    verify(userMapper).updateById(captor.capture());
    assertThat(new BCryptPasswordEncoder().matches("jolywood", captor.getValue().getPasswordHash())).isTrue();
  }

  @Test
  void deleteRemovesUserAndRoleRelations() {
    SysUserEntity entity = new SysUserEntity();
    entity.setId(9100001L);
    entity.setTenantId(1001L);
    entity.setAccount("auditor");
    entity.setUsername("Auditor");
    when(userMapper.selectById(9100001L)).thenReturn(entity);

    userService.delete(9100001L);

    verify(userRoleMapper).deleteByUserIdHard(9100001L);
    verify(userMapper).deleteByIdHard(9100001L);
  }

  @Test
  void deleteRejectsBootstrapAdmin() {
    SysUserEntity entity = new SysUserEntity();
    entity.setId(2001L);
    entity.setTenantId(1001L);
    entity.setAccount("admin");
    entity.setUsername("Platform Administrator");
    when(userMapper.selectById(2001L)).thenReturn(entity);

    assertThatThrownBy(() -> userService.delete(2001L))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("USER_PROTECTED");

    verify(userRoleMapper, never()).deleteByUserIdHard(2001L);
    verify(userMapper, never()).deleteByIdHard(2001L);
  }

  private SysRoleEntity role(Long id) {
    SysRoleEntity entity = new SysRoleEntity();
    entity.setId(id);
    entity.setTenantId(1001L);
    return entity;
  }
}
