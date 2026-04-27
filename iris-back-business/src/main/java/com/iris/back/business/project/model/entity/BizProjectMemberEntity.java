package com.iris.back.business.project.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("biz_project_member")
public class BizProjectMemberEntity extends BaseEntity {

  private Long projectId;
  private Long personnelId;
  private String personnelName;
  private String employeeNo;
  private String department;
  private String role;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getPersonnelId() {
    return personnelId;
  }

  public void setPersonnelId(Long personnelId) {
    this.personnelId = personnelId;
  }

  public String getPersonnelName() {
    return personnelName;
  }

  public void setPersonnelName(String personnelName) {
    this.personnelName = personnelName;
  }

  public String getEmployeeNo() {
    return employeeNo;
  }

  public void setEmployeeNo(String employeeNo) {
    this.employeeNo = employeeNo;
  }

  public String getDepartment() {
    return department;
  }

  public void setDepartment(String department) {
    this.department = department;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
