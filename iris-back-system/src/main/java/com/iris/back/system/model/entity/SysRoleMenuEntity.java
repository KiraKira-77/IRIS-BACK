package com.iris.back.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("sys_role_menu")
public class SysRoleMenuEntity extends BaseEntity {
  private Long roleId;
  private String menuCode;

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
  }

  public String getMenuCode() {
    return menuCode;
  }

  public void setMenuCode(String menuCode) {
    this.menuCode = menuCode;
  }
}
