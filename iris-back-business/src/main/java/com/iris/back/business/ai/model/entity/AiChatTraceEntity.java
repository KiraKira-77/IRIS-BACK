package com.iris.back.business.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("biz_ai_chat_trace")
public class AiChatTraceEntity extends BaseEntity {

  private String traceId;
  private String sessionId;
  private Long userId;
  private String username;
  private String routePath;
  private String entityType;
  private String entityId;
  private String question;
  private String answer;
  private String status;
  private Long modelConfigId;
  private String providerType;
  private String modelName;
  private String toolNamesJson;
  private String citationsJson;
  private Long latencyMs;
  private String errorMessage;

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRoutePath() {
    return routePath;
  }

  public void setRoutePath(String routePath) {
    this.routePath = routePath;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getModelConfigId() {
    return modelConfigId;
  }

  public void setModelConfigId(Long modelConfigId) {
    this.modelConfigId = modelConfigId;
  }

  public String getProviderType() {
    return providerType;
  }

  public void setProviderType(String providerType) {
    this.providerType = providerType;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getToolNamesJson() {
    return toolNamesJson;
  }

  public void setToolNamesJson(String toolNamesJson) {
    this.toolNamesJson = toolNamesJson;
  }

  public String getCitationsJson() {
    return citationsJson;
  }

  public void setCitationsJson(String citationsJson) {
    this.citationsJson = citationsJson;
  }

  public Long getLatencyMs() {
    return latencyMs;
  }

  public void setLatencyMs(Long latencyMs) {
    this.latencyMs = latencyMs;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
