package com.iris.back.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import com.iris.back.system.config.MinioProperties;
import com.iris.back.system.mapper.SysFileMapper;
import com.iris.back.system.mapper.SysFileRefMapper;
import com.iris.back.system.model.dto.FileAttachmentDto;
import com.iris.back.system.model.dto.FileRefView;
import com.iris.back.system.model.entity.SysFileEntity;
import com.iris.back.system.model.entity.SysFileRefEntity;
import com.iris.back.system.service.storage.ObjectStorageService;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

  private static final String STORAGE_TYPE_MINIO = "MINIO";

  private final SysFileMapper fileMapper;
  private final SysFileRefMapper fileRefMapper;
  private final ObjectStorageService objectStorageService;
  private final CurrentUserContext currentUserContext;
  private final IdentifierGenerator identifierGenerator;
  private final MinioProperties minioProperties;

  public FileService(
      SysFileMapper fileMapper,
      SysFileRefMapper fileRefMapper,
      ObjectStorageService objectStorageService,
      CurrentUserContext currentUserContext,
      IdentifierGenerator identifierGenerator,
      MinioProperties minioProperties
  ) {
    this.fileMapper = fileMapper;
    this.fileRefMapper = fileRefMapper;
    this.objectStorageService = objectStorageService;
    this.currentUserContext = currentUserContext;
    this.identifierGenerator = identifierGenerator;
    this.minioProperties = minioProperties;
  }

  public FileAttachmentDto upload(String bizType, Long tenantId, Long bizId, MultipartFile file) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ensureTenantAccess(tenantId, principal.tenantId());
    validateFile(file);

    SysFileEntity fileEntity = new SysFileEntity();
    long fileId = nextId(fileEntity);
    String fileName = resolveFileName(file.getOriginalFilename());
    String objectKey = buildObjectKey(bizType, tenantId, bizId, fileId, fileName);

    try (var inputStream = file.getInputStream()) {
      objectStorageService.putObject(
          minioProperties.getBucketName(),
          objectKey,
          inputStream,
          file.getSize(),
          normalizeContentType(file.getContentType())
      );
    } catch (IOException exception) {
      throw new BusinessException("FILE_UPLOAD_READ_FAILED", "failed to read upload file");
    }

    fileEntity.setId(fileId);
    fileEntity.setTenantId(tenantId);
    fileEntity.setBucketName(minioProperties.getBucketName());
    fileEntity.setObjectKey(objectKey);
    fileEntity.setFileName(fileName);
    fileEntity.setFileExt(extractFileExt(fileName));
    fileEntity.setContentType(normalizeContentType(file.getContentType()));
    fileEntity.setFileSize(file.getSize());
    fileEntity.setStorageType(STORAGE_TYPE_MINIO);
    fileEntity.setDeleted(0);
    fileEntity.setVersion(0L);
    fileEntity.setCreatedBy(principal.userId());
    fileEntity.setUpdatedBy(principal.userId());
    fileMapper.insert(fileEntity);

    SysFileRefEntity refEntity = new SysFileRefEntity();
    refEntity.setId(nextId(refEntity));
    refEntity.setTenantId(tenantId);
    refEntity.setFileId(fileId);
    refEntity.setBizType(normalizeBizType(bizType));
    refEntity.setBizId(bizId);
    refEntity.setCategory("default");
    refEntity.setSortNo(nextSortNo(tenantId, bizType, bizId));
    refEntity.setDeleted(0);
    refEntity.setVersion(0L);
    refEntity.setCreatedBy(principal.userId());
    refEntity.setUpdatedBy(principal.userId());
    fileRefMapper.insert(refEntity);

    return toAttachmentDto(new FileRefViewBuilder()
        .bizId(bizId)
        .fileId(fileId)
        .fileName(fileName)
        .contentType(fileEntity.getContentType())
        .fileSize(fileEntity.getFileSize())
        .objectKey(objectKey)
        .bucketName(fileEntity.getBucketName())
        .uploadedBy(principal.username())
        .uploadedAt(fileEntity.getCreatedAt())
        .build());
  }

  public List<FileAttachmentDto> listByBizId(Long tenantId, String bizType, Long bizId) {
    return listByBizIds(tenantId, bizType, List.of(bizId)).getOrDefault(bizId, List.of());
  }

  public Map<Long, List<FileAttachmentDto>> listByBizIds(Long tenantId, String bizType, Collection<Long> bizIds) {
    if (bizIds == null || bizIds.isEmpty()) {
      return Map.of();
    }

    return fileRefMapper.selectByBizIds(tenantId, normalizeBizType(bizType), List.copyOf(bizIds)).stream()
        .collect(Collectors.groupingBy(
            FileRefView::getBizId,
            Collectors.mapping(this::toAttachmentDto, Collectors.toList())
        ));
  }

  public void copyBindings(String bizType, Long tenantId, Long sourceBizId, Long targetBizId) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ensureTenantAccess(tenantId, principal.tenantId());

    List<SysFileRefEntity> sourceRefs = fileRefMapper.selectList(new LambdaQueryWrapper<SysFileRefEntity>()
        .eq(SysFileRefEntity::getTenantId, tenantId)
        .eq(SysFileRefEntity::getBizType, normalizeBizType(bizType))
        .eq(SysFileRefEntity::getBizId, sourceBizId)
        .eq(SysFileRefEntity::getDeleted, 0))
        .stream()
        .sorted(Comparator
            .comparing((SysFileRefEntity item) -> item.getSortNo() == null ? 0 : item.getSortNo())
            .thenComparing(SysFileRefEntity::getId))
        .toList();

    if (sourceRefs.isEmpty()) {
      return;
    }

    int nextSortNo = nextSortNo(tenantId, bizType, targetBizId);
    for (SysFileRefEntity sourceRef : sourceRefs) {
      SysFileRefEntity targetRef = new SysFileRefEntity();
      targetRef.setId(nextId(targetRef));
      targetRef.setTenantId(tenantId);
      targetRef.setFileId(sourceRef.getFileId());
      targetRef.setBizType(normalizeBizType(bizType));
      targetRef.setBizId(targetBizId);
      targetRef.setCategory(sourceRef.getCategory());
      targetRef.setSortNo(nextSortNo++);
      targetRef.setDeleted(0);
      targetRef.setVersion(0L);
      targetRef.setCreatedBy(principal.userId());
      targetRef.setUpdatedBy(principal.userId());
      fileRefMapper.insert(targetRef);
    }
  }

  public void delete(String bizType, Long tenantId, Long bizId, Long fileId) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    ensureTenantAccess(tenantId, principal.tenantId());

    SysFileRefEntity refEntity = fileRefMapper.selectOne(new LambdaQueryWrapper<SysFileRefEntity>()
        .eq(SysFileRefEntity::getTenantId, tenantId)
        .eq(SysFileRefEntity::getBizType, normalizeBizType(bizType))
        .eq(SysFileRefEntity::getBizId, bizId)
        .eq(SysFileRefEntity::getFileId, fileId));
    if (refEntity == null) {
      throw new BusinessException("FILE_REF_NOT_FOUND", "file binding not found: " + fileId);
    }

    fileRefMapper.update(null, new LambdaUpdateWrapper<SysFileRefEntity>()
        .eq(SysFileRefEntity::getId, refEntity.getId())
        .set(SysFileRefEntity::getDeleted, 1)
        .set(SysFileRefEntity::getUpdatedBy, principal.userId()));

    long remainingRefs = fileRefMapper.selectCount(new LambdaQueryWrapper<SysFileRefEntity>()
        .eq(SysFileRefEntity::getTenantId, tenantId)
        .eq(SysFileRefEntity::getFileId, fileId)
        .eq(SysFileRefEntity::getDeleted, 0));
    if (remainingRefs > 0) {
      return;
    }

    SysFileEntity fileEntity = fileMapper.selectById(fileId);
    if (fileEntity == null) {
      return;
    }

    objectStorageService.removeObject(fileEntity.getBucketName(), fileEntity.getObjectKey());
    fileMapper.update(null, new LambdaUpdateWrapper<SysFileEntity>()
        .eq(SysFileEntity::getId, fileId)
        .set(SysFileEntity::getDeleted, 1)
        .set(SysFileEntity::getUpdatedBy, principal.userId()));
  }

  private FileAttachmentDto toAttachmentDto(FileRefView view) {
    return new FileAttachmentDto(
        String.valueOf(view.getFileId()),
        view.getFileName(),
        objectStorageService.getPresignedGetUrl(
            view.getBucketName(),
            view.getObjectKey(),
            Duration.ofMinutes(minioProperties.getPresignedExpiryMinutes()),
            view.getFileName()
        ),
        view.getFileSize(),
        view.getContentType(),
        view.getUploadedBy(),
        view.getUploadedAt() == null ? null : view.getUploadedAt().toString()
    );
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("FILE_EMPTY", "upload file is empty");
    }
    long maxBytes = (long) minioProperties.getMaxFileSizeMb() * 1024 * 1024;
    if (file.getSize() > maxBytes) {
      throw new BusinessException("FILE_TOO_LARGE", "file exceeds max size: " + minioProperties.getMaxFileSizeMb() + "MB");
    }
  }

  private int nextSortNo(Long tenantId, String bizType, Long bizId) {
    return fileRefMapper.selectCount(new LambdaQueryWrapper<SysFileRefEntity>()
        .eq(SysFileRefEntity::getTenantId, tenantId)
        .eq(SysFileRefEntity::getBizType, normalizeBizType(bizType))
        .eq(SysFileRefEntity::getBizId, bizId)
        .eq(SysFileRefEntity::getDeleted, 0)).intValue();
  }

  private String buildObjectKey(String bizType, Long tenantId, Long bizId, Long fileId, String fileName) {
    String dateSegment = LocalDate.now().toString().replace("-", "");
    return "%s/%s/%s/%s/%s_%s".formatted(
        normalizeBizType(bizType).toLowerCase(Locale.ROOT),
        tenantId,
        bizId,
        dateSegment,
        fileId == null ? UUID.randomUUID() : fileId,
        sanitizeFileName(fileName)
    );
  }

  private String resolveFileName(String originalFileName) {
    String normalized = sanitizeFileName(originalFileName == null ? "file" : originalFileName.trim());
    return normalized.isBlank() ? "file" : normalized;
  }

  private String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
  }

  private String extractFileExt(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    return dotIndex < 0 ? null : fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
  }

  private String normalizeContentType(String contentType) {
    return contentType == null || contentType.isBlank()
        ? "application/octet-stream"
        : contentType;
  }

  private String normalizeBizType(String bizType) {
    return bizType == null ? "" : bizType.trim().toUpperCase(Locale.ROOT);
  }

  private void ensureTenantAccess(Long requestTenantId, Long currentTenantId) {
    if (!Objects.equals(requestTenantId, currentTenantId)) {
      throw new BusinessException("FILE_TENANT_MISMATCH", "file tenant mismatch");
    }
  }

  private long nextId(Object entity) {
    return ((Number) identifierGenerator.nextId(entity)).longValue();
  }

  private static final class FileRefViewBuilder {
    private final FileRefView view = new FileRefView();

    private FileRefViewBuilder bizId(Long value) {
      view.setBizId(value);
      return this;
    }

    private FileRefViewBuilder fileId(Long value) {
      view.setFileId(value);
      return this;
    }

    private FileRefViewBuilder fileName(String value) {
      view.setFileName(value);
      return this;
    }

    private FileRefViewBuilder contentType(String value) {
      view.setContentType(value);
      return this;
    }

    private FileRefViewBuilder fileSize(Long value) {
      view.setFileSize(value);
      return this;
    }

    private FileRefViewBuilder objectKey(String value) {
      view.setObjectKey(value);
      return this;
    }

    private FileRefViewBuilder bucketName(String value) {
      view.setBucketName(value);
      return this;
    }

    private FileRefViewBuilder uploadedBy(String value) {
      view.setUploadedBy(value);
      return this;
    }

    private FileRefViewBuilder uploadedAt(java.time.LocalDateTime value) {
      view.setUploadedAt(value);
      return this;
    }

    private FileRefView build() {
      return view;
    }
  }
}
