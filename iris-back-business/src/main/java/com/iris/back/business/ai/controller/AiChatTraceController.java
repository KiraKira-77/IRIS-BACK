package com.iris.back.business.ai.controller;

import com.iris.back.business.ai.model.dto.AiChatTraceDetailDto;
import com.iris.back.business.ai.model.dto.AiChatTraceListItemDto;
import com.iris.back.business.ai.service.AiChatTraceService;
import com.iris.back.common.model.ApiResponse;
import com.iris.back.common.model.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/chat/traces")
public class AiChatTraceController {

  private final AiChatTraceService aiChatTraceService;

  public AiChatTraceController(AiChatTraceService aiChatTraceService) {
    this.aiChatTraceService = aiChatTraceService;
  }

  @GetMapping
  public ApiResponse<PageResponse<AiChatTraceListItemDto>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(required = false, defaultValue = "1") Long page,
      @RequestParam(required = false, defaultValue = "10") Long pageSize
  ) {
    return ApiResponse.success(aiChatTraceService.list(keyword, status, page, pageSize));
  }

  @GetMapping("/{traceId}")
  public ApiResponse<AiChatTraceDetailDto> detail(@PathVariable String traceId) {
    return ApiResponse.success(aiChatTraceService.detail(traceId));
  }
}
