package com.iris.back.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.system.config.MinioProperties;
import com.iris.back.system.mapper.SysFileMapper;
import com.iris.back.system.mapper.SysFileRefMapper;
import com.iris.back.system.model.dto.FileRefView;
import com.iris.back.system.service.FileService;
import com.iris.back.system.service.storage.ObjectStorageService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTests {

  @Mock
  private SysFileMapper fileMapper;

  @Mock
  private SysFileRefMapper fileRefMapper;

  @Mock
  private ObjectStorageService objectStorageService;

  @Mock
  private CurrentUserContext currentUserContext;

  @Mock
  private IdentifierGenerator identifierGenerator;

  private FileService fileService;

  @BeforeEach
  void setUp() {
    MinioProperties minioProperties = new MinioProperties();
    minioProperties.setPresignedExpiryMinutes(30);
    fileService = new FileService(
        fileMapper,
        fileRefMapper,
        objectStorageService,
        currentUserContext,
        identifierGenerator,
        minioProperties
    );
  }

  @Test
  void formatsUploadedAtWithoutIsoSeparator() {
    FileRefView view = new FileRefView();
    view.setBizId(9901L);
    view.setFileId(7001L);
    view.setFileName("evidence.pdf");
    view.setContentType("application/pdf");
    view.setFileSize(1024L);
    view.setBucketName("iris");
    view.setObjectKey("standard/1001/9901/evidence.pdf");
    view.setUploadedBy("Finance Manager");
    view.setUploadedAt(LocalDateTime.of(2026, 4, 29, 13, 6, 50));
    when(fileRefMapper.selectByBizIds(1001L, "STANDARD", List.of(9901L))).thenReturn(List.of(view));
    when(objectStorageService.getPresignedGetUrl(
        any(),
        any(),
        any(Duration.class),
        any()
    )).thenReturn("http://minio/evidence.pdf");

    var result = fileService.listByBizId(1001L, "STANDARD", 9901L);

    assertThat(result).singleElement().satisfies(item ->
        assertThat(item.uploadedAt()).isEqualTo("2026-04-29 13:06:50"));
  }
}
