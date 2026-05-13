package com.iris.back.business.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("biz_ai_model_config")
public class AiModelConfigEntity extends BaseEntity {

  private String displayName;
  private String providerType;
  private String providerName;
  private String baseUrl;
  private String modelName;
  private String apiKeyCipher;
  private String status;
  private Integer defaultModel;
  private Integer timeoutSeconds;
  private Double temperature;
  private Integer maxTokens;

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public String getProviderName() {
    return providerName;
  }

  public void setProviderName(String providerName) {
    this.providerName = providerName;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getApiKeyCipher() {
    return apiKeyCipher;
  }

  public void setApiKeyCipher(String apiKeyCipher) {
    this.apiKeyCipher = apiKeyCipher;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getDefaultModel() {
    return defaultModel;
  }

  public void setDefaultModel(Integer defaultModel) {
    this.defaultModel = defaultModel;
  }

  public Integer getTimeoutSeconds() {
    return timeoutSeconds;
  }

  public void setTimeoutSeconds(Integer timeoutSeconds) {
    this.timeoutSeconds = timeoutSeconds;
  }

  public Double getTemperature() {
    return temperature;
  }

  public void setTemperature(Double temperature) {
    this.temperature = temperature;
  }

  public Integer getMaxTokens() {
    return maxTokens;
  }

  public void setMaxTokens(Integer maxTokens) {
    this.maxTokens = maxTokens;
  }
}
