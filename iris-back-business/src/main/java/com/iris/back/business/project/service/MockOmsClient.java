package com.iris.back.business.project.service;

import com.iris.back.business.project.model.dto.ProjectTaskDto;
import com.iris.back.common.util.DateTimeFormatters;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class MockOmsClient implements OmsClient {

  private final AtomicInteger sequence = new AtomicInteger(1);

  @Override
  public List<OmsCreateResult> createWorkOrders(ProjectTaskDto task, List<OmsCreateCommand> commands) {
    return commands.stream()
        .map(command -> new OmsCreateResult(
            command.handlerId(),
            nextOmsId(),
            "created",
            null,
            "{\"mock\":true}"
        ))
        .toList();
  }

  @Override
  public OmsWorkOrderSnapshot getWorkOrder(String omsWorkOrderId) {
    return new OmsWorkOrderSnapshot(
        omsWorkOrderId,
        "20",
        "completed",
        true,
        "Mock OMS work order completed",
        "{\"mock\":true,\"status\":\"20\"}"
    );
  }

  @Override
  public List<OmsWorkOrderLogSnapshot> getWorkOrderLogs(String omsWorkOrderId) {
    return List.of(new OmsWorkOrderLogSnapshot(
        DateTimeFormatters.formatDateTime(LocalDateTime.now()),
        "OMS",
        "complete",
        "Mock OMS log for " + omsWorkOrderId
    ));
  }

  @Override
  public List<OmsAttachmentSnapshot> getWorkOrderAttachments(String omsWorkOrderId) {
    return List.of();
  }

  private String nextOmsId() {
    return "OMS-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        + "-" + String.format("%04d", sequence.getAndIncrement());
  }
}
