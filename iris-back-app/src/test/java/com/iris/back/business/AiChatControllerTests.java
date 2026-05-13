package com.iris.back.business;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.ai.controller.AiChatController;
import com.iris.back.business.ai.model.dto.AiChatMessageDto;
import com.iris.back.business.ai.model.dto.AiChatSessionDto;
import com.iris.back.business.ai.service.AiChatService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AiChatControllerTests {

  @Mock
  private AiChatService aiChatService;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new AiChatController(aiChatService)).build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void createSessionReturnsSessionDto() throws Exception {
    when(aiChatService.createSession()).thenReturn(new AiChatSessionDto("session-1", "AI 对话", "2026-05-08 10:00:00"));

    mockMvc.perform(post("/api/v1/ai/chat/sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value("session-1"));
  }

  @Test
  void sendMessageDelegatesToChatService() throws Exception {
    when(aiChatService.sendMessage(any())).thenReturn(new AiChatMessageDto(
        "message-1",
        "trace-1",
        "session-1",
        "assistant",
        "当前权限下找到 1 条数据。",
        "ok",
        List.of(),
        List.of(),
        120L,
        "2026-05-08 10:00:00"
    ));

    mockMvc.perform(post("/api/v1/ai/chat/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(java.util.Map.of(
                "sessionId", "session-1",
                "message", "总结项目"
            ))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").value("当前权限下找到 1 条数据。"))
        .andExpect(jsonPath("$.data.status").value("ok"));
    verify(aiChatService).sendMessage(any());
  }
}
