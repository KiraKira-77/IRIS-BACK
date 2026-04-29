package com.iris.back.business.checklist.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;
import java.time.LocalDate;

@TableName("biz_control_checklist")
public class BizChecklistEntity extends BaseEntity {

  private String checklistCode;
  private String checklistName;
  private String description;

  @TableField("checklist_version")
  private String checklistVersion;

  private Long ownerScopeId;
  private String sharedScopeIds;
  private LocalDate uploadDate;
  private String status;

  public String getChecklistCode() {
    return checklistCode;
  }

  public void setChecklistCode(String checklistCode) {
    this.checklistCode = checklistCode;
  }

  public String getChecklistName() {
    return checklistName;
  }

  public void setChecklistName(String checklistName) {
    this.checklistName = checklistName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getChecklistVersion() {
    return checklistVersion;
  }

  public void setChecklistVersion(String checklistVersion) {
    this.checklistVersion = checklistVersion;
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

  public LocalDate getUploadDate() {
    return uploadDate;
  }

  public void setUploadDate(LocalDate uploadDate) {
    this.uploadDate = uploadDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
