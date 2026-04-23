package com.iris.back.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iris.back.system.model.entity.SysRoleMenuEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenuEntity> {

  @Select("""
      SELECT menu_code
      FROM sys_role_menu
      WHERE role_id = #{roleId}
        AND deleted = 0
      ORDER BY id
      """)
  List<String> selectMenuCodesByRoleId(@Param("roleId") Long roleId);

  @Delete("""
      DELETE FROM sys_role_menu
      WHERE role_id = #{roleId}
      """)
  int deleteByRoleIdHard(@Param("roleId") Long roleId);

  default void replaceForRole(Long roleId, List<SysRoleMenuEntity> entities) {
    deleteByRoleIdHard(roleId);
    entities.forEach(this::insert);
  }
}
