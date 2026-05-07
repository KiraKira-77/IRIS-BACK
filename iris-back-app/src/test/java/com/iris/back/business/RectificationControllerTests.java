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
import com.iris.back.business.project.model.dto.RectificationDto;
import com.iris.back.business.project.model.request.RectificationListQuery;
import com.iris.back.business.project.service.ProjectService;
import com.iris.back.business.project.service.RectificationService;
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
class RectificationControllerTests {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RectificationService rectificationService;

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
  void listAndDetailReturnRectificationPayload() throws Exception {
    when(rectificationService.list(any(RectificationListQuery.class)))
        .thenReturn(PageResponse.of(1, 1, 10, List.of(sampleRectification("pending"))));
    when(rectificationService.get("9001")).thenReturn(sampleRectification("pending"));

    mockMvc.perform(get("/api/v1/rectifications")
            .param("status", "pending")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].id").value("9001"))
        .andExpect(jsonPath("$.data.records[0].code").value("RECT-9001"));

    mockMvc.perform(get("/api/v1/rectifications/9001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value("9001"))
        .andExpect(jsonPath("$.data.status").value("pending"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void createSubmitAndReviewRoutesReturnRectificationPayload() throws Exception {
    when(rectificationService.create(any())).thenReturn(sampleRectification("pending"));
    when(rectificationService.createWorkOrder("9001")).thenReturn(sampleRectification("in_progress"));
    when(rectificationService.returnWorkOrder(org.mockito.ArgumentMatchers.eq("9001"), any()))
        .thenReturn(sampleRectification("in_progress"));
    when(rectificationService.submit("9001")).thenReturn(sampleRectification("submitted"));
    when(rectificationService.review(org.mockito.ArgumentMatchers.eq("9001"), any()))
        .thenReturn(sampleRectification("approved"));

    mockMvc.perform(post("/api/v1/rectifications")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Manual issue",
                  "description": "Missing evidence",
                  "projectId": "7001",
                  "projectName": "Finance project",
                  "taskId": "7201",
                  "assigneeId": "2002",
                  "assigneeName": "Auditor",
                  "reviewerId": "2003",
                  "reviewerName": "Reviewer",
                  "deadline": "2026-05-20"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value("9001"))
        .andExpect(jsonPath("$.data.status").value("pending"));

    mockMvc.perform(post("/api/v1/rectifications/9001/work-order"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("in_progress"));

    mockMvc.perform(post("/api/v1/rectifications/9001/work-order/return")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "reason": "整改证据不足"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("in_progress"));

    mockMvc.perform(post("/api/v1/rectifications/9001/submit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("submitted"));

    mockMvc.perform(post("/api/v1/rectifications/9001/review")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "action": "approve",
                  "comment": "Accepted"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("approved"));
  }

  private RectificationDto sampleRectification(String status) {
    return new RectificationDto(
        "9001",
        "RECT-9001",
        "task",
        "7201",
        "Finance check task",
        "Check OMS evidence",
        "7001",
        "Finance project",
        "Missing approval record",
        "8001",
        "OMS-20260427-0001",
        "Fix local issue",
        "Missing approval record",
        "2002",
        "Auditor",
        "2003",
        "Reviewer",
        status,
        "2026-05-06 09:00:00",
        "2026-05-20 18:00:00",
        null,
        "approve",
        "OMS-RECT-001",
        "20",
        "已完成",
        "2026-05-06 09:10:00",
        "2026-05-08 10:30:00",
        null,
        null,
        null,
        List.of(),
        "Accepted",
        List.of(),
        "2026-05-06 09:00:00",
        "2026-05-06 09:00:00"
    );
  }
}
