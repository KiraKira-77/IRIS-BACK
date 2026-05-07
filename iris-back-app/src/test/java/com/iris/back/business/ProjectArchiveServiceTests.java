package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.project.mapper.BizProjectArchiveMapper;
import com.iris.back.business.project.mapper.BizProjectMemberMapper;
import com.iris.back.business.project.model.dto.ProjectArchiveDto;
import com.iris.back.business.project.model.entity.BizProjectArchiveEntity;
import com.iris.back.business.project.model.entity.BizProjectMemberEntity;
import com.iris.back.business.project.service.ProjectArchiveService;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectArchiveServiceTests {

  @Mock
  private BizProjectArchiveMapper projectArchiveMapper;

  @Mock
  private BizProjectMemberMapper projectMemberMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  private ProjectArchiveService projectArchiveService;

  @BeforeEach
  void setUp() {
    projectArchiveService = new ProjectArchiveService(
        projectArchiveMapper,
        projectMemberMapper,
        currentUserContext,
        new ObjectMapper()
    );
  }

  @Test
  void detailExtractsAttachmentsFromOmsWorkOrderLogs() {
    mockCurrentUser();
    BizProjectArchiveEntity archive = archiveWithSnapshot("""
        {
          "workOrders": [
            {
              "id": "8001",
              "omsAttachmentPayload": "[]",
              "omsLogPayload": "[{\\"RECORD_GDCZ\\":\\"日志\\",\\"RECORD_CZXQ\\":\\"补充材料\\",\\"RECORD_FJ\\":\\"[{\\\\\\"originalFileName\\\\\\":\\\\\\"测试报告20260421.docx\\\\\\",\\\\\\"fileName\\\\\\":\\\\\\"itms/ad6bc26.docx\\\\\\",\\\\\\"minioUrl\\\\\\":\\\\\\"http://host/file.docx\\\\\\",\\\\\\"id\\\\\\":\\\\\\"file-log-1\\\\\\"}]\\"}]"
            }
          ]
        }
        """);
    when(projectArchiveMapper.selectById(9101L)).thenReturn(archive);
    when(projectMemberMapper.selectList(any())).thenReturn(List.of(member(7001L, 2001L)));

    ProjectArchiveDto detail = projectArchiveService.detail("9101");

    assertThat(detail.documents()).hasSize(1);
    assertThat(detail.documents().getFirst().category()).isEqualTo("OMS工单日志附件");
    assertThat(detail.documents().getFirst().name()).isEqualTo("测试报告20260421.docx");
  }

  private BizProjectArchiveEntity archiveWithSnapshot(String snapshotJson) {
    BizProjectArchiveEntity entity = new BizProjectArchiveEntity();
    entity.setId(9101L);
    entity.setTenantId(1001L);
    entity.setProjectId(7001L);
    entity.setProjectCode("PRJ-2026-001");
    entity.setProjectName("Finance project");
    entity.setStatus("active");
    entity.setSnapshotVersion("v1");
    entity.setSnapshotJson(snapshotJson);
    entity.setArchiveDate(LocalDateTime.of(2026, 5, 7, 15, 30));
    entity.setArchivedBy(2001L);
    entity.setArchivedByName("Platform Administrator");
    entity.setTaskCount(1);
    entity.setWorkOrderCount(1);
    entity.setRectificationCount(0);
    entity.setDocumentCount(1);
    entity.setCreatedAt(LocalDateTime.of(2026, 5, 7, 15, 30));
    entity.setUpdatedAt(LocalDateTime.of(2026, 5, 7, 15, 30));
    return entity;
  }

  private BizProjectMemberEntity member(Long projectId, Long personnelId) {
    BizProjectMemberEntity entity = new BizProjectMemberEntity();
    entity.setId(projectId + personnelId);
    entity.setTenantId(1001L);
    entity.setProjectId(projectId);
    entity.setPersonnelId(personnelId);
    entity.setPersonnelName("User " + personnelId);
    entity.setRole("leader");
    return entity;
  }

  private void mockCurrentUser() {
    when(currentUserContext.requireCurrentUser()).thenReturn(new CurrentUserPrincipal(
        "token",
        2001L,
        1001L,
        "admin",
        "Platform Administrator",
        "IRIS",
        List.of("SUPER_ADMIN")
    ));
  }
}
