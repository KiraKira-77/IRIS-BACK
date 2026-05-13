package com.iris.back.business.ai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("biz_ai_chat_trace_event")
public class AiChatTraceEventEntity extends BaseEntity {

  private String traceId;
  private Integer sequenceNo;
  private String eventType;
  private String eventName;
  private String status;
  private String detailJson;
  private Long elapsedMs;

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public Integer getSequenceNo() {
    return sequenceNo;
  }

  public void setSequenceNo(Integer sequenceNo) {
    this.sequenceNo = sequenceNo;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDetailJson() {
    return detailJson;
  }

  public void setDetailJson(String detailJson) {
    this.detailJson = detailJson;
  }

  public Long getElapsedMs() {
    return elapsedMs;
  }

  public void setElapsedMs(Long elapsedMs) {
    this.elapsedMs = elapsedMs;
  }
}
