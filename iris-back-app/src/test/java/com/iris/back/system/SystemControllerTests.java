package com.iris.back.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iris.back.auth.service.AuthService;
import com.iris.back.business.standard.mapper.BizStandardMapper;
import com.iris.back.business.standard.service.StandardService;
import com.iris.back.framework.security.AuthSessionStore;
import com.iris.back.system.mapper.SysOrgMapper;
import com.iris.back.system.mapper.SysResourceScopeMapper;
import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.mapper.SysResourceScopeUsageMapper;
import com.iris.back.system.mapper.SysRoleMenuMapper;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.mapper.SysTenantMapper;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.mapper.SysUserRoleMapper;
import com.iris.back.system.model.entity.SysResourceScopeEntity;
import com.iris.back.system.model.entity.SysTenantEntity;
import com.iris.back.system.model.entity.SysUserEntity;
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
  private SysUserRoleMapper userRoleMapper;

  @MockBean
  private SysRoleMapper roleMapper;

  @MockBean
  private SysRoleMenuMapper roleMenuMapper;

  @MockBean
  private SysResourceScopeMapper resourceScopeMapper;

  @MockBean
  private SysResourceScopeMemberMapper resourceScopeMemberMapper;

  @MockBean
  private SysResourceScopeUsageMapper resourceScopeUsageMapper;

  @MockBean
  private AuthSessionStore authSessionStore;

  @MockBean
  private AuthService authService;

  @MockBean
  private StandardService standardService;

  @MockBean
  private BizStandardMapper bizStandardMapper;

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
                  "remark": "Created from test",
                  "menuCodes": ["workbench.dashboard", "resource.standards"]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.roleCode").value("AUDITOR"))
        .andExpect(jsonPath("$.data.menuCodes[0]").value("workbench.dashboard"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void listResourceScopesReturnsSeedScopes() throws Exception {
    SysResourceScopeEntity entity = new SysResourceScopeEntity();
    entity.setId(9101L);
    entity.setTenantId(1001L);
    entity.setScopeCode("FINANCE");
    entity.setScopeName("Finance Scope");
    entity.setScopeType("RESOURCE");
    entity.setStatus(1);
    when(resourceScopeMapper.selectList(null)).thenReturn(List.of(entity));

    mockMvc.perform(get("/api/v1/system/resource-scopes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].id").value("9101"))
        .andExpect(jsonPath("$.data[0].scopeCode").value("FINANCE"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void listUsersReturnsStringIds() throws Exception {
    SysUserEntity entity = new SysUserEntity();
    entity.setId(2047157959175438304L);
    entity.setTenantId(1001L);
    entity.setOrgId(1101L);
    entity.setAccount("auditor");
    entity.setUsername("Auditor");
    entity.setEmail("auditor@iris.local");
    entity.setMobile("13900000000");
    entity.setStatus(1);
    when(userMapper.selectList(null)).thenReturn(List.of(entity));
    when(userMapper.selectRoleIdsByUserId(1001L, 2047157959175438304L)).thenReturn(List.of(3002L));
    when(userMapper.selectRoleCodesByUserId(1001L, 2047157959175438304L)).thenReturn(List.of("AUDITOR"));

    mockMvc.perform(get("/api/v1/system/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].id").value("2047157959175438304"))
        .andExpect(jsonPath("$.data[0].roleIds[0]").value("3002"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void createResourceScopeReturnsCreatedPayload() throws Exception {
    mockMvc.perform(post("/api/v1/system/resource-scopes")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantId": 1001,
                  "scopeCode": "IT",
                  "scopeName": "IT Scope",
                  "scopeType": "RESOURCE",
                  "status": 1,
                  "remark": "Created from test"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.scopeCode").value("IT"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void resetPasswordReturnsSuccess() throws Exception {
    SysUserEntity entity = new SysUserEntity();
    entity.setId(2002L);
    entity.setTenantId(1001L);
    entity.setAccount("auditor");
    entity.setUsername("Auditor");
    entity.setPasswordHash("old-hash");
    when(userMapper.selectById(2002L)).thenReturn(entity);

    mockMvc.perform(post("/api/v1/system/users/2002/reset-password"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void deleteUserReturnsSuccess() throws Exception {
    SysUserEntity entity = new SysUserEntity();
    entity.setId(2002L);
    entity.setTenantId(1001L);
    entity.setAccount("auditor");
    entity.setUsername("Auditor");
    when(userMapper.selectById(2002L)).thenReturn(entity);

    mockMvc.perform(delete("/api/v1/system/users/2002"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void deleteRoleReturnsSuccess() throws Exception {
    var entity = new com.iris.back.system.model.entity.SysRoleEntity();
    entity.setId(3002L);
    entity.setTenantId(1001L);
    entity.setRoleCode("AUDITOR");
    entity.setRoleName("Auditor");
    when(roleMapper.selectById(3002L)).thenReturn(entity);
    when(userRoleMapper.countActiveAssignments(3002L)).thenReturn(0L);

    mockMvc.perform(delete("/api/v1/system/roles/3002"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void deleteResourceScopeReturnsSuccess() throws Exception {
    SysResourceScopeEntity entity = new SysResourceScopeEntity();
    entity.setId(9101L);
    entity.setTenantId(1001L);
    entity.setScopeCode("FINANCE");
    entity.setScopeName("Finance Scope");
    entity.setScopeType("RESOURCE");
    entity.setStatus(1);
    when(resourceScopeMapper.selectById(9101L)).thenReturn(entity);
    when(resourceScopeUsageMapper.countOwnerReferences(9101L)).thenReturn(0L);
    when(resourceScopeUsageMapper.countSharedReferences(9101L)).thenReturn(0L);

    mockMvc.perform(delete("/api/v1/system/resource-scopes/9101"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }
}
