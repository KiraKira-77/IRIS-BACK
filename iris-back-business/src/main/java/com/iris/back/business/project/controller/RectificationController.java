package com.iris.back.business.project.controller;

import com.iris.back.business.project.model.dto.RectificationDto;
import com.iris.back.business.project.model.request.RectificationCreateRequest;
import com.iris.back.business.project.model.request.RectificationListQuery;
import com.iris.back.business.project.model.request.RectificationReviewRequest;
import com.iris.back.business.project.service.RectificationService;
import com.iris.back.common.model.ApiResponse;
import com.iris.back.common.model.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rectifications")
public class RectificationController {

  private final RectificationService rectificationService;

  public RectificationController(RectificationService rectificationService) {
    this.rectificationService = rectificationService;
  }

  @GetMapping
  public ApiResponse<PageResponse<RectificationDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String projectId,
      @RequestParam(required = false) String assigneeId,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(rectificationService.list(new RectificationListQuery(
        keyword,
        status,
        projectId,
        assigneeId,
        page,
        pageSize
    )));
  }

  @GetMapping("/{id}")
  public ApiResponse<RectificationDto> get(@PathVariable String id) {
    return ApiResponse.success(rectificationService.get(id));
  }

  @PostMapping
  public ApiResponse<RectificationDto> create(@Valid @RequestBody RectificationCreateRequest request) {
    return ApiResponse.success("rectification created", rectificationService.create(request));
  }

  @PostMapping("/{id}/submit")
  public ApiResponse<RectificationDto> submit(@PathVariable String id) {
    return ApiResponse.success("rectification submitted", rectificationService.submit(id));
  }

  @PostMapping("/{id}/review")
  public ApiResponse<RectificationDto> review(
      @PathVariable String id,
      @Valid @RequestBody RectificationReviewRequest request
  ) {
    return ApiResponse.success("rectification reviewed", rectificationService.review(id, request));
  }
}
