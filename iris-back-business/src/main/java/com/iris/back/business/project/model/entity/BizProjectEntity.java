package com.iris.back.business.project.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("biz_project")
public class BizProjectEntity extends BaseEntity {

  private String projectCode;
  private String projectName;
  private String source;
  private Long planId;
  private String planName;
  private String description;
  private LocalDate startDate;
  private LocalDate endDate;
  private String status;
  private String tagIds;
  private String tagNames;
  private Long leaderId;
  private String leaderName;
  private String checklistIds;
  private String archiveStatus;
  private LocalDateTime archiveStartedAt;
  private LocalDateTime archiveCompletedAt;
  private String archiveError;

  public String getProjectCode() {
    return projectCode;
  }

  public void setProjectCode(String projectCode) {
    this.projectCode = projectCode;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Long getPlanId() {
    return planId;
  }

  public void setPlanId(Long planId) {
    this.planId = planId;
  }

  public String getPlanName() {
    return planName;
  }

  public void setPlanName(String planName) {
    this.planName = planName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getTagIds() {
    return tagIds;
  }

  public void setTagIds(String tagIds) {
    this.tagIds = tagIds;
  }

  public String getTagNames() {
    return tagNames;
  }

  public void setTagNames(String tagNames) {
    this.tagNames = tagNames;
  }

  public Long getLeaderId() {
    return leaderId;
  }

  public void setLeaderId(Long leaderId) {
    this.leaderId = leaderId;
  }

  public String getLeaderName() {
    return leaderName;
  }

  public void setLeaderName(String leaderName) {
    this.leaderName = leaderName;
  }

  public String getChecklistIds() {
    return checklistIds;
  }

  public void setChecklistIds(String checklistIds) {
    this.checklistIds = checklistIds;
  }

  public String getArchiveStatus() {
    return archiveStatus;
  }

  public void setArchiveStatus(String archiveStatus) {
    this.archiveStatus = archiveStatus;
  }

  public LocalDateTime getArchiveStartedAt() {
    return archiveStartedAt;
  }

  public void setArchiveStartedAt(LocalDateTime archiveStartedAt) {
    this.archiveStartedAt = archiveStartedAt;
  }

  public LocalDateTime getArchiveCompletedAt() {
    return archiveCompletedAt;
  }

  public void setArchiveCompletedAt(LocalDateTime archiveCompletedAt) {
    this.archiveCompletedAt = archiveCompletedAt;
  }

  public String getArchiveError() {
    return archiveError;
  }

  public void setArchiveError(String archiveError) {
    this.archiveError = archiveError;
  }
}
