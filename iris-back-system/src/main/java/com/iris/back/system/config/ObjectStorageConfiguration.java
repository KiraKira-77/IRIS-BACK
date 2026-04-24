package com.iris.back.system.config;

import com.iris.back.system.service.storage.DisabledObjectStorageService;
import com.iris.back.system.service.storage.MinioObjectStorageService;
import com.iris.back.system.service.storage.ObjectStorageService;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class ObjectStorageConfiguration {

  @Bean("minioInternalClient")
  @ConditionalOnProperty(prefix = "iris.minio", name = "enabled", havingValue = "true")
  public MinioClient minioInternalClient(MinioProperties properties) {
    return MinioClient.builder()
        .endpoint(properties.getEndpoint(), properties.getPort(), properties.isSecure())
        .credentials(properties.getAccessKey(), properties.getSecretKey())
        .build();
  }

  @Bean("minioPublicClient")
  @ConditionalOnProperty(prefix = "iris.minio", name = "enabled", havingValue = "true")
  public MinioClient minioPublicClient(MinioProperties properties) {
    String endpoint = properties.getExternalIp() == null || properties.getExternalIp().isBlank()
        ? properties.getEndpoint()
        : properties.getExternalIp();
    int port = properties.getExternalPort() == null ? properties.getPort() : properties.getExternalPort();
    boolean secure = "https".equalsIgnoreCase(properties.getExternalHttp()) || properties.isSecure();

    return MinioClient.builder()
        .endpoint(endpoint, port, secure)
        .credentials(properties.getAccessKey(), properties.getSecretKey())
        .build();
  }

  @Bean
  @ConditionalOnProperty(prefix = "iris.minio", name = "enabled", havingValue = "true")
  public ObjectStorageService minioObjectStorageService(
      MinioProperties properties,
      MinioClient minioInternalClient,
      MinioClient minioPublicClient
  ) {
    return new MinioObjectStorageService(properties, minioInternalClient, minioPublicClient);
  }

  @Bean
  @ConditionalOnMissingBean(ObjectStorageService.class)
  public ObjectStorageService disabledObjectStorageService() {
    return new DisabledObjectStorageService();
  }
}
