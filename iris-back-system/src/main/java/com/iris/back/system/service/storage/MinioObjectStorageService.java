package com.iris.back.system.service.storage;

import com.iris.back.common.exception.BusinessException;
import com.iris.back.system.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import java.io.InputStream;
import java.time.Duration;

public class MinioObjectStorageService implements ObjectStorageService {

  private final MinioProperties properties;
  private final MinioClient internalClient;
  private final MinioClient publicClient;

  public MinioObjectStorageService(
      MinioProperties properties,
      MinioClient internalClient,
      MinioClient publicClient
  ) {
    this.properties = properties;
    this.internalClient = internalClient;
    this.publicClient = publicClient;
  }

  @Override
  public void putObject(String bucketName, String objectKey, InputStream inputStream, long size, String contentType) {
    try {
      ensureBucket(bucketName);
      internalClient.putObject(PutObjectArgs.builder()
          .bucket(bucketName)
          .object(objectKey)
          .stream(inputStream, size, -1)
          .contentType(contentType)
          .build());
    } catch (Exception exception) {
      throw new BusinessException("FILE_UPLOAD_FAILED", "failed to upload file to MinIO");
    }
  }

  @Override
  public void removeObject(String bucketName, String objectKey) {
    try {
      internalClient.removeObject(RemoveObjectArgs.builder()
          .bucket(bucketName)
          .object(objectKey)
          .build());
    } catch (Exception exception) {
      throw new BusinessException("FILE_DELETE_FAILED", "failed to delete file from MinIO");
    }
  }

  @Override
  public String getPresignedGetUrl(String bucketName, String objectKey, Duration expiry, String fileName) {
    try {
      int expirySeconds = Math.max(60, (int) expiry.getSeconds());
      return publicClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
          .method(Method.GET)
          .bucket(bucketName)
          .object(objectKey)
          .expiry(expirySeconds)
          .extraQueryParams(java.util.Map.of(
              "response-content-disposition",
              "attachment; filename=\"" + fileName + "\""
          ))
          .build());
    } catch (Exception exception) {
      throw new BusinessException("FILE_URL_GENERATE_FAILED", "failed to generate file download url");
    }
  }

  private void ensureBucket(String bucketName) throws Exception {
    boolean exists = internalClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
    if (!exists) {
      internalClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
    }
  }
}
