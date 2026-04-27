package com.iris.back.business.project.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;
import java.time.LocalDateTime;

@TableName("biz_project_task_work_order")
public class BizProjectTaskWorkOrderEntity extends BaseEntity {

  private Long projectId;
  private Long taskId;
  private String omsWorkOrderId;
  private String idempotencyKey;
  private Long handlerId;
  private String handlerName;
  private String workOrderTitle;
  private String workOrderDescription;
  private LocalDateTime issuedAt;
  private LocalDateTime completedAt;
  private String omsStatus;
  private String omsStatusName;
  private String omsResultSummary;
  private String omsDetailPayload;
  private String omsLogPayload;
  private String omsAttachmentPayload;
  private String syncStatus;
  private LocalDateTime lastSyncedAt;
  private String syncError;
  private String requestPayload;
  private String responsePayload;
  private String irisReviewStatus;
  private String irisReviewOpinion;
  private LocalDateTime irisReviewedAt;
  private Long irisReviewedBy;
  private Long rectificationId;
  private Integer reviewLocked;
  private String archiveBatchId;
  private String detailSnapshotJson;
  private String logSnapshotJson;
  private String attachmentSnapshotJson;
  private String snapshotVersion;
  private LocalDateTime snapshottedAt;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getTaskId() {
    return taskId;
  }

  public void setTaskId(Long taskId) {
    this.taskId = taskId;
  }

  public String getOmsWorkOrderId() {
    return omsWorkOrderId;
  }

  public void setOmsWorkOrderId(String omsWorkOrderId) {
    this.omsWorkOrderId = omsWorkOrderId;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public Long getHandlerId() {
    return handlerId;
  }

  public void setHandlerId(Long handlerId) {
    this.handlerId = handlerId;
  }

  public String getHandlerName() {
    return handlerName;
  }

  public void setHandlerName(String handlerName) {
    this.handlerName = handlerName;
  }

  public String getWorkOrderTitle() {
    return workOrderTitle;
  }

  public void setWorkOrderTitle(String workOrderTitle) {
    this.workOrderTitle = workOrderTitle;
  }

  public String getWorkOrderDescription() {
    return workOrderDescription;
  }

  public void setWorkOrderDescription(String workOrderDescription) {
    this.workOrderDescription = workOrderDescription;
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

  public String getOmsStatus() {
    return omsStatus;
  }

  public void setOmsStatus(String omsStatus) {
    this.omsStatus = omsStatus;
  }

  public String getOmsStatusName() {
    return omsStatusName;
  }

  public void setOmsStatusName(String omsStatusName) {
    this.omsStatusName = omsStatusName;
  }

  public String getOmsResultSummary() {
    return omsResultSummary;
  }

  public void setOmsResultSummary(String omsResultSummary) {
    this.omsResultSummary = omsResultSummary;
  }

  public String getOmsDetailPayload() {
    return omsDetailPayload;
  }

  public void setOmsDetailPayload(String omsDetailPayload) {
    this.omsDetailPayload = omsDetailPayload;
  }

  public String getOmsLogPayload() {
    return omsLogPayload;
  }

  public void setOmsLogPayload(String omsLogPayload) {
    this.omsLogPayload = omsLogPayload;
  }

  public String getOmsAttachmentPayload() {
    return omsAttachmentPayload;
  }

  public void setOmsAttachmentPayload(String omsAttachmentPayload) {
    this.omsAttachmentPayload = omsAttachmentPayload;
  }

  public String getSyncStatus() {
    return syncStatus;
  }

  public void setSyncStatus(String syncStatus) {
    this.syncStatus = syncStatus;
  }

  public LocalDateTime getLastSyncedAt() {
    return lastSyncedAt;
  }

  public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
    this.lastSyncedAt = lastSyncedAt;
  }

  public String getSyncError() {
    return syncError;
  }

  public void setSyncError(String syncError) {
    this.syncError = syncError;
  }

  public String getRequestPayload() {
    return requestPayload;
  }

  public void setRequestPayload(String requestPayload) {
    this.requestPayload = requestPayload;
  }

  public String getResponsePayload() {
    return responsePayload;
  }

  public void setResponsePayload(String responsePayload) {
    this.responsePayload = responsePayload;
  }

  public String getIrisReviewStatus() {
    return irisReviewStatus;
  }

  public void setIrisReviewStatus(String irisReviewStatus) {
    this.irisReviewStatus = irisReviewStatus;
  }

  public String getIrisReviewOpinion() {
    return irisReviewOpinion;
  }

  public void setIrisReviewOpinion(String irisReviewOpinion) {
    this.irisReviewOpinion = irisReviewOpinion;
  }

  public LocalDateTime getIrisReviewedAt() {
    return irisReviewedAt;
  }

  public void setIrisReviewedAt(LocalDateTime irisReviewedAt) {
    this.irisReviewedAt = irisReviewedAt;
  }

  public Long getIrisReviewedBy() {
    return irisReviewedBy;
  }

  public void setIrisReviewedBy(Long irisReviewedBy) {
    this.irisReviewedBy = irisReviewedBy;
  }

  public Long getRectificationId() {
    return rectificationId;
  }

  public void setRectificationId(Long rectificationId) {
    this.rectificationId = rectificationId;
  }

  public Integer getReviewLocked() {
    return reviewLocked;
  }

  public void setReviewLocked(Integer reviewLocked) {
    this.reviewLocked = reviewLocked;
  }

  public String getArchiveBatchId() {
    return archiveBatchId;
  }

  public void setArchiveBatchId(String archiveBatchId) {
    this.archiveBatchId = archiveBatchId;
  }

  public String getDetailSnapshotJson() {
    return detailSnapshotJson;
  }

  public void setDetailSnapshotJson(String detailSnapshotJson) {
    this.detailSnapshotJson = detailSnapshotJson;
  }

  public String getLogSnapshotJson() {
    return logSnapshotJson;
  }

  public void setLogSnapshotJson(String logSnapshotJson) {
    this.logSnapshotJson = logSnapshotJson;
  }

  public String getAttachmentSnapshotJson() {
    return attachmentSnapshotJson;
  }

  public void setAttachmentSnapshotJson(String attachmentSnapshotJson) {
    this.attachmentSnapshotJson = attachmentSnapshotJson;
  }

  public String getSnapshotVersion() {
    return snapshotVersion;
  }

  public void setSnapshotVersion(String snapshotVersion) {
    this.snapshotVersion = snapshotVersion;
  }

  public LocalDateTime getSnapshottedAt() {
    return snapshottedAt;
  }

  public void setSnapshottedAt(LocalDateTime snapshottedAt) {
    this.snapshottedAt = snapshottedAt;
  }
}
