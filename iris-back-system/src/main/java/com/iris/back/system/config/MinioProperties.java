package com.iris.back.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iris.minio")
public class MinioProperties {

  private boolean enabled;
  private String endpoint;
  private Integer port;
  private String accessKey;
  private String secretKey;
  private boolean secure;
  private String bucketName;
  private String externalHttp;
  private String externalIp;
  private Integer externalPort;
  private Integer presignedExpiryMinutes = 30;
  private Integer maxFileSizeMb = 20;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public Integer getPort() {
    return port;
  }

  public void setPort(Integer port) {
    this.port = port;
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public boolean isSecure() {
    return secure;
  }

  public void setSecure(boolean secure) {
    this.secure = secure;
  }

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
  }

  public String getExternalHttp() {
    return externalHttp;
  }

  public void setExternalHttp(String externalHttp) {
    this.externalHttp = externalHttp;
  }

  public String getExternalIp() {
    return externalIp;
  }

  public void setExternalIp(String externalIp) {
    this.externalIp = externalIp;
  }

  public Integer getExternalPort() {
    return externalPort;
  }

  public void setExternalPort(Integer externalPort) {
    this.externalPort = externalPort;
  }

  public Integer getPresignedExpiryMinutes() {
    return presignedExpiryMinutes;
  }

  public void setPresignedExpiryMinutes(Integer presignedExpiryMinutes) {
    this.presignedExpiryMinutes = presignedExpiryMinutes;
  }

  public Integer getMaxFileSizeMb() {
    return maxFileSizeMb;
  }

  public void setMaxFileSizeMb(Integer maxFileSizeMb) {
    this.maxFileSizeMb = maxFileSizeMb;
  }
}
