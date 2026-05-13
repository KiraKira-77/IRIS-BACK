package com.iris.back.business.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OpenAiCompatibleLlmConnectivityClient implements LlmConnectivityClient {

  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public OpenAiCompatibleLlmConnectivityClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  }

  @Override
  public TestResult test(TestCommand command) {
    long startedAt = System.nanoTime();
    if (!"openai_compatible".equals(command.providerType())) {
      return new TestResult(false, "当前仅支持 OpenAI Compatible 协议", elapsed(startedAt));
    }
    try {
      Map<String, Object> payload = Map.of(
          "model", command.modelName(),
          "messages", List.of(Map.of("role", "user", "content", "请只回复 ok")),
          "temperature", command.temperature() == null ? 0 : command.temperature(),
          "max_tokens", 8
      );
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(trimTrailingSlash(command.baseUrl()) + "/chat/completions"))
          .timeout(Duration.ofSeconds(command.timeoutSeconds() == null ? 30 : command.timeoutSeconds()))
          .header("Content-Type", "application/json")
          .header("Authorization", "Bearer " + command.apiKey())
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonNode root = readJson(response.body());
      if (response.statusCode() >= 200 && response.statusCode() < 300 && root.path("choices").isArray()) {
        return new TestResult(true, "模型连通性测试成功", elapsed(startedAt));
      }
      return new TestResult(false, errorMessage(root, response.statusCode()), elapsed(startedAt));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return new TestResult(false, "模型连通性测试被中断", elapsed(startedAt));
    } catch (Exception exception) {
      return new TestResult(false, "模型连通性测试失败：" + exception.getMessage(), elapsed(startedAt));
    }
  }

  private JsonNode readJson(String body) {
    try {
      return objectMapper.readTree(body == null || body.isBlank() ? "{}" : body);
    } catch (Exception exception) {
      return objectMapper.createObjectNode();
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
    return "模型接口返回异常，HTTP 状态码：" + statusCode;
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
