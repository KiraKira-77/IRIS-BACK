package com.iris.back.system.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.iris.back.common.model.BaseEntity;

@TableName("sys_file_ref")
public class SysFileRefEntity extends BaseEntity {

  private Long fileId;
  private String bizType;
  private Long bizId;
  private String category;
  private Integer sortNo;

  public Long getFileId() {
    return fileId;
  }

  public void setFileId(Long fileId) {
    this.fileId = fileId;
  }

  public String getBizType() {
    return bizType;
  }

  public void setBizType(String bizType) {
    this.bizType = bizType;
  }

  public Long getBizId() {
    return bizId;
  }

  public void setBizId(Long bizId) {
    this.bizId = bizId;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Integer getSortNo() {
    return sortNo;
  }

  public void setSortNo(Integer sortNo) {
    this.sortNo = sortNo;
  }
}
