package com.iris.back.system.service.storage;

import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorageService {

  void putObject(String bucketName, String objectKey, InputStream inputStream, long size, String contentType);

  void removeObject(String bucketName, String objectKey);

  String getPresignedGetUrl(String bucketName, String objectKey, Duration expiry, String fileName);
}
