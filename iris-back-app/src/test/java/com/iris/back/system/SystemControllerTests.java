package com.iris.back.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iris.back.auth.service.AuthService;
import com.iris.back.framework.security.AuthSessionStore;
import com.iris.back.system.mapper.SysOrgMapper;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.mapper.SysTenantMapper;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.model.entity.SysTenantEntity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@AutoConfigureMockMvc
class SystemControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private SysTenantMapper tenantMapper;

  @MockBean
  private SysOrgMapper orgMapper;

  @MockBean
  private SysUserMapper userMapper;

  @MockBean
  private SysRoleMapper roleMapper;

  @MockBean
  private AuthSessionStore authSessionStore;

  @MockBean
  private AuthService authService;

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void listTenantsReturnsSeedTenant() throws Exception {
    SysTenantEntity entity = new SysTenantEntity();
    entity.setId(1001L);
    entity.setTenantId(1001L);
    entity.setTenantCode("DEFAULT");
    entity.setTenantName("Default Tenant");
    entity.setStatus(1);
    when(tenantMapper.selectList(null)).thenReturn(List.of(entity));

    mockMvc.perform(get("/api/v1/system/tenants"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].tenantCode").value("DEFAULT"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void createRoleReturnsCreatedPayload() throws Exception {
    mockMvc.perform(post("/api/v1/system/roles")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantId": 1001,
                  "roleCode": "AUDITOR",
                  "roleName": "Auditor",
                  "scopeType": "TENANT",
                  "status": 1,
                  "remark": "Created from test"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.roleCode").value("AUDITOR"));
  }
}
