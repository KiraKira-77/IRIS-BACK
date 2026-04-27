package com.iris.back.business.project.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;
import java.time.LocalDateTime;

@TableName("biz_project_task")
public class BizProjectTaskEntity extends BaseEntity {

  private Long projectId;
  private Long checklistId;
  private String checklistName;
  private Long checklistItemId;
  private String checkContent;
  private String checkCriterion;
  private String controlFrequency;
  private String evaluationType;
  private String taskName;
  private String taskDescription;
  private Long assigneeId;
  private String assigneeName;
  private Long contactId;
  private String contactName;
  private String status;
  private LocalDateTime issuedAt;
  private LocalDateTime completedAt;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getChecklistId() {
    return checklistId;
  }

  public void setChecklistId(Long checklistId) {
    this.checklistId = checklistId;
  }

  public String getChecklistName() {
    return checklistName;
  }

  public void setChecklistName(String checklistName) {
    this.checklistName = checklistName;
  }

  public Long getChecklistItemId() {
    return checklistItemId;
  }

  public void setChecklistItemId(Long checklistItemId) {
    this.checklistItemId = checklistItemId;
  }

  public String getCheckContent() {
    return checkContent;
  }

  public void setCheckContent(String checkContent) {
    this.checkContent = checkContent;
  }

  public String getCheckCriterion() {
    return checkCriterion;
  }

  public void setCheckCriterion(String checkCriterion) {
    this.checkCriterion = checkCriterion;
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

  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public String getTaskDescription() {
    return taskDescription;
  }

  public void setTaskDescription(String taskDescription) {
    this.taskDescription = taskDescription;
  }

  public Long getAssigneeId() {
    return assigneeId;
  }

  public void setAssigneeId(Long assigneeId) {
    this.assigneeId = assigneeId;
  }

  public String getAssigneeName() {
    return assigneeName;
  }

  public void setAssigneeName(String assigneeName) {
    this.assigneeName = assigneeName;
  }

  public Long getContactId() {
    return contactId;
  }

  public void setContactId(Long contactId) {
    this.contactId = contactId;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(LocalDateTime issuedAt) {
    this.issuedAt = issuedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }
}
