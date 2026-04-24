package com.iris.back.system.service.storage;

import com.iris.back.common.exception.BusinessException;
import java.io.InputStream;
import java.time.Duration;

public class DisabledObjectStorageService implements ObjectStorageService {

  @Override
  public void putObject(String bucketName, String objectKey, InputStream inputStream, long size, String contentType) {
    throw new BusinessException("FILE_STORAGE_DISABLED", "object storage is not enabled");
  }

  @Override
  public void removeObject(String bucketName, String objectKey) {
    throw new BusinessException("FILE_STORAGE_DISABLED", "object storage is not enabled");
  }

  @Override
  public String getPresignedGetUrl(String bucketName, String objectKey, Duration expiry, String fileName) {
    throw new BusinessException("FILE_STORAGE_DISABLED", "object storage is not enabled");
  }
}
