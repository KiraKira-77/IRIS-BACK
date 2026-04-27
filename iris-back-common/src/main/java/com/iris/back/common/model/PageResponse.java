package com.iris.back.common.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageResponse<T> {
  long total;
  long pageNo;
  long pageSize;
  List<T> records;

  public PageResponse(long total, long pageNo, long pageSize, List<T> records) {
    this.total = total;
    this.pageNo = pageNo;
    this.pageSize = pageSize;
    this.records = records;
  }

  public static <T> PageResponse<T> of(long total, long pageNo, long pageSize, List<T> records) {
    return new PageResponse<>(total, pageNo, pageSize, records);
  }

  public long getTotal() {
    return total;
  }

  public long getPageNo() {
    return pageNo;
  }

  public long getPageSize() {
    return pageSize;
  }

  public List<T> getRecords() {
    return records;
  }
}
