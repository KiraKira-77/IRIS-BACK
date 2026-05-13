package com.iris.back.business.project.controller;

import com.iris.back.business.project.model.dto.AlertEventDto;
import com.iris.back.business.project.model.request.AlertQuery;
import com.iris.back.business.project.service.AlertService;
import com.iris.back.common.model.ApiResponse;
import com.iris.back.common.model.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

  private final AlertService alertService;

  public AlertController(AlertService alertService) {
    this.alertService = alertService;
  }

  @GetMapping
  public ApiResponse<PageResponse<AlertEventDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String level,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(alertService.list(new AlertQuery(keyword, level, page, pageSize)));
  }

  @PutMapping("/{id}/ack")
  public ApiResponse<Void> acknowledge(@PathVariable String id) {
    alertService.acknowledge(id);
    return ApiResponse.success("alert acknowledged", null);
  }
}
