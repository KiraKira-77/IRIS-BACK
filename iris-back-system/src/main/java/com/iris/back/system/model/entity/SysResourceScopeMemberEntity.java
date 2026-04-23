package com.iris.back.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("sys_resource_scope_member")
public class SysResourceScopeMemberEntity extends BaseEntity {
  private Long scopeId;
  private Long userId;
  private Integer canView;
  private Integer canCreate;
  private Integer canEdit;
  private Integer canDelete;
  private Integer canManage;

  public Long getScopeId() {
    return scopeId;
  }

  public void setScopeId(Long scopeId) {
    this.scopeId = scopeId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Integer getCanView() {
    return canView;
  }

  public void setCanView(Integer canView) {
    this.canView = canView;
  }

  public Integer getCanCreate() {
    return canCreate;
  }

  public void setCanCreate(Integer canCreate) {
    this.canCreate = canCreate;
  }

  public Integer getCanEdit() {
    return canEdit;
  }

  public void setCanEdit(Integer canEdit) {
    this.canEdit = canEdit;
  }

  public Integer getCanDelete() {
    return canDelete;
  }

  public void setCanDelete(Integer canDelete) {
    this.canDelete = canDelete;
  }

  public Integer getCanManage() {
    return canManage;
  }

  public void setCanManage(Integer canManage) {
    this.canManage = canManage;
  }
}
