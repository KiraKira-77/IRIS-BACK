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
}
