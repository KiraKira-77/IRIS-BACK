package com.iris.back;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.back.auth.service.AuthService;
import com.iris.back.framework.security.AuthSessionStore;
import com.iris.back.framework.web.GlobalExceptionHandler;
import com.iris.back.system.mapper.SysFileMapper;
import com.iris.back.system.mapper.SysFileRefMapper;
import com.iris.back.system.mapper.SysOrgMapper;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.mapper.SysTenantMapper;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
class FrameworkSmokeTests {

  @Autowired
  private GlobalExceptionHandler globalExceptionHandler;

  @MockBean
  private SysTenantMapper tenantMapper;

  @MockBean
  private SysOrgMapper orgMapper;

  @MockBean
  private SysUserMapper userMapper;

  @MockBean
  private SysUserRoleMapper userRoleMapper;

  @MockBean
  private SysRoleMapper roleMapper;

  @MockBean
  private AuthSessionStore authSessionStore;

  @MockBean
  private AuthService authService;

  @MockBean
  private SysFileMapper sysFileMapper;

  @MockBean
  private SysFileRefMapper sysFileRefMapper;

  @Test
  void globalExceptionHandlerBeanExists() {
    assertThat(globalExceptionHandler).isNotNull();
  }
}
