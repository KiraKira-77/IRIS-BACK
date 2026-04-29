package com.iris.back.business.checklist.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("biz_control_checklist_item")
public class BizChecklistItemEntity extends BaseEntity {

  private Long checklistId;
  private Integer sequenceNo;
  private String content;
  private String criterion;
  private String controlFrequency;
  private String evaluationType;
  private String organizationIds;

  public Long getChecklistId() {
    return checklistId;
  }

  public void setChecklistId(Long checklistId) {
    this.checklistId = checklistId;
  }

  public Integer getSequenceNo() {
    return sequenceNo;
  }

  public void setSequenceNo(Integer sequenceNo) {
    this.sequenceNo = sequenceNo;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getCriterion() {
    return criterion;
  }

  public void setCriterion(String criterion) {
    this.criterion = criterion;
  }

  public String getControlFrequency() {
    return controlFrequency;
  }

  public void setControlFrequency(String controlFrequency) {
    this.controlFrequency = controlFrequency;
  }

  public String getEvaluationType() {
    return evaluationType;
  }

  public void setEvaluationType(String evaluationType) {
    this.evaluationType = evaluationType;
  }

  public String getOrganizationIds() {
    return organizationIds;
  }

  public void setOrganizationIds(String organizationIds) {
    this.organizationIds = organizationIds;
  }
}
