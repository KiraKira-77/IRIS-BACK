package com.iris.back.business.project.controller;

import com.iris.back.business.project.model.dto.ProjectOperationLogDto;
import com.iris.back.business.project.model.request.ProjectOperationLogQuery;
import com.iris.back.business.project.service.ProjectOperationLogService;
import com.iris.back.common.model.ApiResponse;
import com.iris.back.common.model.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {

  private final ProjectOperationLogService operationLogService;

  public LogController(ProjectOperationLogService operationLogService) {
    this.operationLogService = operationLogService;
  }

  @GetMapping
  public ApiResponse<PageResponse<ProjectOperationLogDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String source,
      @RequestParam(required = false) String level,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String projectId,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(operationLogService.list(new ProjectOperationLogQuery(
        keyword,
        source,
        level,
        action,
        projectId,
        page,
        pageSize
    )));
  }
}
