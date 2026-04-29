package com.iris.back.business.plan.controller;

import com.iris.back.business.plan.model.dto.PlanDto;
import com.iris.back.business.plan.model.request.PlanListQuery;
import com.iris.back.business.plan.model.request.PlanUpsertRequest;
import com.iris.back.business.plan.service.PlanService;
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
@RequestMapping("/api/v1/plans")
public class PlanController {

  private final PlanService planService;

  public PlanController(PlanService planService) {
    this.planService = planService;
  }

  @GetMapping
  public ApiResponse<PageResponse<PlanDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) String status,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(planService.list(new PlanListQuery(keyword, year, status, page, pageSize)));
  }

  @GetMapping("/{id}")
  public ApiResponse<PlanDto> get(@PathVariable String id) {
    return ApiResponse.success(planService.get(id));
  }

  @PostMapping
  public ApiResponse<PlanDto> create(@Valid @RequestBody PlanUpsertRequest request) {
    return ApiResponse.success("plan created", planService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<PlanDto> update(
      @PathVariable String id,
      @Valid @RequestBody PlanUpsertRequest request
  ) {
    return ApiResponse.success("plan updated", planService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    planService.delete(id);
    return ApiResponse.success("plan deleted", null);
  }

  @PostMapping("/{id}/submit")
  public ApiResponse<PlanDto> submit(@PathVariable String id) {
    return ApiResponse.success("plan submitted", planService.submit(id));
  }

  @PostMapping("/{id}/approve")
  public ApiResponse<PlanDto> approve(@PathVariable String id) {
    return ApiResponse.success("plan approved", planService.approve(id));
  }
}
