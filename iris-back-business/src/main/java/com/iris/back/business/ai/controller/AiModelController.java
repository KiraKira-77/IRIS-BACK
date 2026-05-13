package com.iris.back.business.ai.controller;

import com.iris.back.business.ai.model.dto.AiModelConfigDto;
import com.iris.back.business.ai.model.dto.AiModelTestResultDto;
import com.iris.back.business.ai.model.request.AiModelConfigUpsertRequest;
import com.iris.back.business.ai.service.AiModelConfigService;
import com.iris.back.common.model.ApiResponse;
import com.iris.back.common.model.PageResponse;
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
@RequestMapping("/api/v1/models")
public class AiModelController {

  private final AiModelConfigService aiModelConfigService;

  public AiModelController(AiModelConfigService aiModelConfigService) {
    this.aiModelConfigService = aiModelConfigService;
  }

  @GetMapping
  public ApiResponse<PageResponse<AiModelConfigDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String providerType,
      @RequestParam(required = false) String status,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(aiModelConfigService.list(keyword, providerType, status, page, pageSize));
  }

  @PostMapping
  public ApiResponse<AiModelConfigDto> create(@RequestBody AiModelConfigUpsertRequest request) {
    return ApiResponse.success("model config created", aiModelConfigService.create(request));
  }

  @PutMapping("/{id}")
  public ApiResponse<AiModelConfigDto> update(
      @PathVariable String id,
      @RequestBody AiModelConfigUpsertRequest request
  ) {
    return ApiResponse.success("model config updated", aiModelConfigService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable String id) {
    aiModelConfigService.delete(id);
    return ApiResponse.success("model config deleted", null);
  }

  @PostMapping("/{id}/test")
  public ApiResponse<AiModelTestResultDto> testConnection(@PathVariable String id) {
    return ApiResponse.success(aiModelConfigService.testConnection(id));
  }
}
