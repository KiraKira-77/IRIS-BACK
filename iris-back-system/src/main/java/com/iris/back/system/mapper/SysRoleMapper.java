package com.iris.back.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iris.back.system.model.entity.SysRoleEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysRoleMapper extends BaseMapper<SysRoleEntity> {

  @Delete("""
      DELETE FROM sys_role
      WHERE id = #{id}
      """)
  int deleteByIdHard(@Param("id") Long id);
}
