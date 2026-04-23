package com.iris.back.system.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iris.back.system.model.dto.ResourceScopeMemberDto;
import com.iris.back.system.model.entity.SysResourceScopeMemberEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysResourceScopeMemberMapper extends BaseMapper<SysResourceScopeMemberEntity> {

  @Select("""
      SELECT
        m.id AS id,
        m.scope_id AS scopeId,
        m.user_id AS userId,
        u.account AS account,
        u.username AS username,
        m.can_view AS canView,
        m.can_create AS canCreate,
        m.can_edit AS canEdit,
        m.can_delete AS canDelete,
        m.can_manage AS canManage,
        m.remark AS remark
      FROM sys_resource_scope_member m
      INNER JOIN sys_user u ON u.id = m.user_id AND u.deleted = 0
      WHERE m.scope_id = #{scopeId}
        AND m.deleted = 0
      ORDER BY m.id
      """)
  List<ResourceScopeMemberDto> selectMembersByScopeId(@Param("scopeId") Long scopeId);

  default void replaceForScope(Long scopeId, List<SysResourceScopeMemberEntity> entities) {
    delete(new LambdaQueryWrapper<SysResourceScopeMemberEntity>()
        .eq(SysResourceScopeMemberEntity::getScopeId, scopeId));
    entities.forEach(this::insert);
  }
}
