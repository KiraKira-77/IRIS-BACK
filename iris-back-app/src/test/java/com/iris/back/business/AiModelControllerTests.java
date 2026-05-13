package com.iris.back.business;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iris.back.auth.service.AuthService;
import com.iris.back.business.ai.mapper.AiModelConfigMapper;
import com.iris.back.business.ai.mapper.AiChatTraceEventMapper;
import com.iris.back.business.ai.mapper.AiChatTraceMapper;
import com.iris.back.business.ai.model.dto.AiModelConfigDto;
import com.iris.back.business.ai.model.dto.AiModelTestResultDto;
import com.iris.back.business.ai.model.request.AiModelConfigUpsertRequest;
import com.iris.back.business.ai.service.AiModelConfigService;
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
import com.iris.back.business.project.service.AlertService;
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
class AiModelControllerTests {

  @MockBean
  private AiModelConfigMapper aiModelConfigMapper;

  @MockBean
  private AiChatTraceMapper aiChatTraceMapper;

  @MockBean
  private AiChatTraceEventMapper aiChatTraceEventMapper;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private AiModelConfigService aiModelConfigService;

  @MockBean
  private AlertService alertService;

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
  void listReturnsModelConfigsWithoutApiKey() throws Exception {
    when(aiModelConfigService.list(null, null, null, 1L, 10L)).thenReturn(PageResponse.of(
        1,
        1,
        10,
        List.of(modelDto())
    ));

    mockMvc.perform(get("/api/v1/models")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].name").value("整改建议生成模型"))
        .andExpect(jsonPath("$.data.records[0].apiKeyConfigured").value(true))
        .andExpect(jsonPath("$.data.records[0].apiKey").doesNotExist());
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void createAcceptsOpenAiCompatibleModelConfig() throws Exception {
    when(aiModelConfigService.create(any(AiModelConfigUpsertRequest.class))).thenReturn(modelDto());

    mockMvc.perform(post("/api/v1/models")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name":"整改建议生成模型",
                  "providerType":"openai_compatible",
                  "baseUrl":"https://ai.example.com/v1",
                  "modelName":"gpt-4.1-mini",
                  "apiKey":"sk-test",
                  "status":"online",
                  "defaultModel":true,
                  "timeoutSeconds":30,
                  "temperature":0.2,
                  "maxTokens":3000,
                  "remark":"用于 OMS 工单审核建议"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("整改建议生成模型"))
        .andExpect(jsonPath("$.data.apiKeyConfigured").value(true));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void testConnectionDelegatesToModelService() throws Exception {
    when(aiModelConfigService.testConnection("9001")).thenReturn(new AiModelTestResultDto(true, "ok", 123L));

    mockMvc.perform(post("/api/v1/models/9001/test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.success").value(true))
        .andExpect(jsonPath("$.data.message").value("ok"));

    verify(aiModelConfigService).testConnection("9001");
  }

  private AiModelConfigDto modelDto() {
    return new AiModelConfigDto(
        "9001",
        "整改建议生成模型",
        "llm",
        "openai_compatible",
        "OpenAI Compatible",
        "https://ai.example.com/v1",
        "gpt-4.1-mini",
        true,
        "online",
        true,
        30,
        0.2,
        3000,
        "用于 OMS 工单审核建议",
        "2026-05-08 10:00:00",
        "2026-05-08 10:00:00"
    );
  }
}
