package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.project.model.dto.ProjectTaskDto;
import com.iris.back.business.project.service.HttpOmsClient;
import com.iris.back.business.project.service.OmsClient;
import com.iris.back.common.exception.BusinessException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpOmsClientTests {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final List<RecordedRequest> requests = new ArrayList<>();
  private HttpServer server;
  private HttpOmsClient client;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/", this::handle);
    server.start();
    client = new HttpOmsClient(
        objectMapper,
        "http://localhost:" + server.getAddress().getPort() + "/je/jolywood-it",
        3
    );
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void createWorkOrdersCallsInternalControlCreateAndMapsTaskIdAndTaskNo() throws Exception {
    List<OmsClient.OmsCreateResult> results = client.createWorkOrders(
        task(),
        List.of(new OmsClient.OmsCreateCommand(
            "201",
            "EMP001",
            "Handler A",
            "Finance check",
            "Handle in OMS",
            "7201:EMP001:8001",
            8001L
        ))
    );

    assertThat(requests).hasSize(1);
    RecordedRequest request = requests.get(0);
    assertThat(request.path()).isEqualTo("/je/jolywood-it/itsm/internal-control/task/create");
    JsonNode body = objectMapper.readTree(request.body());
    assertThat(body.get("taskName").asText()).isEqualTo("Finance check");
    assertThat(body.get("ownerCode").asText()).isEqualTo("EMP001");
    assertThat(body.get("checkOwnerCode").asText()).isEqualTo("EMP001");
    assertThat(body.get("taskDescription").asText()).isEqualTo("Handle in OMS");
    assertThat(body.get("issuedTime").asText()).isEqualTo("2026-05-06");
    assertThat(results.get(0).handlerId()).isEqualTo("201");
    assertThat(results.get(0).omsWorkOrderId()).isEqualTo("TASK-001");
    assertThat(results.get(0).status()).isEqualTo("created");
    assertThat(results.get(0).responsePayload()).contains("Ticket20260506001");
  }

  @Test
  void createWorkOrdersTrustsSuccessFlagWhenOmsReturnsNonStandardCode() {
    List<OmsClient.OmsCreateResult> results = client.createWorkOrders(
        task(),
        List.of(new OmsClient.OmsCreateCommand(
            "201",
            "EMP001",
            "Handler A",
            "OMS_CODE_SUCCESS",
            "Handle in OMS",
            "7201:EMP001:8001",
            8001L
        ))
    );

    assertThat(results).hasSize(1);
    assertThat(results.get(0).omsWorkOrderId()).isEqualTo("TASK-CODE-SUCCESS");
    assertThat(results.get(0).responsePayload()).contains("TicketCodeSuccess");
  }

  @Test
  void getWorkOrderCallsInternalControlDetailAndMapsStatus() {
    OmsClient.OmsWorkOrderSnapshot snapshot = client.getWorkOrder("TASK-001");

    assertThat(requests.get(0).path()).isEqualTo("/je/jolywood-it/itsm/internal-control/task/detail");
    assertThat(snapshot.omsWorkOrderId()).isEqualTo("TASK-001");
    assertThat(snapshot.omsStatus()).isEqualTo("20");
    assertThat(snapshot.omsStatusName()).isEqualTo("已完成");
    assertThat(snapshot.reviewable()).isTrue();
    assertThat(snapshot.resultSummary()).isEqualTo("处理完成");
  }

  @Test
  void returnWorkOrderCallsInternalControlBackWithReason() throws Exception {
    client.returnWorkOrder("TASK-001", "Need more evidence");

    assertThat(requests.get(0).path()).isEqualTo("/je/jolywood-it/itsm/internal-control/task/back");
    JsonNode body = objectMapper.readTree(requests.get(0).body());
    assertThat(body.get("taskId").asText()).isEqualTo("TASK-001");
    assertThat(body.get("remark").asText()).isEqualTo("Need more evidence");
  }

  @Test
  void createWorkOrdersPropagatesOmsFailureMessage() {
    assertThatThrownBy(() -> client.createWorkOrders(
        task(),
        List.of(new OmsClient.OmsCreateCommand(
            "201",
            "EMP001",
            "Handler A",
            "OMS_FAIL",
            "Handle in OMS",
            "7201:EMP001:8001",
            8001L
        ))
    ))
        .isInstanceOf(BusinessException.class)
        .hasMessage("负责人不存在：EMP001")
        .extracting("code")
        .isEqualTo("PROJECT_OMS_RESPONSE_FAILED");
  }

  private void handle(HttpExchange exchange) throws IOException {
    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    requests.add(new RecordedRequest(exchange.getRequestURI().getPath(), body));
    String response = responseFor(exchange.getRequestURI().getPath(), body);
    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }

  private String responseFor(String path, String body) {
    return switch (path) {
      case "/je/jolywood-it/itsm/internal-control/task/create" -> body.contains("OMS_FAIL")
          ? """
          {"success":false,"code":"OMS_OWNER_NOT_FOUND","msg":"负责人不存在：EMP001","data":null}
          """
          : body.contains("OMS_CODE_SUCCESS")
              ? """
          {"success":true,"code":"OMS_TASK_CREATED","msg":"created","data":{"taskId":"TASK-CODE-SUCCESS","taskNo":"TicketCodeSuccess"}}
          """
              : """
          {"success":true,"data":{"taskId":"TASK-001","taskNo":"Ticket20260506001"}}
          """;
      case "/je/jolywood-it/itsm/internal-control/task/detail" -> """
          {"success":true,"data":{"taskId":"TASK-001","taskNo":"Ticket20260506001","taskName":"Finance check","taskDescription":"Handle in OMS","status":"20","statusName":"已完成","issuedTime":"2026-05-06","completedTime":"2026-05-08 10:30:00","checkOwnerName":"Handler A","resultSummary":"处理完成"}}
          """;
      case "/je/jolywood-it/itsm/internal-control/task/logs" -> """
          {"success":true,"data":[{"occurredAt":"2026-05-08 10:30:00","operator":"Handler A","action":"complete","content":"处理完成"}]}
          """;
      case "/je/jolywood-it/itsm/internal-control/task/back" -> """
          {"success":true,"data":true}
          """;
      default -> throw new IllegalArgumentException("Unexpected path " + path);
    };
  }

  private ProjectTaskDto task() {
    return new ProjectTaskDto(
        "7201",
        "7001",
        "8801",
        "Finance checklist",
        "9901",
        "Check evidence",
        "Evidence complete",
        "monthly",
        "manual",
        "Finance check",
        "Handle in OMS",
        "201",
        "Handler A",
        null,
        null,
        "in_progress",
        "2026-05-06",
        null,
        0,
        0,
        0,
        List.of(),
        List.of()
    );
  }

  private record RecordedRequest(String path, String body) {
  }
}
