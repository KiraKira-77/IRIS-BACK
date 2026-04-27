package com.iris.back.business.standard.model.request;

public record StandardListQuery(
    String keyword,
    String category,
    String status,
    Long page,
    Long pageSize
) {
  public long normalizedPage() {
    return page == null || page < 1 ? 1 : page;
  }

  public long normalizedPageSize() {
    if (pageSize == null || pageSize < 1) {
      return 10;
    }
    return Math.min(pageSize, 100);
  }
}
