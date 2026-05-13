package com.iris.back.business;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iris.back.business.ai.controller.AiChatTraceController;
import com.iris.back.business.ai.model.dto.AiChatTraceDetailDto;
import com.iris.back.business.ai.model.dto.AiChatTraceEventDto;
import com.iris.back.business.ai.model.dto.AiChatTraceListItemDto;
import com.iris.back.business.ai.service.AiChatTraceService;
import com.iris.back.common.model.PageResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AiChatTraceControllerTests {

  @Mock
  private AiChatTraceService aiChatTraceService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new AiChatTraceController(aiChatTraceService)).build();
  }

  @Test
  void listReturnsTracePage() throws Exception {
    when(aiChatTraceService.list("项目", "ok", 1L, 10L)).thenReturn(PageResponse.of(
        1,
        1,
        10,
        List.of(new AiChatTraceListItemDto(
            "trace-1",
            "session-1",
            "10001",
            "管理员",
            "/smart/models",
            "模型库里有哪些模型？",
            "ok",
            "gpt-4o-mini",
            "[\"model_config\"]",
            126L,
            "2026-05-08 10:00:00"
        ))
    ));

    mockMvc.perform(get("/api/v1/ai/chat/traces")
            .param("keyword", "项目")
            .param("status", "ok")
            .param("page", "1")
            .param("pageSize", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.total").value(1))
        .andExpect(jsonPath("$.data.records[0].traceId").value("trace-1"))
        .andExpect(jsonPath("$.data.records[0].question").value("模型库里有哪些模型？"))
        .andExpect(jsonPath("$.data.records[0].status").value("ok"));
  }

  @Test
  void detailReturnsTraceWithEvents() throws Exception {
    when(aiChatTraceService.detail("trace-1")).thenReturn(new AiChatTraceDetailDto(
        "trace-1",
        "session-1",
        "10001",
        "管理员",
        "/smart/models",
        "model",
        "100",
        "模型库里有哪些模型？",
        "当前模型库有 2 个模型。",
        "ok",
        "openai_compatible",
        "gpt-4o-mini",
        "[\"model_config\"]",
        "[{\"source\":\"model_config\"}]",
        126L,
        null,
        "2026-05-08 10:00:00",
        "2026-05-08 10:00:01",
        List.of(new AiChatTraceEventDto(
            1,
            "tool_context",
            "collect_context",
            "ok",
            "{\"tools\":[\"model_config\"]}",
            40L,
            "2026-05-08 10:00:00"
        ))
    ));

    mockMvc.perform(get("/api/v1/ai/chat/traces/trace-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.traceId").value("trace-1"))
        .andExpect(jsonPath("$.data.answer").value("当前模型库有 2 个模型。"))
        .andExpect(jsonPath("$.data.events[0].eventName").value("collect_context"))
        .andExpect(jsonPath("$.data.events[0].detailJson").value("{\"tools\":[\"model_config\"]}"));
  }
}
