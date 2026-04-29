package com.iris.back.business.checklist.model.request;

public record ChecklistListQuery(
    String keyword,
    String status,
    String scopeId,
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
