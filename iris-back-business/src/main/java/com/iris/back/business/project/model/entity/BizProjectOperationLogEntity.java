package com.iris.back.business.project.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("biz_project_operation_log")
public class BizProjectOperationLogEntity extends BaseEntity {

  private Long projectId;
  private Long taskId;
  private Long workOrderId;
  private String action;
  private Long operatorId;
  private String operatorName;

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

  public Long getWorkOrderId() {
    return workOrderId;
  }

  public void setWorkOrderId(Long workOrderId) {
    this.workOrderId = workOrderId;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public Long getOperatorId() {
    return operatorId;
  }

  public void setOperatorId(Long operatorId) {
    this.operatorId = operatorId;
  }

  public String getOperatorName() {
    return operatorName;
  }

  public void setOperatorName(String operatorName) {
    this.operatorName = operatorName;
  }
}
