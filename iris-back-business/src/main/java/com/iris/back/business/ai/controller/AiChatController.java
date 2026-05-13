package com.iris.back.business.ai.controller;

import com.iris.back.business.ai.model.dto.AiChatMessageDto;
import com.iris.back.business.ai.model.dto.AiChatSessionDto;
import com.iris.back.business.ai.model.request.AiChatMessageRequest;
import com.iris.back.business.ai.service.AiChatService;
import com.iris.back.common.model.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/chat")
public class AiChatController {

  private final AiChatService aiChatService;

  public AiChatController(AiChatService aiChatService) {
    this.aiChatService = aiChatService;
  }

  @PostMapping("/sessions")
  public ApiResponse<AiChatSessionDto> createSession() {
    return ApiResponse.success(aiChatService.createSession());
  }

  @PostMapping("/messages")
  public ApiResponse<AiChatMessageDto> sendMessage(@RequestBody AiChatMessageRequest request) {
    return ApiResponse.success(aiChatService.sendMessage(request));
  }
}
