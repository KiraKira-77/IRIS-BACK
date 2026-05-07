package com.iris.back.business.project.controller;

import com.iris.back.business.project.model.dto.ProjectArchiveDto;
import com.iris.back.business.project.service.ProjectArchiveService;
import com.iris.back.common.model.ApiResponse;
import com.iris.back.common.model.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/archives")
public class ProjectArchiveController {

  private final ProjectArchiveService projectArchiveService;

  public ProjectArchiveController(ProjectArchiveService projectArchiveService) {
    this.projectArchiveService = projectArchiveService;
  }

  @GetMapping
  public ApiResponse<PageResponse<ProjectArchiveDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(projectArchiveService.list(keyword, status, page, pageSize));
  }

  @GetMapping("/{id}")
  public ApiResponse<ProjectArchiveDto> detail(@PathVariable String id) {
    return ApiResponse.success(projectArchiveService.detail(id));
  }
}
