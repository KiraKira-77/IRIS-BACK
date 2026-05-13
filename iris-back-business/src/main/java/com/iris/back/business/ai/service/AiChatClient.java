package com.iris.back.business.ai.service;

import java.util.List;

public interface AiChatClient {

  ChatResult chat(ChatCommand command);

  record ChatCommand(
      String providerType,
      String baseUrl,
      String modelName,
      String apiKey,
      Integer timeoutSeconds,
      Double temperature,
      Integer maxTokens,
      List<ChatMessage> messages
  ) {
  }

  record ChatMessage(
      String role,
      String content
  ) {
  }

  record ChatResult(
      String content,
      Long latencyMs
  ) {
  }
}
