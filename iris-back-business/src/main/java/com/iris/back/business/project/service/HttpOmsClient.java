package com.iris.back.business.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.project.model.dto.ProjectTaskDto;
import com.iris.back.common.exception.BusinessException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "iris.oms.mode", havingValue = "http")
public class HttpOmsClient implements OmsClient {

  // OMS 侧由工单系统提供的内控任务接口，baseUrl 只配置到 /je/jolywood-it。
  private static final String CREATE_PATH = "/itsm/internal-control/task/create";
  private static final String DETAIL_PATH = "/itsm/internal-control/task/detail";
  private static final String LOGS_PATH = "/itsm/internal-control/task/logs";
  private static final String BACK_PATH = "/itsm/internal-control/task/back";
  // OMS 状态码 20/25/30 表示内控侧可以进入复核处理，后续如 OMS 状态枚举变化需同步调整。
  private static final Set<String> REVIEWABLE_STATUSES = Set.of("20", "25", "30");

  private final ObjectMapper objectMapper;
  private final String baseUrl;
  private final HttpClient httpClient;
  private final Duration timeout;

  public HttpOmsClient(
      ObjectMapper objectMapper,
      @Value("${iris.oms.base-url:http://10.8.25.218:8002/je/jolywood-it}") String baseUrl,
      @Value("${iris.oms.timeout-seconds:5}") int timeoutSeconds
  ) {
    this.objectMapper = objectMapper;
    this.baseUrl = trimTrailingSlash(baseUrl);
    this.timeout = Duration.ofSeconds(timeoutSeconds);
    this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  @Override
  public List<OmsCreateResult> createWorkOrders(ProjectTaskDto task, List<OmsCreateCommand> commands) {
    List<OmsCreateResult> results = new ArrayList<>();
    for (OmsCreateCommand command : commands) {
      // OMS 当前用员工工号识别处理人，ownerCode 和 checkOwnerCode 都传同一个 handlerEmployeeNo。
      Map<String, Object> payload = new LinkedHashMap<>();
      payload.put("taskName", nonBlank(command.title(), task.taskName(), "IRIS internal control task"));
      payload.put("ownerCode", command.handlerEmployeeNo());
      payload.put("checkOwnerCode", command.handlerEmployeeNo());
      payload.put("taskDescription", nonBlank(command.description(), task.taskDescription(), null));
      payload.put("issuedTime", issuedDate(task.issuedAt()));

      JsonNode data = post(CREATE_PATH, payload);
      String omsWorkOrderId = firstText(data, "taskId", "taskNo");
      results.add(new OmsCreateResult(
          command.handlerId(),
          omsWorkOrderId,
          "created",
          null,
          data.toString()
      ));
    }
    return results;
  }

  @Override
  public OmsWorkOrderSnapshot getWorkOrder(String omsWorkOrderId) {
    JsonNode data = post(DETAIL_PATH, Map.of("taskId", omsWorkOrderId));
    String status = firstText(data, "status", "taskStatus", "state");
    String statusName = firstText(data, "statusName", "taskStatusName", "stateName");
    String resultSummary = firstText(data, "resultSummary", "taskResult", "taskDescription");
    return new OmsWorkOrderSnapshot(
        firstNonBlank(firstText(data, "taskId"), omsWorkOrderId),
        status,
        statusName,
        REVIEWABLE_STATUSES.contains(status),
        resultSummary,
        data.toString()
    );
  }

  @Override
  public List<OmsWorkOrderLogSnapshot> getWorkOrderLogs(String omsWorkOrderId) {
    JsonNode data = post(LOGS_PATH, Map.of("taskId", omsWorkOrderId, "logType", "all"));
    if (!data.isArray()) {
      return List.of();
    }
    List<OmsWorkOrderLogSnapshot> logs = new ArrayList<>();
    for (JsonNode row : data) {
      logs.add(new OmsWorkOrderLogSnapshot(
          firstText(row, "occurredAt", "createTime", "createdAt", "operateTime"),
          firstText(row, "operator", "operatorName", "userName", "createName"),
          firstText(row, "action", "actionName", "logType", "operation"),
          firstText(row, "content", "remark", "description", "message")
      ));
    }
    return logs;
  }

  @Override
  public List<OmsAttachmentSnapshot> getWorkOrderAttachments(String omsWorkOrderId) {
    // 目前 OMS 暂未提供附件接口，保留字段用于后续接入后统一回写本地快照。
    return List.of();
  }

  @Override
  public void returnWorkOrder(String omsWorkOrderId, String reason) {
    post(BACK_PATH, Map.of("taskId", omsWorkOrderId, "remark", reason));
  }

  private JsonNode post(String path, Map<String, Object> payload) {
    try {
      String body = objectMapper.writeValueAsString(payload);
      HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
          .timeout(timeout)
          .header("Content-Type", "application/json; charset=utf-8")
          .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      JsonNode root = objectMapper.readTree(response.body());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        // HTTP 层失败也尽量透传 OMS 响应体里的中文错误，方便前端直接展示真实失败原因。
        throw new BusinessException("PROJECT_OMS_HTTP_FAILED", omsErrorMessage(root, "PROJECT_OMS_HTTP_FAILED"));
      }
      ensureSuccess(root);
      return root.has("data") ? root.get("data") : root;
    } catch (JsonProcessingException exception) {
      throw new BusinessException("PROJECT_OMS_PAYLOAD_SERIALIZE_FAILED", "PROJECT_OMS_PAYLOAD_SERIALIZE_FAILED");
    } catch (IOException exception) {
      throw new BusinessException("PROJECT_OMS_HTTP_FAILED", "PROJECT_OMS_HTTP_FAILED");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new BusinessException("PROJECT_OMS_HTTP_INTERRUPTED", "PROJECT_OMS_HTTP_INTERRUPTED");
    }
  }

  private void ensureSuccess(JsonNode root) {
    JsonNode success = root.get("success");
    if (success != null && success.isBoolean()) {
      if (!success.asBoolean()) {
        throw new BusinessException("PROJECT_OMS_RESPONSE_FAILED", omsErrorMessage(root, "PROJECT_OMS_RESPONSE_FAILED"));
      }
      // OMS 有些成功响应 code 不是统一成功码，只要 success=true 就按成功处理。
      return;
    }
    JsonNode code = root.get("code");
    if (code != null && !code.isNull()) {
      String value = code.asText();
      if (!value.isBlank() && !Set.of("0", "200", "success", "SUCCESS").contains(value)) {
        throw new BusinessException("PROJECT_OMS_RESPONSE_FAILED", omsErrorMessage(root, "PROJECT_OMS_RESPONSE_FAILED"));
      }
    }
  }

  private String omsErrorMessage(JsonNode root, String fallback) {
    String message = firstText(root, "msg", "message", "error", "errorMsg", "errorMessage");
    if (message != null) {
      return message;
    }
    JsonNode data = root.get("data");
    if (data != null && data.isObject()) {
      message = firstText(data, "msg", "message", "error", "errorMsg", "errorMessage");
    }
    return message == null ? fallback : message;
  }

  private String issuedDate(String issuedAt) {
    String normalized = trimToNull(issuedAt);
    if (normalized == null) {
      return LocalDate.now().toString();
    }
    return normalized.length() >= 10 ? normalized.substring(0, 10) : normalized;
  }

  private String firstText(JsonNode node, String... names) {
    for (String name : names) {
      JsonNode value = node.get(name);
      if (value != null && !value.isNull()) {
        String text = trimToNull(value.asText());
        if (text != null) {
          return text;
        }
      }
    }
    return null;
  }

  private String nonBlank(String first, String second, String fallback) {
    return firstNonBlank(first, second, fallback);
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      String normalized = trimToNull(value);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String trimTrailingSlash(String value) {
    String normalized = value == null || value.isBlank()
        ? "http://10.8.25.218:8002/je/jolywood-it"
        : value.trim();
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }
}
