package com.iris.back.business.project.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;
import java.time.LocalDateTime;

@TableName("biz_project_archive")
public class BizProjectArchiveEntity extends BaseEntity {

  private Long projectId;
  private String projectCode;
  private String projectName;
  private LocalDateTime archiveDate;
  private Long archivedBy;
  private String archivedByName;
  private String status;
  private Integer taskCount;
  private Integer workOrderCount;
  private Integer rectificationCount;
  private Integer documentCount;
  private String snapshotVersion;
  private String snapshotJson;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

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

  public LocalDateTime getArchiveDate() {
    return archiveDate;
  }

  public void setArchiveDate(LocalDateTime archiveDate) {
    this.archiveDate = archiveDate;
  }

  public Long getArchivedBy() {
    return archivedBy;
  }

  public void setArchivedBy(Long archivedBy) {
    this.archivedBy = archivedBy;
  }

  public String getArchivedByName() {
    return archivedByName;
  }

  public void setArchivedByName(String archivedByName) {
    this.archivedByName = archivedByName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getTaskCount() {
    return taskCount;
  }

  public void setTaskCount(Integer taskCount) {
    this.taskCount = taskCount;
  }

  public Integer getWorkOrderCount() {
    return workOrderCount;
  }

  public void setWorkOrderCount(Integer workOrderCount) {
    this.workOrderCount = workOrderCount;
  }

  public Integer getRectificationCount() {
    return rectificationCount;
  }

  public void setRectificationCount(Integer rectificationCount) {
    this.rectificationCount = rectificationCount;
  }

  public Integer getDocumentCount() {
    return documentCount;
  }

  public void setDocumentCount(Integer documentCount) {
    this.documentCount = documentCount;
  }

  public String getSnapshotVersion() {
    return snapshotVersion;
  }

  public void setSnapshotVersion(String snapshotVersion) {
    this.snapshotVersion = snapshotVersion;
  }

  public String getSnapshotJson() {
    return snapshotJson;
  }

  public void setSnapshotJson(String snapshotJson) {
    this.snapshotJson = snapshotJson;
  }
}
