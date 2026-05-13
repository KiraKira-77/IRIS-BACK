package com.iris.back.business.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.common.exception.BusinessException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenAiCompatibleChatClient implements AiChatClient {

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public OpenAiCompatibleChatClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  @Override
  public ChatResult chat(ChatCommand command) {
    if (!"openai_compatible".equals(command.providerType())) {
      throw new BusinessException("AI_CHAT_PROVIDER_UNSUPPORTED", "AI_CHAT_PROVIDER_UNSUPPORTED");
    }
    long startedAt = System.nanoTime();
    try {
      Map<String, Object> payload = Map.of(
          "model", command.modelName(),
          "messages", command.messages().stream()
              .map(message -> Map.of("role", message.role(), "content", message.content()))
              .toList(),
          "temperature", command.temperature() == null ? 0.2 : command.temperature(),
          "max_tokens", command.maxTokens() == null ? 3000 : command.maxTokens()
      );
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(trimTrailingSlash(command.baseUrl()) + "/chat/completions"))
          .timeout(Duration.ofSeconds(command.timeoutSeconds() == null ? 30 : command.timeoutSeconds()))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + command.apiKey())
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonNode root = objectMapper.readTree(response.body() == null || response.body().isBlank() ? "{}" : response.body());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (!content.isBlank()) {
          return new ChatResult(content, elapsed(startedAt));
        }
      }
      throw new BusinessException("AI_CHAT_MODEL_FAILED", errorMessage(root, response.statusCode()));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new BusinessException("AI_CHAT_MODEL_INTERRUPTED", "AI_CHAT_MODEL_INTERRUPTED");
    } catch (BusinessException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new BusinessException("AI_CHAT_MODEL_FAILED", exception.getMessage());
    }
  }

  private String errorMessage(JsonNode root, int statusCode) {
    JsonNode error = root.path("error");
    if (error.isObject() && !error.path("message").asText("").isBlank()) {
      return error.path("message").asText();
    }
    if (!root.path("message").asText("").isBlank()) {
      return root.path("message").asText();
    }
    return "AI_CHAT_MODEL_HTTP_" + statusCode;
  }

  private long elapsed(long startedAt) {
    return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
  }

  private String trimTrailingSlash(String value) {
    if (value == null) {
      return "";
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }
}
