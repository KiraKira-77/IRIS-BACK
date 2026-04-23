package com.iris.back.business.standard.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;
import java.time.LocalDate;

@TableName("biz_standard")
public class BizStandardEntity extends BaseEntity {

  private String standardGroupId;
  private String title;
  private String category;

  @TableField("standard_version")
  private String standardVersion;

  private Integer versionNumber;
  private Long previousVersionId;
  private LocalDate publishDate;
  private String status;
  private String description;
  private String tags;
  private String visibilityLevel;
  private Long ownerScopeId;
  private String sharedScopeIds;
  private String changeLog;

  public String getStandardGroupId() {
    return standardGroupId;
  }

  public void setStandardGroupId(String standardGroupId) {
    this.standardGroupId = standardGroupId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getStandardVersion() {
    return standardVersion;
  }

  public void setStandardVersion(String standardVersion) {
    this.standardVersion = standardVersion;
  }

  public Integer getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(Integer versionNumber) {
    this.versionNumber = versionNumber;
  }

  public Long getPreviousVersionId() {
    return previousVersionId;
  }

  public void setPreviousVersionId(Long previousVersionId) {
    this.previousVersionId = previousVersionId;
  }

  public LocalDate getPublishDate() {
    return publishDate;
  }

  public void setPublishDate(LocalDate publishDate) {
    this.publishDate = publishDate;
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

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public String getVisibilityLevel() {
    return visibilityLevel;
  }

  public void setVisibilityLevel(String visibilityLevel) {
    this.visibilityLevel = visibilityLevel;
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

  public String getChangeLog() {
    return changeLog;
  }

  public void setChangeLog(String changeLog) {
    this.changeLog = changeLog;
  }
}
