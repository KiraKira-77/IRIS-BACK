package com.iris.back.system.controller;

import com.iris.back.common.model.ApiResponse;
import com.iris.back.system.model.dto.ResourceScopeDto;
import com.iris.back.system.model.dto.ResourceScopeMemberDto;
import com.iris.back.system.model.request.ResourceScopeMemberReplaceRequest;
import com.iris.back.system.model.request.ResourceScopeUpsertRequest;
import com.iris.back.system.service.ResourceScopeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/resource-scopes")
public class ResourceScopeController {

  private final ResourceScopeService resourceScopeService;

  public ResourceScopeController(ResourceScopeService resourceScopeService) {
    this.resourceScopeService = resourceScopeService;
  }

  @GetMapping
  public ApiResponse<List<ResourceScopeDto>> list() {
    return ApiResponse.success(resourceScopeService.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<ResourceScopeDto> get(@PathVariable Long id) {
    return ApiResponse.success(resourceScopeService.get(id));
  }

  @PostMapping
  public ApiResponse<ResourceScopeDto> create(@Valid @RequestBody ResourceScopeUpsertRequest request) {
    return ApiResponse.success("resource scope created", resourceScopeService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<ResourceScopeDto> update(
      @PathVariable Long id,
      @Valid @RequestBody ResourceScopeUpsertRequest request
  ) {
    return ApiResponse.success("resource scope updated", resourceScopeService.update(id, request));
  }

  @GetMapping("/{id}/members")
  public ApiResponse<List<ResourceScopeMemberDto>> listMembers(@PathVariable Long id) {
    return ApiResponse.success(resourceScopeService.listMembers(id));
  }

  @PutMapping("/{id}/members")
  public ApiResponse<Void> replaceMembers(
      @PathVariable Long id,
      @Valid @RequestBody ResourceScopeMemberReplaceRequest request
  ) {
    resourceScopeService.replaceMembers(id, request);
    return ApiResponse.success("resource scope members updated", null);
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    resourceScopeService.delete(id);
    return ApiResponse.success("resource scope deleted", null);
  }
}
