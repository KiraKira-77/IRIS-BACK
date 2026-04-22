package com.iris.back.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iris.back.system.model.entity.SysUserEntity;
import com.iris.back.system.model.dto.AuthUserView;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SysUserMapper extends BaseMapper<SysUserEntity> {

  @Select("""
      SELECT
        u.id AS user_id,
        u.tenant_id AS tenant_id,
        u.account AS account,
        u.username AS username,
        u.password_hash AS password_hash,
        t.tenant_name AS tenant_name
      FROM sys_user u
      INNER JOIN sys_tenant t ON t.tenant_id = u.tenant_id AND t.deleted = 0
      WHERE u.account = #{account}
        AND u.status = 1
        AND u.deleted = 0
      LIMIT 1
      """)
  AuthUserView selectAuthUserByAccount(@Param("account") String account);

  @Select("""
      SELECT r.role_code
      FROM sys_user_role ur
      INNER JOIN sys_role r ON r.id = ur.role_id
      WHERE ur.tenant_id = #{tenantId}
        AND ur.user_id = #{userId}
        AND ur.deleted = 0
        AND r.deleted = 0
        AND r.status = 1
      ORDER BY r.id
      """)
  List<String> selectRoleCodesByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
