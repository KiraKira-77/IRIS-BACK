package com.iris.back.business.plan.model.request;

public record PlanListQuery(
    String keyword,
    Integer year,
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
