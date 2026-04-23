package com.iris.back.system;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.mapper.SysRoleMenuMapper;
import com.iris.back.system.mapper.SysUserRoleMapper;
import com.iris.back.system.model.entity.SysResourceScopeMemberEntity;
import com.iris.back.system.model.entity.SysRoleMenuEntity;
import com.iris.back.system.model.entity.SysUserRoleEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RelationReplaceMapperTests {

  @Mock
  private SysResourceScopeMemberMapper resourceScopeMemberMapper;

  @Mock
  private SysUserRoleMapper userRoleMapper;

  @Mock
  private SysRoleMenuMapper roleMenuMapper;

  @Test
  void replaceForScopeUsesHardDeleteBeforeInsert() {
    List<SysResourceScopeMemberEntity> entities = List.of(new SysResourceScopeMemberEntity());
    doCallRealMethod().when(resourceScopeMemberMapper).replaceForScope(9101L, entities);

    resourceScopeMemberMapper.replaceForScope(9101L, entities);

    verify(resourceScopeMemberMapper).deleteByScopeIdHard(9101L);
    verify(resourceScopeMemberMapper, times(1)).insert(entities.getFirst());
  }

  @Test
  void replaceForUserUsesHardDeleteBeforeInsert() {
    List<SysUserRoleEntity> entities = List.of(new SysUserRoleEntity());
    doCallRealMethod().when(userRoleMapper).replaceForUser(2001L, entities);

    userRoleMapper.replaceForUser(2001L, entities);

    verify(userRoleMapper).deleteByUserIdHard(2001L);
    verify(userRoleMapper, times(1)).insert(entities.getFirst());
  }

  @Test
  void replaceForRoleUsesHardDeleteBeforeInsert() {
    List<SysRoleMenuEntity> entities = List.of(new SysRoleMenuEntity());
    doCallRealMethod().when(roleMenuMapper).replaceForRole(3001L, entities);

    roleMenuMapper.replaceForRole(3001L, entities);

    verify(roleMenuMapper).deleteByRoleIdHard(3001L);
    verify(roleMenuMapper, times(1)).insert(entities.getFirst());
  }
}
