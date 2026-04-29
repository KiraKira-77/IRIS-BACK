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
import com.iris.back.business.checklist.service.ChecklistService;
import com.iris.back.business.plan.mapper.BizPlanItemMapper;
import com.iris.back.business.plan.mapper.BizPlanMapper;
import com.iris.back.business.plan.model.dto.PlanDto;
import com.iris.back.business.plan.model.dto.PlanItemDto;
import com.iris.back.business.plan.model.request.PlanListQuery;
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
class PlanControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PlanService planService;

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

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void listReturnsPlanPayloadWithItems() throws Exception {
    when(planService.list(any(PlanListQuery.class))).thenReturn(PageResponse.of(
        1,
        1,
        10,
        List.of(samplePlan("approved"))
    ));

    mockMvc.perform(get("/api/v1/plans")
            .param("keyword", "annual")
            .param("year", "2026")
            .param("status", "approved")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].code").value("PL-2026-001"))
        .andExpect(jsonPath("$.data.records[0].ownerScopeId").value("9001"))
        .andExpect(jsonPath("$.data.records[0].grants[0].scopeId").value("9002"))
        .andExpect(jsonPath("$.data.records[0].grants[0].actions[0]").value("view"))
        .andExpect(jsonPath("$.data.records[0].items[0].targetScope").value("Finance"))
        .andExpect(jsonPath("$.data.records[0].items[0].checklistIds[0]").value("8801"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void createReturnsCreatedPlanPayload() throws Exception {
    when(planService.create(any())).thenReturn(samplePlan("draft"));

    mockMvc.perform(post("/api/v1/plans")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "code": "PL-2026-001",
                  "name": "2026 annual control plan",
                  "cycle": "yearly",
                  "year": 2026,
                  "period": "full-year",
                  "status": "draft",
                  "description": "annual scope",
                  "ownerScopeId": "9001",
                  "grantScopeIds": ["9002"],
                  "items": [{
                    "targetScope": "Finance",
                    "checklistIds": ["8801"],
                    "plannedStartDate": "2026-01-01",
                    "plannedEndDate": "2026-12-31",
                    "assignee": "2001"
                  }]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("draft"))
        .andExpect(jsonPath("$.data.items[0].plannedStartDate").value("2026-01-01"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void updateSubmitApproveAndDeleteReturnSuccess() throws Exception {
    when(planService.update(org.mockito.ArgumentMatchers.eq("9001"), any())).thenReturn(samplePlan("draft"));
    when(planService.submit("9001")).thenReturn(samplePlan("approved"));
    when(planService.approve("9001")).thenReturn(samplePlan("approved"));

    mockMvc.perform(put("/api/v1/plans/9001")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "name": "2026 annual control plan",
                  "cycle": "yearly",
                  "year": 2026,
                  "period": "full-year",
                  "ownerScopeId": "9001",
                  "grantScopeIds": ["9002"],
                  "items": []
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value("9001"));

    mockMvc.perform(post("/api/v1/plans/9001/submit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("approved"));

    mockMvc.perform(post("/api/v1/plans/9001/approve"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("approved"));

    mockMvc.perform(delete("/api/v1/plans/9001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  private PlanDto samplePlan(String status) {
    return new PlanDto(
        "9001",
        "PL-2026-001",
        "2026 annual control plan",
        "yearly",
        2026,
        "full-year",
        status,
        "annual scope",
        "9001",
        List.of(new PlanDto.ScopeGrantDto("9002", List.of("view"))),
        List.of(new PlanItemDto(
            "9101",
            "9001",
            1,
            "Finance",
            List.of("8801"),
            "2026-01-01",
            "2026-12-31",
            "2001",
            null,
            null
        )),
        null,
        List.of(),
        "2001",
        null,
        "2026-04-27T10:00:00",
        "2026-04-27T10:00:00"
    );
  }
}
