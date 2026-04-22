package com.iris.back.system.controller;

import com.iris.back.common.model.ApiResponse;
import com.iris.back.system.model.dto.OrgDto;
import com.iris.back.system.model.request.OrgUpsertRequest;
import com.iris.back.system.service.OrgService;
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
@RequestMapping("/api/v1/system/orgs")
public class OrgController {

  private final OrgService orgService;

  public OrgController(OrgService orgService) {
    this.orgService = orgService;
  }

  @GetMapping
  public ApiResponse<List<OrgDto>> list() {
    return ApiResponse.success(orgService.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<OrgDto> get(@PathVariable Long id) {
    return ApiResponse.success(orgService.get(id));
  }

  @PostMapping
  public ApiResponse<OrgDto> create(@Valid @RequestBody OrgUpsertRequest request) {
    return ApiResponse.success("org created", orgService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<OrgDto> update(@PathVariable Long id, @Valid @RequestBody OrgUpsertRequest request) {
    return ApiResponse.success("org updated", orgService.update(id, request));
  }
}
