package com.iris.back.business;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iris.back.auth.service.AuthService;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
import com.iris.back.business.checklist.model.dto.ChecklistDto;
import com.iris.back.business.checklist.model.dto.ChecklistItemDto;
import com.iris.back.business.checklist.model.request.ChecklistListQuery;
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
class ChecklistControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ChecklistService checklistService;

  @MockBean
  private StandardService standardService;

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
  private FileService fileService;

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

  @MockBean
  private PlanService planService;

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void listReturnsChecklistPayloadWithItemsAndScopes() throws Exception {
    when(checklistService.list(any(ChecklistListQuery.class))).thenReturn(PageResponse.of(
        1,
        1,
        10,
        List.of(sampleChecklist())
    ));

    mockMvc.perform(get("/api/v1/checklists")
            .param("keyword", "资金")
            .param("status", "active")
            .param("scopeId", "9001")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].code").value("CL-2026-001"))
        .andExpect(jsonPath("$.data.records[0].name").value("资金活动内控检查清单"))
        .andExpect(jsonPath("$.data.records[0].version").value("V1.0"))
        .andExpect(jsonPath("$.data.records[0].ownerScopeId").value("9001"))
        .andExpect(jsonPath("$.data.records[0].grants[0].scopeId").value("9002"))
        .andExpect(jsonPath("$.data.records[0].grants[0].actions[0]").value("view"))
        .andExpect(jsonPath("$.data.records[0].items[0].controlFrequency").value("monthly"))
        .andExpect(jsonPath("$.data.records[0].items[0].evaluationType").value("operation"))
        .andExpect(jsonPath("$.data.records[0].items[0].organizationIds[0]").value("org-finance"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void createReturnsCreatedChecklistPayload() throws Exception {
    when(checklistService.create(any())).thenReturn(sampleChecklist());

    mockMvc.perform(post("/api/v1/checklists")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "CL-2026-001",
                  "name": "资金活动内控检查清单",
                  "description": "资金活动控制点",
                  "version": "V1.0",
                  "ownerScopeId": "9001",
                  "grantScopeIds": ["9002"],
                  "status": "active",
                  "uploadDate": "2026-04-27",
                  "items": [{
                    "content": "银行对账是否及时完成",
                    "criterion": "每月5日前完成上月银行余额调节表",
                    "controlFrequency": "monthly",
                    "evaluationType": "operation",
                    "organizationIds": ["org-finance"]
                  }]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.code").value("CL-2026-001"))
        .andExpect(jsonPath("$.data.items[0].criterion").value("每月5日前完成上月银行余额调节表"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void updateReturnsUpdatedChecklistPayload() throws Exception {
    when(checklistService.update(org.mockito.ArgumentMatchers.eq("8801"), any())).thenReturn(sampleChecklist());

    mockMvc.perform(put("/api/v1/checklists/8801")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "CL-2026-001",
                  "name": "资金活动内控检查清单",
                  "version": "V1.1",
                  "ownerScopeId": "9001",
                  "grantScopeIds": ["9002"],
                  "status": "draft",
                  "items": []
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value("8801"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void deleteReturnsSuccess() throws Exception {
    mockMvc.perform(delete("/api/v1/checklists/8801"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  private ChecklistDto sampleChecklist() {
    return new ChecklistDto(
        "8801",
        "CL-2026-001",
        "资金活动内控检查清单",
        "资金活动控制点",
        "V1.0",
        "9001",
        List.of(new ChecklistDto.ScopeGrantDto("9002", List.of("view"))),
        List.of(new ChecklistItemDto(
            "9901",
            "8801",
            1,
            "银行对账是否及时完成",
            "每月5日前完成上月银行余额调节表",
            "monthly",
            "operation",
            List.of("org-finance")
        )),
        "active",
        "2026-04-27",
        "2026-04-27T10:00:00",
        "2026-04-27T10:00:00"
    );
  }
}
