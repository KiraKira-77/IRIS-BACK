package com.iris.back.business.standard.model.request;

public record StandardListQuery(
    String keyword,
    String category,
    String status,
    Long page,
    Long pageSize,
    String sortBy,
    String sortOrder
) {
  public StandardListQuery(
      String keyword,
      String category,
      String status,
      Long page,
      Long pageSize
  ) {
    this(keyword, category, status, page, pageSize, null, null);
  }

  public long normalizedPage() {
    return page == null || page < 1 ? 1 : page;
  }

  public long normalizedPageSize() {
    if (pageSize == null || pageSize < 1) {
      return 10;
    }
    return Math.min(pageSize, 100);
  }

  public String normalizedSortBy() {
    return sortBy == null || sortBy.isBlank() ? "uploadDate" : sortBy.trim();
  }

  public boolean sortAscending() {
    return "asc".equalsIgnoreCase(sortOrder)
        || "ascending".equalsIgnoreCase(sortOrder);
  }
}
