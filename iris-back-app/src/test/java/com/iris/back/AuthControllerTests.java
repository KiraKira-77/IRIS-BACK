package com.iris.back;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import com.iris.back.auth.model.CurrentUserResponse;
import com.iris.back.auth.model.LoginResponse;
import com.iris.back.auth.service.AuthService;
import com.iris.back.framework.security.AuthSessionStore;
import com.iris.back.system.mapper.SysOrgMapper;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.mapper.SysTenantMapper;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.mapper.SysUserRoleMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@AutoConfigureMockMvc
class AuthControllerTests {

  @Autowired
  private MockMvc mockMvc;

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

  @Test
  void loginReturnsSuccessEnvelope() throws Exception {
    when(authService.login(org.mockito.ArgumentMatchers.any())).thenReturn(
        new LoginResponse("test-token", 2001L, 1001L, "Platform Administrator", "Default Tenant")
    );

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "account": "admin",
                  "password": "admin123"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.token").isNotEmpty())
        .andExpect(jsonPath("$.data.tenantId").value(1001));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void currentUserReturnsDefaultProfile() throws Exception {
    when(authService.currentUser()).thenReturn(
        new CurrentUserResponse(
            2001L,
            1001L,
            "admin",
            "Platform Administrator",
            "Default Tenant",
            java.util.List.of("PLATFORM_ADMIN")
        )
    );

    mockMvc.perform(get("/api/v1/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.userId").value(2001))
        .andExpect(jsonPath("$.data.username").value("admin"))
        .andExpect(jsonPath("$.data.roles[0]").value("PLATFORM_ADMIN"));
  }
}
