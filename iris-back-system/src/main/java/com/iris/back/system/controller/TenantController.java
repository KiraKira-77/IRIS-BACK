package com.iris.back.system.controller;

import com.iris.back.common.model.ApiResponse;
import com.iris.back.system.model.dto.TenantDto;
import com.iris.back.system.model.request.TenantUpsertRequest;
import com.iris.back.system.service.TenantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/tenants")
public class TenantController {

  private final TenantService tenantService;

  public TenantController(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @GetMapping
  public ApiResponse<List<TenantDto>> list() {
    return ApiResponse.success(tenantService.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<TenantDto> get(@PathVariable Long id) {
    return ApiResponse.success(tenantService.get(id));
  }

  @PostMapping
  public ApiResponse<TenantDto> create(@Valid @RequestBody TenantUpsertRequest request) {
    return ApiResponse.success("tenant created", tenantService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<TenantDto> update(@PathVariable Long id, @Valid @RequestBody TenantUpsertRequest request) {
    return ApiResponse.success("tenant updated", tenantService.update(id, request));
  }
}
