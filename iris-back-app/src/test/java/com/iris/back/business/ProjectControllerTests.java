package com.iris.back.business;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iris.back.auth.service.AuthService;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
import com.iris.back.business.checklist.service.ChecklistService;
import com.iris.back.business.plan.mapper.BizPlanItemMapper;
import com.iris.back.business.plan.mapper.BizPlanMapper;
import com.iris.back.business.plan.service.PlanService;
import com.iris.back.business.project.mapper.BizProjectMapper;
import com.iris.back.business.project.mapper.BizProjectMemberMapper;
import com.iris.back.business.project.mapper.BizProjectOperationLogMapper;
import com.iris.back.business.project.mapper.BizProjectRectificationMapper;
import com.iris.back.business.project.mapper.BizProjectTaskMapper;
import com.iris.back.business.project.mapper.BizProjectTaskWorkOrderMapper;
import com.iris.back.business.project.model.dto.ProjectDto;
import com.iris.back.business.project.model.request.ProjectListQuery;
import com.iris.back.business.project.service.ProjectService;
import com.iris.back.business.standard.mapper.BizStandardMapper;
import com.iris.back.business.standard.service.StandardService;
import com.iris.back.common.model.PageResponse;
import com.iris.back.framework.security.AuthSessionStore;
import com.iris.back.system.mapper.SysFileMapper;
import com.iris.back.system.mapper.SysFileRefMapper;
import com.iris.back.system.mapper.SysOrgMapper;
import com.iris.back.system.mapper.SysResourceScopeMapper;
import com.iris.back.system.mapper.SysResourceScopeMemberMapper;
import com.iris.back.system.mapper.SysResourceScopeUsageMapper;
import com.iris.back.system.mapper.SysRoleMapper;
import com.iris.back.system.mapper.SysRoleMenuMapper;
import com.iris.back.system.mapper.SysTenantMapper;
import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.mapper.SysUserRoleMapper;
import com.iris.back.system.service.FileService;
import java.util.List;
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
class ProjectControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ProjectService projectService;

  @MockBean
  private ChecklistService checklistService;

  @MockBean
  private PlanService planService;

  @MockBean
  private StandardService standardService;

  @MockBean
  private AuthService authService;

  @MockBean
  private FileService fileService;

  @MockBean
  private AuthSessionStore authSessionStore;

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
  private SysFileMapper sysFileMapper;

  @MockBean
  private SysFileRefMapper sysFileRefMapper;

  @MockBean
  private BizStandardMapper bizStandardMapper;

  @MockBean
  private BizChecklistMapper bizChecklistMapper;

  @MockBean
  private BizChecklistItemMapper bizChecklistItemMapper;

  @MockBean
  private BizPlanMapper bizPlanMapper;

  @MockBean
  private BizPlanItemMapper bizPlanItemMapper;

  @MockBean
  private BizProjectMapper bizProjectMapper;

  @MockBean
  private BizProjectMemberMapper bizProjectMemberMapper;

  @MockBean
  private BizProjectTaskMapper bizProjectTaskMapper;

  @MockBean
  private BizProjectTaskWorkOrderMapper bizProjectTaskWorkOrderMapper;

  @MockBean
  private BizProjectRectificationMapper bizProjectRectificationMapper;

  @MockBean
  private BizProjectOperationLogMapper bizProjectOperationLogMapper;

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void listReturnsProjectPayload() throws Exception {
    when(projectService.list(any(ProjectListQuery.class))).thenReturn(PageResponse.of(1, 1, 10, List.of(sampleProject())));

    mockMvc.perform(get("/api/v1/projects")
            .param("keyword", "Finance")
            .param("status", "in_progress")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value("7001"))
        .andExpect(jsonPath("$.data.records[0].status").value("in_progress"))
        .andExpect(jsonPath("$.data.records[0].taskCount").value(2));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void createAndLifecycleRoutesReturnProjectPayload() throws Exception {
    when(projectService.create(any())).thenReturn(sampleProject());
    when(projectService.start("7001")).thenReturn(sampleProject("in_progress"));
    when(projectService.complete("7001")).thenReturn(sampleProject("completed"));

    mockMvc.perform(post("/api/v1/projects")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "PRJ-2026-001",
                  "name": "Finance project",
                  "source": "manual",
                  "startDate": "2026-04-27",
                  "leaderId": "2001",
                  "leaderName": "Admin",
                  "checklistIds": ["8801"],
                  "members": []
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value("7001"));

    mockMvc.perform(post("/api/v1/projects/7001/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("in_progress"));

    mockMvc.perform(post("/api/v1/projects/7001/complete"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("completed"));
  }

  private ProjectDto sampleProject() {
    return sampleProject("in_progress");
  }

  private ProjectDto sampleProject(String status) {
    return new ProjectDto(
        "7001",
        "PRJ-2026-001",
        "Finance project",
        "manual",
        null,
        null,
        "Finance controls",
        "2026-04-27",
        null,
        status,
        List.of("maintenance"),
        List.of("maintenance"),
        "2001",
        "Admin",
        List.of("8801"),
        "none",
        null,
        null,
        null,
        2,
        1,
        0,
        50,
        List.of(),
        List.of(),
        List.of("start", "complete"),
        "2026-04-27T10:00:00",
        "2026-04-27T10:00:00"
    );
  }
}
