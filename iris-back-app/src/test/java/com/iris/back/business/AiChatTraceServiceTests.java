package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iris.back.business.ai.mapper.AiChatTraceEventMapper;
import com.iris.back.business.ai.mapper.AiChatTraceMapper;
import com.iris.back.business.ai.model.entity.AiChatTraceEntity;
import com.iris.back.business.ai.service.AiChatTraceService;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiChatTraceServiceTests {

  @Mock
  private AiChatTraceMapper traceMapper;

  @Mock
  private AiChatTraceEventMapper eventMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  private AiChatTraceService traceService;

  @BeforeEach
  void setUp() {
    traceService = new AiChatTraceService(
        traceMapper,
        eventMapper,
        new ObjectMapper(),
        currentUserContext
    );
  }

  @Test
  void findLastProjectCodeInSessionExtractsProjectCodeFromRecentTraceText() {
    CurrentUserPrincipal principal = new CurrentUserPrincipal(
        "token",
        2001L,
        1001L,
        "admin",
        "Platform Administrator",
        "IRIS",
        List.of("PLATFORM_ADMIN")
    );
    when(traceMapper.selectList(any())).thenReturn(List.of(
        trace(2L, "\u521a\u624d\u7684\u95ee\u9898", "\u6ca1\u6709\u7f16\u53f7"),
        trace(1L, "\u9879\u76ee PRJ-2049683439946584066", "\u5df2\u5f52\u6863")
    ));

    String projectCode = traceService.findLastProjectCodeInSession(principal, "session-1");

    assertThat(projectCode).isEqualTo("PRJ-2049683439946584066");
  }

  private AiChatTraceEntity trace(Long id, String question, String answer) {
    AiChatTraceEntity entity = new AiChatTraceEntity();
    entity.setId(id);
    entity.setTenantId(1001L);
    entity.setUserId(2001L);
    entity.setSessionId("session-1");
    entity.setQuestion(question);
    entity.setAnswer(answer);
    entity.setDeleted(0);
    entity.setCreatedAt(LocalDateTime.now());
    return entity;
  }
}
