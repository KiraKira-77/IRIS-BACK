package com.iris.back.business.project.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;
import java.time.LocalDateTime;

@TableName("biz_project_rectification")
public class BizProjectRectificationEntity extends BaseEntity {

  private String rectificationCode;
  private String title;
  private String description;
  private String taskName;
  private String taskDescription;
  private Long projectId;
  private String projectName;
  private Long taskId;
  private Long checklistItemId;
  private String checkContent;
  private Long sourceWorkOrderRecordId;
  private String omsWorkOrderId;
  private Long assigneeId;
  private String assigneeName;
  private Long contactId;
  private String contactName;
  private LocalDateTime issuedAt;
  private LocalDateTime deadline;
  private String status;

  public String getRectificationCode() {
    return rectificationCode;
  }

  public void setRectificationCode(String rectificationCode) {
    this.rectificationCode = rectificationCode;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
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

  public Long getSourceWorkOrderRecordId() {
    return sourceWorkOrderRecordId;
  }

  public void setSourceWorkOrderRecordId(Long sourceWorkOrderRecordId) {
    this.sourceWorkOrderRecordId = sourceWorkOrderRecordId;
  }

  public String getOmsWorkOrderId() {
    return omsWorkOrderId;
  }

  public void setOmsWorkOrderId(String omsWorkOrderId) {
    this.omsWorkOrderId = omsWorkOrderId;
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

  public LocalDateTime getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(LocalDateTime issuedAt) {
    this.issuedAt = issuedAt;
  }

  public LocalDateTime getDeadline() {
    return deadline;
  }

  public void setDeadline(LocalDateTime deadline) {
    this.deadline = deadline;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
