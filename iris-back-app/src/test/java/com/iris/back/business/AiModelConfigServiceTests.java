package com.iris.back.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iris.back.business.ai.mapper.AiModelConfigMapper;
import com.iris.back.business.ai.model.dto.AiModelConfigDto;
import com.iris.back.business.ai.model.entity.AiModelConfigEntity;
import com.iris.back.business.ai.model.request.AiModelConfigUpsertRequest;
import com.iris.back.business.ai.service.AiModelConfigService;
import com.iris.back.business.ai.service.LlmConnectivityClient;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class AiModelConfigServiceTests {

  @Mock
  private AiModelConfigMapper aiModelConfigMapper;

  @Mock
  private CurrentUserContext currentUserContext;

  @Mock
  private LlmConnectivityClient connectivityClient;

  private AiModelConfigService aiModelConfigService;

  @BeforeEach
  void setUp() {
    aiModelConfigService = new AiModelConfigService(
        aiModelConfigMapper,
        currentUserContext,
        connectivityClient,
        "unit-test-secret"
    );
  }

  @Test
  void createStoresEncryptedApiKeyAndReturnsMaskedConfiguration() {
    mockCurrentUser();

    AiModelConfigDto created = aiModelConfigService.create(new AiModelConfigUpsertRequest(
        "整改建议生成模型",
        "openai_compatible",
        "https://ai.example.com/v1",
        "gpt-4.1-mini",
        "sk-real-secret",
        "online",
        true,
        30,
        0.2,
        3000,
        "用于 OMS 工单审核建议"
    ));

    ArgumentCaptor<AiModelConfigEntity> captor = ArgumentCaptor.forClass(AiModelConfigEntity.class);
    verify(aiModelConfigMapper).insert(captor.capture());
    AiModelConfigEntity stored = captor.getValue();
    assertThat(stored.getApiKeyCipher()).isNotBlank();
    assertThat(stored.getApiKeyCipher()).doesNotContain("sk-real-secret");
    assertThat(stored.getProviderType()).isEqualTo("openai_compatible");
    assertThat(stored.getDefaultModel()).isEqualTo(1);
    assertThat(created.apiKeyConfigured()).isTrue();
  }

  @Test
  void testConnectionUsesDecryptedApiKeyAndReturnsClientResult() {
    mockCurrentUser();
    AiModelConfigDto created = aiModelConfigService.create(new AiModelConfigUpsertRequest(
        "默认模型",
        "openai_compatible",
        "https://ai.example.com/v1",
        "deepseek-chat",
        "sk-test-secret",
        "online",
        true,
        20,
        0.1,
        2000,
        null
    ));
    ArgumentCaptor<AiModelConfigEntity> insertCaptor = ArgumentCaptor.forClass(AiModelConfigEntity.class);
    verify(aiModelConfigMapper).insert(insertCaptor.capture());
    AiModelConfigEntity persisted = insertCaptor.getValue();
    persisted.setId(Long.valueOf(created.id()));
    when(aiModelConfigMapper.selectById(persisted.getId())).thenReturn(persisted);
    when(connectivityClient.test(any())).thenReturn(new LlmConnectivityClient.TestResult(true, "ok", 123L));

    var result = aiModelConfigService.testConnection(created.id());

    assertThat(result.success()).isTrue();
    assertThat(result.message()).isEqualTo("ok");
    ArgumentCaptor<LlmConnectivityClient.TestCommand> commandCaptor =
        ArgumentCaptor.forClass(LlmConnectivityClient.TestCommand.class);
    verify(connectivityClient).test(commandCaptor.capture());
    assertThat(commandCaptor.getValue().apiKey()).isEqualTo("sk-test-secret");
    assertThat(commandCaptor.getValue().baseUrl()).isEqualTo("https://ai.example.com/v1");
    assertThat(commandCaptor.getValue().modelName()).isEqualTo("deepseek-chat");
  }

  @Test
  void listReturnsOnlyTenantModelsWithoutApiKeyPlainText() {
    mockCurrentUser();
    AiModelConfigEntity entity = new AiModelConfigEntity();
    entity.setId(9001L);
    entity.setTenantId(1001L);
    entity.setModelName("qwen-plus");
    entity.setDisplayName("通义模型");
    entity.setProviderType("openai_compatible");
    entity.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
    entity.setApiKeyCipher("cipher-text");
    entity.setStatus("online");
    entity.setDefaultModel(0);
    entity.setTimeoutSeconds(30);
    when(aiModelConfigMapper.selectList(any())).thenReturn(List.of(entity));

    var page = aiModelConfigService.list(null, null, null, 1L, 10L);

    assertThat(page.getTotal()).isEqualTo(1);
    assertThat(page.getRecords().getFirst().apiKeyConfigured()).isTrue();
  }

  private void mockCurrentUser() {
    when(currentUserContext.requireCurrentUser()).thenReturn(new CurrentUserPrincipal(
        "token",
        2001L,
        1001L,
        "admin",
        "Platform Administrator",
        "IRIS",
        List.of("SUPER_ADMIN")
    ));
  }
}
