package com.iris.back.business;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iris.back.auth.service.AuthService;
import com.iris.back.business.checklist.mapper.BizChecklistItemMapper;
import com.iris.back.business.checklist.mapper.BizChecklistMapper;
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
import com.iris.back.business.standard.model.dto.StandardDto;
import com.iris.back.business.standard.model.request.StandardListQuery;
import com.iris.back.business.standard.model.request.StandardRollbackRequest;
import com.iris.back.business.standard.model.request.StandardUpgradeRequest;
import com.iris.back.business.standard.service.StandardService;
import com.iris.back.common.model.PageResponse;
import com.iris.back.framework.security.AuthSessionStore;
import com.iris.back.system.model.dto.FileAttachmentDto;
import com.iris.back.system.mapper.SysOrgMapper;
import com.iris.back.system.mapper.SysFileMapper;
import com.iris.back.system.mapper.SysFileRefMapper;
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
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
})
@AutoConfigureMockMvc
class StandardControllerTests {

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
  void listReturnsRealStandardPayload() throws Exception {
    when(standardService.list(any(StandardListQuery.class))).thenReturn(PageResponse.of(
        1,
        1,
        10,
        List.of(new StandardDto(
        "9901",
        "std-001",
        "STD-FIN-001",
        "Finance Standard",
        "internal",
        "V1.0",
        "2026-04-23",
        "active",
        List.of(),
        "desc",
        "2026-04-23T00:00:00",
        "2026-04-23T00:00:00",
        1,
        null,
        "SCOPED",
        "9001",
        List.of(new StandardDto.ScopeGrantDto("9002", List.of("view"))),
        "initial draft",
        "Platform Administrator"
    ))));

    mockMvc.perform(get("/api/v1/standards")
            .param("keyword", "Finance")
            .param("category", "internal")
            .param("status", "active")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.pageNo").value(1))
        .andExpect(jsonPath("$.data.records[0].standardCode").value("STD-FIN-001"))
        .andExpect(jsonPath("$.data.records[0].title").value("Finance Standard"))
        .andExpect(jsonPath("$.data.records[0].visibilityLevel").value("SCOPED"))
        .andExpect(jsonPath("$.data.records[0].ownerScopeId").value("9001"))
        .andExpect(jsonPath("$.data.records[0].operatorName").value("Platform Administrator"))
        .andExpect(jsonPath("$.data.records[0].grants[0].scopeId").value("9002"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void createReturnsCreatedStandardPayload() throws Exception {
    when(standardService.create(any())).thenReturn(new StandardDto(
        "9902",
        "9902",
        "STD-FIN-002",
        "Finance Standard",
        "internal",
        "V1.0",
        "2026-04-23",
        "draft",
        List.of(),
        "desc",
        "2026-04-23T00:00:00",
        "2026-04-23T00:00:00",
        1,
        null,
        "PUBLIC",
        "9001",
        List.of(new StandardDto.ScopeGrantDto("9002", List.of("view"))),
        "initial draft",
        "Platform Administrator"
    ));

    mockMvc.perform(post("/api/v1/standards")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "tenantId": 1001,
                  "title": "Finance Standard",
                  "category": "internal",
                  "version": "V1.0",
                  "status": "draft",
                  "publishDate": "2026-04-23",
                  "description": "desc",
                  "standardCode": "STD-FIN-002",
                  "visibilityLevel": "PUBLIC",
                  "ownerScopeId": "9001",
                  "grantScopeIds": ["9002"],
                  "changeLog": "initial draft"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value("9902"))
        .andExpect(jsonPath("$.data.standardCode").value("STD-FIN-002"))
        .andExpect(jsonPath("$.data.ownerScopeId").value("9001"))
        .andExpect(jsonPath("$.data.operatorName").value("Platform Administrator"))
        .andExpect(jsonPath("$.data.grants[0].scopeId").value("9002"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void upgradeReturnsCreatedStandardPayload() throws Exception {
    when(standardService.upgrade(org.mockito.ArgumentMatchers.eq("9902"), any(StandardUpgradeRequest.class)))
        .thenReturn(new StandardDto(
            "9903",
            "9902",
            "STD-FIN-002",
            "Finance Standard",
            "internal",
            "V2.0",
            null,
            "draft",
            List.of(),
            "desc",
            "2026-04-24T00:00:00",
            "2026-04-24T00:00:00",
            2,
            "9902",
            "PUBLIC",
            "9001",
            List.of(new StandardDto.ScopeGrantDto("9002", List.of("view"))),
            "upgrade draft",
            "Platform Administrator"
        ));

    mockMvc.perform(post("/api/v1/standards/9902/upgrade")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "version": "V2.0",
                  "changeLog": "upgrade draft"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value("9903"))
        .andExpect(jsonPath("$.data.version").value("V2.0"))
        .andExpect(jsonPath("$.data.status").value("draft"))
        .andExpect(jsonPath("$.data.previousVersionId").value("9902"))
        .andExpect(jsonPath("$.data.operatorName").value("Platform Administrator"))
        .andExpect(jsonPath("$.data.changeLog").value("upgrade draft"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void publishReturnsPublishedStandardPayload() throws Exception {
    when(standardService.publish("9903")).thenReturn(new StandardDto(
        "9903",
        "9902",
        "STD-FIN-002",
        "Finance Standard",
        "internal",
        "V2.0",
        "2026-04-24",
        "active",
        List.of(),
        "desc",
        "2026-04-24T00:00:00",
        "2026-04-24T00:00:00",
        2,
        "9902",
        "PUBLIC",
        "9001",
        List.of(),
        "upgrade draft",
        "Platform Administrator"
    ));

    mockMvc.perform(post("/api/v1/standards/9903/publish"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("standard published"))
        .andExpect(jsonPath("$.data.status").value("active"))
        .andExpect(jsonPath("$.data.publishDate").value("2026-04-24"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void rollbackReturnsCreatedDraftPayload() throws Exception {
    when(standardService.rollback(org.mockito.ArgumentMatchers.eq("9901"), any(StandardRollbackRequest.class)))
        .thenReturn(new StandardDto(
            "9904",
            "9902",
            "STD-FIN-002",
            "Finance Standard",
            "internal",
            "V3.0",
            null,
            "draft",
            List.of(),
            "desc",
            "2026-04-24T00:00:00",
            "2026-04-24T00:00:00",
            3,
            "9901",
            "PUBLIC",
            "9001",
            List.of(),
            "[回退] 回退至 V1.0：restore old controls",
            "Platform Administrator"
        ));

    mockMvc.perform(post("/api/v1/standards/9901/rollback")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "version": "V3.0",
                  "reason": "restore old controls"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("standard rollback draft created"))
        .andExpect(jsonPath("$.data.id").value("9904"))
        .andExpect(jsonPath("$.data.status").value("draft"))
        .andExpect(jsonPath("$.data.previousVersionId").value("9901"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void uploadAttachmentReturnsUploadedPayload() throws Exception {
    when(standardService.uploadAttachment(org.mockito.ArgumentMatchers.eq("9902"), any()))
        .thenReturn(new FileAttachmentDto(
            "7001",
            "evidence.pdf",
            "http://minio/download",
            1024L,
            "application/pdf",
            "Platform Administrator",
            "2026-04-24T11:00:00"
        ));

    mockMvc.perform(multipart("/api/v1/standards/9902/attachments")
            .file(new MockMultipartFile("file", "evidence.pdf", "application/pdf", "demo".getBytes())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.id").value("7001"))
        .andExpect(jsonPath("$.data.name").value("evidence.pdf"))
        .andExpect(jsonPath("$.data.type").value("application/pdf"));
  }

  @Test
  @WithMockUser(username = "admin", roles = "PLATFORM_ADMIN")
  void deleteAttachmentReturnsSuccessPayload() throws Exception {
    doNothing().when(standardService).deleteAttachment("9902", "7001");

    mockMvc.perform(delete("/api/v1/standards/9902/attachments/7001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("standard attachment deleted"));
  }
}
