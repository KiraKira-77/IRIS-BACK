package com.iris.back.business.project.controller;

import com.iris.back.business.project.model.dto.ProjectDto;
import com.iris.back.business.project.model.request.ProjectListQuery;
import com.iris.back.business.project.model.request.ProjectUpsertRequest;
import com.iris.back.business.project.service.ProjectService;
import com.iris.back.common.model.ApiResponse;
import com.iris.back.common.model.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

  private final ProjectService projectService;

  public ProjectController(ProjectService projectService) {
    this.projectService = projectService;
  }

  @GetMapping
  public ApiResponse<PageResponse<ProjectDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String tagId,
      @RequestParam(required = false) String source,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(projectService.list(new ProjectListQuery(
        keyword,
        status,
        tagId,
        source,
        startDate,
        endDate,
        page,
        pageSize
    )));
  }

  @GetMapping("/{id}")
  public ApiResponse<ProjectDto> get(@PathVariable String id) {
    return ApiResponse.success(projectService.get(id));
  }

  @PostMapping
  public ApiResponse<ProjectDto> create(@Valid @RequestBody ProjectUpsertRequest request) {
    return ApiResponse.success("project created", projectService.create(request));
  }

  @PostMapping("/{id}/start")
  public ApiResponse<ProjectDto> start(@PathVariable String id) {
    return ApiResponse.success("project started", projectService.start(id));
  }

  @PostMapping("/{id}/complete")
  public ApiResponse<ProjectDto> complete(@PathVariable String id) {
    return ApiResponse.success("project completed", projectService.complete(id));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    projectService.delete(id);
    return ApiResponse.success("project deleted", null);
  }
}
