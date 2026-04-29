package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.back.business.project.service.MockOmsClient;
import org.junit.jupiter.api.Test;

class MockOmsClientTests {

  @Test
  void formatsLogOccurredAtWithoutIsoSeparator() {
    MockOmsClient client = new MockOmsClient();

    var logs = client.getWorkOrderLogs("OMS-20260429-0001");

    assertThat(logs).singleElement().satisfies(log -> {
      assertThat(log.occurredAt()).doesNotContain("T");
      assertThat(log.occurredAt()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    });
  }
}
