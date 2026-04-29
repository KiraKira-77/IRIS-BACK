package com.iris.back.business.plan.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;
import java.time.LocalDate;

@TableName("biz_control_plan_item")
public class BizPlanItemEntity extends BaseEntity {

  private Long planId;
  private Integer sequenceNo;
  private String targetScope;
  private String checklistIds;
  private LocalDate plannedStartDate;
  private LocalDate plannedEndDate;
  private String assignee;
  private String projectId;

  public Long getPlanId() {
    return planId;
  }

  public void setPlanId(Long planId) {
    this.planId = planId;
  }

  public Integer getSequenceNo() {
    return sequenceNo;
  }

  public void setSequenceNo(Integer sequenceNo) {
    this.sequenceNo = sequenceNo;
  }

  public String getTargetScope() {
    return targetScope;
  }

  public void setTargetScope(String targetScope) {
    this.targetScope = targetScope;
  }

  public String getChecklistIds() {
    return checklistIds;
  }

  public void setChecklistIds(String checklistIds) {
    this.checklistIds = checklistIds;
  }

  public LocalDate getPlannedStartDate() {
    return plannedStartDate;
  }

  public void setPlannedStartDate(LocalDate plannedStartDate) {
    this.plannedStartDate = plannedStartDate;
  }

  public LocalDate getPlannedEndDate() {
    return plannedEndDate;
  }

  public void setPlannedEndDate(LocalDate plannedEndDate) {
    this.plannedEndDate = plannedEndDate;
  }

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }
}
