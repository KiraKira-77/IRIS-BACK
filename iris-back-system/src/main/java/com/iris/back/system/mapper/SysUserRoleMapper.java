package com.iris.back.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iris.back.system.model.entity.SysUserRoleEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysUserRoleMapper extends BaseMapper<SysUserRoleEntity> {

  @Select("""
      SELECT COUNT(1)
      FROM sys_user_role
      WHERE role_id = #{roleId}
        AND deleted = 0
      """)
  long countActiveAssignments(@Param("roleId") Long roleId);

  @Delete("""
      DELETE FROM sys_user_role
      WHERE user_id = #{userId}
      """)
  int deleteByUserIdHard(@Param("userId") Long userId);

  default void replaceForUser(Long userId, List<SysUserRoleEntity> entities) {
    deleteByUserIdHard(userId);
    entities.forEach(this::insert);
  }
}
