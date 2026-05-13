package com.iris.back.business.ai.service;

public interface LlmConnectivityClient {

  TestResult test(TestCommand command);

  record TestCommand(
      String providerType,
      String baseUrl,
      String modelName,
      String apiKey,
      Integer timeoutSeconds,
      Double temperature,
      Integer maxTokens
  ) {
  }

  record TestResult(
      boolean success,
      String message,
      Long latencyMs
  ) {
  }
}
