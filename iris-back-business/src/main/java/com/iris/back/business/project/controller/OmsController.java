package com.iris.back.business.project.controller;

import com.iris.back.business.project.service.OmsClient;
import com.iris.back.common.model.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/oms")
public class OmsController {

  private final OmsClient omsClient;

  public OmsController(OmsClient omsClient) {
    this.omsClient = omsClient;
  }

  @GetMapping("/users")
  public ApiResponse<List<OmsClient.OmsUser>> searchUsers(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false, defaultValue = "1") Integer page,
      @RequestParam(required = false, defaultValue = "20") Integer pageSize
  ) {
    return ApiResponse.success(omsClient.searchUsers(keyword, page, pageSize));
  }
}
