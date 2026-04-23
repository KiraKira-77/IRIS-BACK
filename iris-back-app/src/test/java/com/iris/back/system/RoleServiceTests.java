package com.iris.back.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.mapper.SysRoleMenuMapper;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.mapper.SysUserRoleMapper;
import com.iris.back.system.model.entity.SysRoleMenuEntity;
import com.iris.back.system.model.entity.SysRoleEntity;
import com.iris.back.system.model.request.RoleUpsertRequest;
import com.iris.back.system.service.RoleService;
import java.util.List;
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

  @Mock
  private SysRoleMenuMapper roleMenuMapper;

  @Mock
  private SysUserRoleMapper userRoleMapper;

  @InjectMocks
  private RoleService roleService;

  @Test
  void createAssignsGeneratedIdBeforeInsert() {
    when(identifierGenerator.nextId(any())).thenReturn(9300001L);

    var created = roleService.create(new RoleUpsertRequest(
        1001L,
        "REVIEWER",
        "Reviewer",
        "TENANT",
        1,
        "created in test",
        List.of("workbench.dashboard", "resource.standards")
    ));

    ArgumentCaptor<SysRoleEntity> captor = ArgumentCaptor.forClass(SysRoleEntity.class);
    verify(roleMapper).insert(captor.capture());
    ArgumentCaptor<List<SysRoleMenuEntity>> menuCaptor = ArgumentCaptor.forClass(List.class);
    verify(roleMenuMapper).replaceForRole(any(), menuCaptor.capture());

    assertThat(created.id()).isEqualTo("9300001");
    assertThat(captor.getValue().getId()).isEqualTo(9300001L);
    assertThat(captor.getValue().getRoleCode()).isEqualTo("REVIEWER");
    assertThat(captor.getValue().getScopeType()).isEqualTo("BUSINESS");
    assertThat(created.scopeType()).isEqualTo("BUSINESS");
    assertThat(created.menuCodes()).containsExactly("workbench.dashboard", "resource.standards");
    assertThat(menuCaptor.getValue()).extracting(SysRoleMenuEntity::getMenuCode)
        .containsExactly("workbench.dashboard", "resource.standards");
  }

  @Test
  void getNormalizesLegacyPlatformScopeType() {
    SysRoleEntity entity = new SysRoleEntity();
    entity.setId(3001L);
    entity.setTenantId(1001L);
    entity.setRoleCode("PLATFORM_ADMIN");
    entity.setRoleName("Platform Administrator");
    entity.setScopeType("PLATFORM");
    entity.setStatus(1);
    when(roleMapper.selectById(3001L)).thenReturn(entity);
    when(roleMenuMapper.selectMenuCodesByRoleId(3001L)).thenReturn(List.of("system.roles"));

    var role = roleService.get(3001L);

    assertThat(role.scopeType()).isEqualTo("GLOBAL");
  }

  @Test
  void deleteRemovesUnusedRoleAndMenus() {
    SysRoleEntity entity = new SysRoleEntity();
    entity.setId(9300001L);
    entity.setTenantId(1001L);
    entity.setRoleCode("REVIEWER");
    entity.setRoleName("Reviewer");
    when(roleMapper.selectById(9300001L)).thenReturn(entity);
    when(userRoleMapper.countActiveAssignments(9300001L)).thenReturn(0L);

    roleService.delete(9300001L);

    verify(roleMenuMapper).deleteByRoleIdHard(9300001L);
    verify(roleMapper).deleteByIdHard(9300001L);
  }

  @Test
  void deleteRejectsAssignedRole() {
    SysRoleEntity entity = new SysRoleEntity();
    entity.setId(9300001L);
    entity.setTenantId(1001L);
    entity.setRoleCode("REVIEWER");
    entity.setRoleName("Reviewer");
    when(roleMapper.selectById(9300001L)).thenReturn(entity);
    when(userRoleMapper.countActiveAssignments(9300001L)).thenReturn(2L);

    assertThatThrownBy(() -> roleService.delete(9300001L))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("ROLE_IN_USE");

    verify(roleMenuMapper, never()).deleteByRoleIdHard(9300001L);
    verify(roleMapper, never()).deleteByIdHard(9300001L);
  }

  @Test
  void deleteRejectsProtectedPlatformAdminRole() {
    SysRoleEntity entity = new SysRoleEntity();
    entity.setId(3001L);
    entity.setTenantId(1001L);
    entity.setRoleCode("PLATFORM_ADMIN");
    entity.setRoleName("Platform Administrator");
    when(roleMapper.selectById(3001L)).thenReturn(entity);

    assertThatThrownBy(() -> roleService.delete(3001L))
        .isInstanceOf(BusinessException.class)
        .extracting("code")
        .isEqualTo("ROLE_PROTECTED");

    verify(userRoleMapper, never()).countActiveAssignments(3001L);
    verify(roleMenuMapper, never()).deleteByRoleIdHard(3001L);
    verify(roleMapper, never()).deleteByIdHard(3001L);
  }
}
