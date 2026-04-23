package com.iris.back.system.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysResourceScopeUsageMapper {

  @Select("""
      SELECT COUNT(1)
      FROM biz_standard
      WHERE owner_scope_id = #{scopeId}
        AND deleted = 0
      """)
  long countOwnerReferences(@Param("scopeId") Long scopeId);

  @Select("""
      SELECT COUNT(1)
      FROM biz_standard
      WHERE FIND_IN_SET(CAST(#{scopeId} AS CHAR), shared_scope_ids) > 0
        AND deleted = 0
      """)
  long countSharedReferences(@Param("scopeId") Long scopeId);
}
