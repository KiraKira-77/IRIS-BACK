package com.iris.back.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("sys_resource_scope")
public class SysResourceScopeEntity extends BaseEntity {
  private String scopeCode;
  private String scopeName;
  private String scopeType;
  private Integer status;

  public String getScopeCode() {
    return scopeCode;
  }

  public void setScopeCode(String scopeCode) {
    this.scopeCode = scopeCode;
  }

  public String getScopeName() {
    return scopeName;
  }

  public void setScopeName(String scopeName) {
    this.scopeName = scopeName;
  }

  public String getScopeType() {
    return scopeType;
  }

  public void setScopeType(String scopeType) {
    this.scopeType = scopeType;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }
}
