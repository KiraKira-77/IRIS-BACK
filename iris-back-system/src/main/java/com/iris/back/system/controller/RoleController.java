package com.iris.back.system.controller;

import com.iris.back.common.model.ApiResponse;
import com.iris.back.system.model.dto.RoleDto;
import com.iris.back.system.model.request.RoleUpsertRequest;
import com.iris.back.system.service.RoleService;
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
@RequestMapping("/api/v1/system/roles")
public class RoleController {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping
  public ApiResponse<List<RoleDto>> list() {
    return ApiResponse.success(roleService.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<RoleDto> get(@PathVariable Long id) {
    return ApiResponse.success(roleService.get(id));
  }

  @PostMapping
  public ApiResponse<RoleDto> create(@Valid @RequestBody RoleUpsertRequest request) {
    return ApiResponse.success("role created", roleService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<RoleDto> update(@PathVariable Long id, @Valid @RequestBody RoleUpsertRequest request) {
    return ApiResponse.success("role updated", roleService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    roleService.delete(id);
    return ApiResponse.success("role deleted", null);
  }
}
