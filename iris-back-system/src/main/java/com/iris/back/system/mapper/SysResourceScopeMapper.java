package com.iris.back.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iris.back.system.model.entity.SysResourceScopeEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface SysResourceScopeMapper extends BaseMapper<SysResourceScopeEntity> {

  @Delete("""
      DELETE FROM sys_resource_scope
      WHERE id = #{id}
      """)
  int deleteByIdHard(@Param("id") Long id);
}
