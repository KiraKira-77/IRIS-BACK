package com.iris.back.business.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.project.mapper.BizProjectArchiveMapper;
import com.iris.back.business.project.mapper.BizProjectMemberMapper;
import com.iris.back.business.project.model.dto.ProjectArchiveDocumentDto;
import com.iris.back.business.project.model.dto.ProjectArchiveDto;
import com.iris.back.business.project.model.entity.BizProjectArchiveEntity;
import com.iris.back.business.project.model.entity.BizProjectMemberEntity;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.common.model.PageResponse;
import com.iris.back.common.util.DateTimeFormatters;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProjectArchiveService {

  private final BizProjectArchiveMapper projectArchiveMapper;
  private final BizProjectMemberMapper projectMemberMapper;
  private final CurrentUserContext currentUserContext;
  private final ObjectMapper objectMapper;

  public ProjectArchiveService(
      BizProjectArchiveMapper projectArchiveMapper,
      BizProjectMemberMapper projectMemberMapper,
      CurrentUserContext currentUserContext,
      ObjectMapper objectMapper
  ) {
    this.projectArchiveMapper = projectArchiveMapper;
    this.projectMemberMapper = projectMemberMapper;
    this.currentUserContext = currentUserContext;
    this.objectMapper = objectMapper;
  }

  public PageResponse<ProjectArchiveDto> list(String keyword, String status, Long page, Long pageSize) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    Set<Long> visibleProjectIds = visibleProjectIds(principal);
    if (visibleProjectIds.isEmpty()) {
      return PageResponse.of(0, normalizedPage(page), normalizedPageSize(pageSize), List.of());
    }
    String normalizedKeyword = trimToNull(keyword);
    String normalizedStatus = trimToNull(status);
    List<ProjectArchiveDto> filtered = nullToList(projectArchiveMapper.selectList(
        new LambdaQueryWrapper<BizProjectArchiveEntity>()
            .eq(BizProjectArchiveEntity::getTenantId, principal.tenantId())
            .in(BizProjectArchiveEntity::getProjectId, visibleProjectIds)
            .orderByDesc(BizProjectArchiveEntity::getArchiveDate)
            .orderByDesc(BizProjectArchiveEntity::getId)))
        .stream()
        .filter(archive -> normalizedKeyword == null
            || containsIgnoreCase(archive.getProjectName(), normalizedKeyword)
            || containsIgnoreCase(archive.getProjectCode(), normalizedKeyword))
        .filter(archive -> normalizedStatus == null || normalizedStatus.equals(archive.getStatus()))
        .map(this::toDto)
        .toList();
    long pageNo = normalizedPage(page);
    long size = normalizedPageSize(pageSize);
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * size);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + size);
    return PageResponse.of(filtered.size(), pageNo, size, filtered.subList(fromIndex, toIndex));
  }

  public ProjectArchiveDto detail(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    BizProjectArchiveEntity archive = projectArchiveMapper.selectById(parseId(id, "PROJECT_ARCHIVE_ID_INVALID"));
    if (archive == null || !Objects.equals(archive.getTenantId(), principal.tenantId())) {
      throw new BusinessException("PROJECT_ARCHIVE_NOT_FOUND", "PROJECT_ARCHIVE_NOT_FOUND");
    }
    if (!visibleProjectIds(principal).contains(archive.getProjectId())) {
      throw new BusinessException("PROJECT_ARCHIVE_FORBIDDEN", "PROJECT_ARCHIVE_FORBIDDEN");
    }
    return toDto(archive);
  }

  private Set<Long> visibleProjectIds(CurrentUserPrincipal principal) {
    return nullToList(projectMemberMapper.selectList(new LambdaQueryWrapper<BizProjectMemberEntity>()
        .eq(BizProjectMemberEntity::getTenantId, principal.tenantId())
        .eq(BizProjectMemberEntity::getPersonnelId, principal.userId())))
        .stream()
        .map(BizProjectMemberEntity::getProjectId)
        .collect(Collectors.toSet());
  }

  private ProjectArchiveDto toDto(BizProjectArchiveEntity archive) {
    return new ProjectArchiveDto(
        String.valueOf(archive.getId()),
        archive.getProjectId() == null ? null : String.valueOf(archive.getProjectId()),
        archive.getProjectCode(),
        archive.getProjectName(),
        DateTimeFormatters.formatDateTime(archive.getArchiveDate()),
        archive.getArchivedBy() == null ? null : String.valueOf(archive.getArchivedBy()),
        archive.getArchivedByName(),
        archive.getStatus(),
        archive.getTaskCount(),
        archive.getWorkOrderCount(),
        archive.getRectificationCount(),
        archive.getDocumentCount(),
        archive.getSnapshotVersion(),
        archive.getSnapshotJson(),
        extractDocuments(archive),
        DateTimeFormatters.formatDateTime(archive.getCreatedAt()),
        DateTimeFormatters.formatDateTime(archive.getUpdatedAt())
    );
  }

  private List<ProjectArchiveDocumentDto> extractDocuments(BizProjectArchiveEntity archive) {
    String snapshotJson = trimToNull(archive.getSnapshotJson());
    if (snapshotJson == null) {
      return List.of();
    }
    try {
      var root = objectMapper.readTree(snapshotJson);
      var workOrders = root.path("workOrders");
      if (!workOrders.isArray()) {
        return List.of();
      }
      List<ProjectArchiveDocumentDto> documents = new ArrayList<>();
      for (var workOrder : workOrders) {
        var attachments = objectMapper.readTree(workOrder.path("omsAttachmentPayload").asText("[]"));
        if (!attachments.isArray()) {
          continue;
        }
        for (int index = 0; index < attachments.size(); index++) {
          var attachment = attachments.get(index);
          String name = firstNonBlank(
              attachment.path("originalFileName").asText(null),
              attachment.path("fileName").asText(null),
              "附件-" + (documents.size() + 1)
          );
          documents.add(new ProjectArchiveDocumentDto(
              archive.getId() + "-" + workOrder.path("id").asText() + "-" + index,
              String.valueOf(archive.getId()),
              "OMS工单附件",
              name,
              List.of(objectMapper.convertValue(attachment, Object.class))
          ));
        }
      }
      return documents;
    } catch (JsonProcessingException exception) {
      return List.of();
    }
  }

  private String firstNonBlank(String first, String second, String fallback) {
    String normalizedFirst = trimToNull(first);
    if (normalizedFirst != null) {
      return normalizedFirst;
    }
    String normalizedSecond = trimToNull(second);
    return normalizedSecond == null ? fallback : normalizedSecond;
  }

  private Long parseId(String id, String code) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException exception) {
      throw new BusinessException(code, code);
    }
  }

  private long normalizedPage(Long page) {
    return page == null || page < 1 ? 1 : page;
  }

  private long normalizedPageSize(Long pageSize) {
    if (pageSize == null || pageSize < 1) {
      return 10;
    }
    return Math.min(pageSize, 100);
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private boolean containsIgnoreCase(String value, String keyword) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
  }

  private <T> List<T> nullToList(List<T> values) {
    return values == null ? List.of() : values;
  }
}
