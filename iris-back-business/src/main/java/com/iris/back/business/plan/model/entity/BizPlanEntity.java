package com.iris.back.business.plan.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("biz_control_plan")
public class BizPlanEntity extends BaseEntity {

  private String planCode;
  private String planName;
  private String cycle;
  private Integer planYear;
  private String period;
  private String status;
  private String description;
  private Long ownerScopeId;
  private String sharedScopeIds;
  private Long parentId;
  private Long approvedBy;

  public String getPlanCode() {
    return planCode;
  }

  public void setPlanCode(String planCode) {
    this.planCode = planCode;
  }

  public String getPlanName() {
    return planName;
  }

  public void setPlanName(String planName) {
    this.planName = planName;
  }

  public String getCycle() {
    return cycle;
  }

  public void setCycle(String cycle) {
    this.cycle = cycle;
  }

  public Integer getPlanYear() {
    return planYear;
  }

  public void setPlanYear(Integer planYear) {
    this.planYear = planYear;
  }

  public String getPeriod() {
    return period;
  }

  public void setPeriod(String period) {
    this.period = period;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getOwnerScopeId() {
    return ownerScopeId;
  }

  public void setOwnerScopeId(Long ownerScopeId) {
    this.ownerScopeId = ownerScopeId;
  }

  public String getSharedScopeIds() {
    return sharedScopeIds;
  }

  public void setSharedScopeIds(String sharedScopeIds) {
    this.sharedScopeIds = sharedScopeIds;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Long getApprovedBy() {
    return approvedBy;
  }

  public void setApprovedBy(Long approvedBy) {
    this.approvedBy = approvedBy;
  }
}
