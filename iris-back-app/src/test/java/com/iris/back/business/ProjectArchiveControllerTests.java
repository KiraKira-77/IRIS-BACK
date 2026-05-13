package com.iris.back.business;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iris.back.auth.service.AuthService;
import com.iris.back.business.ai.mapper.AiModelConfigMapper;
import com.iris.back.business.ai.mapper.AiChatTraceEventMapper;
import com.iris.back.business.ai.mapper.AiChatTraceMapper;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
import com.iris.back.business.checklist.service.ChecklistService;
import com.iris.back.business.plan.mapper.BizPlanItemMapper;
import com.iris.back.business.plan.mapper.BizPlanMapper;
import com.iris.back.business.plan.service.PlanService;
import com.iris.back.business.project.mapper.BizProjectArchiveMapper;
import com.iris.back.business.project.mapper.BizProjectMapper;
import com.iris.back.business.project.mapper.BizProjectMemberMapper;
import com.iris.back.business.project.mapper.BizProjectOperationLogMapper;
import com.iris.back.business.project.mapper.BizProjectRectificationMapper;
import com.iris.back.business.project.mapper.BizProjectTaskMapper;
import com.iris.back.business.project.mapper.BizProjectTaskWorkOrderMapper;
import com.iris.back.business.project.model.dto.ProjectArchiveDto;
import com.iris.back.business.project.service.ProjectArchiveService;
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
class ProjectArchiveControllerTests {

  @MockBean
  private AiModelConfigMapper aiModelConfigMapper;

  @MockBean
  private AiChatTraceMapper aiChatTraceMapper;

  @MockBean
  private AiChatTraceEventMapper aiChatTraceEventMapper;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ProjectArchiveService projectArchiveService;

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
  private BizProjectArchiveMapper bizProjectArchiveMapper;

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
  void listArchivesReturnsProjectArchiveLedger() throws Exception {
    when(projectArchiveService.list(any(), any(), any(), any()))
        .thenReturn(PageResponse.of(1, 1, 10, List.of(sampleArchive())));

    mockMvc.perform(get("/api/v1/archives")
            .param("keyword", "Finance")
            .param("status", "active")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].projectName").value("Finance project"))
        .andExpect(jsonPath("$.data.records[0].taskCount").value(2))
        .andExpect(jsonPath("$.data.records[0].documentCount").value(1));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void archiveDetailReturnsSnapshotJson() throws Exception {
    when(projectArchiveService.detail("9101")).thenReturn(sampleArchive());

    mockMvc.perform(get("/api/v1/archives/9101"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value("9101"))
        .andExpect(jsonPath("$.data.snapshotJson").value("{\"project\":{\"projectName\":\"Finance project\"}}"));
  }

  private ProjectArchiveDto sampleArchive() {
    return new ProjectArchiveDto(
        "9101",
        "7001",
        "PRJ-2026-001",
        "Finance project",
        "2026-05-07 15:30:00",
        "2001",
        "Platform Administrator",
        "active",
        2,
        1,
        1,
        1,
        "v1",
        "{\"project\":{\"projectName\":\"Finance project\"}}",
        List.of(),
        "2026-05-07 15:30:00",
        "2026-05-07 15:30:00"
    );
  }
}
