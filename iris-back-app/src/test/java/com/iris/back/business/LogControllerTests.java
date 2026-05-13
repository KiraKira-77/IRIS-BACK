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
import com.iris.back.business.project.model.dto.ProjectOperationLogDto;
import com.iris.back.business.project.model.request.ProjectOperationLogQuery;
import com.iris.back.business.project.service.ProjectOperationLogService;
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
class LogControllerTests {

  @MockBean
  private AiModelConfigMapper aiModelConfigMapper;

  @MockBean
  private AiChatTraceMapper aiChatTraceMapper;

  @MockBean
  private AiChatTraceEventMapper aiChatTraceEventMapper;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ProjectOperationLogService operationLogService;

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
  void listReturnsOperationLogPayload() throws Exception {
    when(operationLogService.list(any(ProjectOperationLogQuery.class))).thenReturn(PageResponse.of(
        1,
        1,
        10,
        List.of(new ProjectOperationLogDto(
            "9001",
            "项目管理",
            "info",
            "审核工单",
            "业务确认通过",
            "2026-05-07 10:30:00",
            "7001",
            "7201",
            "8001",
            "审核工单",
            "Platform Administrator",
            "success"
        ))
    ));

    mockMvc.perform(get("/api/v1/logs")
            .param("keyword", "审核")
            .param("source", "项目管理")
            .param("level", "info")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].source").value("项目管理"))
        .andExpect(jsonPath("$.data.records[0].message").value("审核工单"))
        .andExpect(jsonPath("$.data.records[0].operatorName").value("Platform Administrator"));
  }
}
