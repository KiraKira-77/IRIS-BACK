package com.iris.back.business.checklist.controller;

import com.iris.back.business.checklist.model.dto.ChecklistDto;
import com.iris.back.business.checklist.model.request.ChecklistListQuery;
import com.iris.back.business.checklist.model.request.ChecklistUpsertRequest;
import com.iris.back.business.checklist.service.ChecklistService;
import com.iris.back.common.model.ApiResponse;
import com.iris.back.common.model.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checklists")
public class ChecklistController {

  private final ChecklistService checklistService;

  public ChecklistController(ChecklistService checklistService) {
    this.checklistService = checklistService;
  }

  @GetMapping
  public ApiResponse<PageResponse<ChecklistDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String scopeId,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(checklistService.list(new ChecklistListQuery(
        keyword,
        status,
        scopeId,
        page,
        pageSize
    )));
  }

  @GetMapping("/{id}")
  public ApiResponse<ChecklistDto> get(@PathVariable String id) {
    return ApiResponse.success(checklistService.get(id));
  }

  @PostMapping
  public ApiResponse<ChecklistDto> create(@Valid @RequestBody ChecklistUpsertRequest request) {
    return ApiResponse.success("checklist created", checklistService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<ChecklistDto> update(
      @PathVariable String id,
      @Valid @RequestBody ChecklistUpsertRequest request
  ) {
    return ApiResponse.success("checklist updated", checklistService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    checklistService.delete(id);
    return ApiResponse.success("checklist deleted", null);
  }
}
