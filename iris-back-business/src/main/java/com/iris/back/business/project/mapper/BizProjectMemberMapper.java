package com.iris.back.business.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iris.back.business.project.model.entity.BizProjectMemberEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface BizProjectMemberMapper extends BaseMapper<BizProjectMemberEntity> {
  @Delete("DELETE FROM biz_project_member WHERE tenant_id = #{tenantId} AND project_id = #{projectId}")
  int hardDeleteByProject(@Param("tenantId") Long tenantId, @Param("projectId") Long projectId);
}
