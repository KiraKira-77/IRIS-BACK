package com.iris.back.framework.tenant;

public final class TenantContext {

  private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();

  private TenantContext() {
  }

  public static void setTenantId(Long tenantId) {
    TENANT_HOLDER.set(tenantId);
  }

  public static Long getTenantId() {
    return TENANT_HOLDER.get();
  }

  public static void clear() {
    TENANT_HOLDER.remove();
  }
}
