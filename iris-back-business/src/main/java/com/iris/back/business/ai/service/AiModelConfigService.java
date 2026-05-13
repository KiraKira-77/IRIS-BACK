package com.iris.back.business.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.iris.back.business.ai.mapper.AiModelConfigMapper;
import com.iris.back.business.ai.model.dto.AiModelConfigDto;
import com.iris.back.business.ai.model.dto.AiModelTestResultDto;
import com.iris.back.business.ai.model.entity.AiModelConfigEntity;
import com.iris.back.business.ai.model.request.AiModelConfigUpsertRequest;
import com.iris.back.common.exception.BusinessException;
import com.iris.back.common.model.PageResponse;
import com.iris.back.common.util.DateTimeFormatters;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiModelConfigService {

  private static final String PROVIDER_OPENAI_COMPATIBLE = "openai_compatible";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final AiModelConfigMapper aiModelConfigMapper;
  private final CurrentUserContext currentUserContext;
  private final LlmConnectivityClient connectivityClient;
  private final String secretKey;

  public AiModelConfigService(
      AiModelConfigMapper aiModelConfigMapper,
      CurrentUserContext currentUserContext,
      LlmConnectivityClient connectivityClient,
      @Value("${iris.ai.secret-key:${IRIS_AI_SECRET_KEY:iris-dev-ai-secret}}") String secretKey
  ) {
    this.aiModelConfigMapper = aiModelConfigMapper;
    this.currentUserContext = currentUserContext;
    this.connectivityClient = connectivityClient;
    this.secretKey = secretKey;
  }

  public PageResponse<AiModelConfigDto> list(
      String keyword,
      String providerType,
      String status,
      Long page,
      Long pageSize
  ) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    String normalizedKeyword = trimToNull(keyword);
    String normalizedProviderType = trimToNull(providerType);
    String normalizedStatus = trimToNull(status);
    List<AiModelConfigDto> filtered = nullToList(aiModelConfigMapper.selectList(
            new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getTenantId, principal.tenantId())
                .orderByDesc(AiModelConfigEntity::getUpdatedAt)
                .orderByDesc(AiModelConfigEntity::getId)))
        .stream()
        .filter(entity -> !Objects.equals(entity.getDeleted(), 1))
        .filter(entity -> normalizedProviderType == null || normalizedProviderType.equals(entity.getProviderType()))
        .filter(entity -> normalizedStatus == null || normalizedStatus.equals(entity.getStatus()))
        .filter(entity -> normalizedKeyword == null
            || containsIgnoreCase(entity.getDisplayName(), normalizedKeyword)
            || containsIgnoreCase(entity.getModelName(), normalizedKeyword)
            || containsIgnoreCase(entity.getProviderName(), normalizedKeyword)
            || containsIgnoreCase(entity.getBaseUrl(), normalizedKeyword))
        .sorted(Comparator
            .comparing(AiModelConfigEntity::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(AiModelConfigEntity::getId, Comparator.nullsLast(Comparator.reverseOrder())))
        .map(this::toDto)
        .toList();
    long pageNo = normalizedPage(page);
    long size = normalizedPageSize(pageSize);
    int fromIndex = (int) Math.min(filtered.size(), (pageNo - 1) * size);
    int toIndex = (int) Math.min(filtered.size(), fromIndex + size);
    return PageResponse.of(filtered.size(), pageNo, size, filtered.subList(fromIndex, toIndex));
  }

  @Transactional
  public AiModelConfigDto create(AiModelConfigUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    AiModelConfigEntity entity = new AiModelConfigEntity();
    entity.setId(nextId());
    entity.setTenantId(principal.tenantId());
    applyRequest(entity, request, true);
    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    entity.setCreatedBy(principal.userId());
    entity.setUpdatedBy(principal.userId());
    entity.setDeleted(0);
    entity.setVersion(0L);
    if (flag(entity.getDefaultModel())) {
      clearTenantDefault(principal.tenantId(), null);
    }
    aiModelConfigMapper.insert(entity);
    return toDto(entity);
  }

  @Transactional
  public AiModelConfigDto update(String id, AiModelConfigUpsertRequest request) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    AiModelConfigEntity entity = requireModel(parseId(id), principal.tenantId());
    applyRequest(entity, request, false);
    entity.setUpdatedAt(LocalDateTime.now());
    entity.setUpdatedBy(principal.userId());
    if (flag(entity.getDefaultModel())) {
      clearTenantDefault(principal.tenantId(), entity.getId());
    }
    aiModelConfigMapper.updateById(entity);
    return toDto(entity);
  }

  @Transactional
  public void delete(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    AiModelConfigEntity entity = requireModel(parseId(id), principal.tenantId());
    aiModelConfigMapper.deleteById(entity.getId());
  }

  public AiModelTestResultDto testConnection(String id) {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    AiModelConfigEntity entity = requireModel(parseId(id), principal.tenantId());
    String apiKey = decryptApiKey(normalizeRequiredText(entity.getApiKeyCipher(), "AI_MODEL_API_KEY_REQUIRED"));
    LlmConnectivityClient.TestResult result = connectivityClient.test(new LlmConnectivityClient.TestCommand(
        entity.getProviderType(),
        entity.getBaseUrl(),
        entity.getModelName(),
        apiKey,
        entity.getTimeoutSeconds(),
        entity.getTemperature(),
        entity.getMaxTokens()
    ));
    return new AiModelTestResultDto(result.success(), result.message(), result.latencyMs());
  }

  private void applyRequest(AiModelConfigEntity entity, AiModelConfigUpsertRequest request, boolean create) {
    entity.setDisplayName(normalizeRequiredText(request.name(), "AI_MODEL_NAME_REQUIRED"));
    String providerType = normalizeProviderType(request.providerType());
    entity.setProviderType(providerType);
    entity.setProviderName(providerName(providerType));
    entity.setBaseUrl(trimTrailingSlash(normalizeRequiredText(request.baseUrl(), "AI_MODEL_BASE_URL_REQUIRED")));
    entity.setModelName(normalizeRequiredText(request.modelName(), "AI_MODEL_MODEL_NAME_REQUIRED"));
    String apiKey = trimToNull(request.apiKey());
    if (apiKey != null) {
      entity.setApiKeyCipher(encryptApiKey(apiKey));
    } else if (create) {
      throw new BusinessException("AI_MODEL_API_KEY_REQUIRED", "AI_MODEL_API_KEY_REQUIRED");
    }
    entity.setStatus(normalizeStatus(request.status()));
    entity.setDefaultModel(Boolean.TRUE.equals(request.defaultModel()) ? 1 : 0);
    entity.setTimeoutSeconds(normalizeTimeout(request.timeoutSeconds()));
    entity.setTemperature(request.temperature() == null ? 0.2 : request.temperature());
    entity.setMaxTokens(request.maxTokens() == null ? 3000 : request.maxTokens());
    entity.setRemark(trimToNull(request.remark()));
  }

  private void clearTenantDefault(Long tenantId, Long exceptId) {
    // 同一个租户只能保留一个默认模型，新增或修改默认模型时需要先清掉旧默认值。
    UpdateWrapper<AiModelConfigEntity> wrapper = new UpdateWrapper<AiModelConfigEntity>()
        .eq("tenant_id", tenantId)
        .set("default_model", 0);
    if (exceptId != null) {
      wrapper.ne("id", exceptId);
    }
    aiModelConfigMapper.update(null, wrapper);
  }

  private AiModelConfigEntity requireModel(Long id, Long tenantId) {
    AiModelConfigEntity entity = aiModelConfigMapper.selectById(id);
    if (entity == null || !Objects.equals(entity.getTenantId(), tenantId) || Objects.equals(entity.getDeleted(), 1)) {
      throw new BusinessException("AI_MODEL_NOT_FOUND", "AI_MODEL_NOT_FOUND");
    }
    return entity;
  }

  private AiModelConfigDto toDto(AiModelConfigEntity entity) {
    return new AiModelConfigDto(
        String.valueOf(entity.getId()),
        entity.getDisplayName(),
        "llm",
        entity.getProviderType(),
        entity.getProviderName(),
        entity.getBaseUrl(),
        entity.getModelName(),
        trimToNull(entity.getApiKeyCipher()) != null,
        entity.getStatus(),
        flag(entity.getDefaultModel()),
        entity.getTimeoutSeconds(),
        entity.getTemperature(),
        entity.getMaxTokens(),
        entity.getRemark(),
        DateTimeFormatters.formatDateTime(entity.getCreatedAt()),
        DateTimeFormatters.formatDateTime(entity.getUpdatedAt())
    );
  }

  private String encryptApiKey(String apiKey) {
    try {
      byte[] iv = new byte[12];
      SECURE_RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey(), "AES"), new GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
      buffer.put(iv);
      buffer.put(encrypted);
      return Base64.getEncoder().encodeToString(buffer.array());
    } catch (Exception exception) {
      throw new BusinessException("AI_MODEL_API_KEY_ENCRYPT_FAILED", "AI_MODEL_API_KEY_ENCRYPT_FAILED");
    }
  }

  private String decryptApiKey(String cipherText) {
    try {
      byte[] payload = Base64.getDecoder().decode(cipherText);
      byte[] iv = Arrays.copyOfRange(payload, 0, 12);
      byte[] encrypted = Arrays.copyOfRange(payload, 12, payload.length);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey(), "AES"), new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new BusinessException("AI_MODEL_API_KEY_DECRYPT_FAILED", "AI_MODEL_API_KEY_DECRYPT_FAILED");
    }
  }

  private byte[] encryptionKey() throws Exception {
    return Arrays.copyOf(MessageDigest.getInstance("SHA-256")
        .digest(secretKey.getBytes(StandardCharsets.UTF_8)), 16);
  }

  private Long nextId() {
    return Math.abs(UUID.randomUUID().getMostSignificantBits());
  }

  private String normalizeProviderType(String value) {
    String normalized = normalizeRequiredText(value, "AI_MODEL_PROVIDER_TYPE_REQUIRED").toLowerCase(Locale.ROOT);
    if (!PROVIDER_OPENAI_COMPATIBLE.equals(normalized)) {
      throw new BusinessException("AI_MODEL_PROVIDER_TYPE_UNSUPPORTED", "AI_MODEL_PROVIDER_TYPE_UNSUPPORTED");
    }
    return normalized;
  }

  private String providerName(String providerType) {
    return PROVIDER_OPENAI_COMPATIBLE.equals(providerType) ? "OpenAI Compatible" : providerType;
  }

  private String normalizeStatus(String status) {
    String normalized = trimToNull(status);
    if (normalized == null) {
      return "online";
    }
    if (!List.of("online", "offline").contains(normalized)) {
      throw new BusinessException("AI_MODEL_STATUS_INVALID", "AI_MODEL_STATUS_INVALID");
    }
    return normalized;
  }

  private Integer normalizeTimeout(Integer timeoutSeconds) {
    if (timeoutSeconds == null) {
      return 30;
    }
    return Math.min(120, Math.max(1, timeoutSeconds));
  }

  private Long parseId(String id) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException exception) {
      throw new BusinessException("AI_MODEL_ID_INVALID", "AI_MODEL_ID_INVALID");
    }
  }

  private String normalizeRequiredText(String value, String code) {
    String normalized = trimToNull(value);
    if (normalized == null) {
      throw new BusinessException(code, code);
    }
    return normalized;
  }

  private String trimTrailingSlash(String value) {
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private boolean flag(Integer value) {
    return Integer.valueOf(1).equals(value);
  }

  private boolean containsIgnoreCase(String value, String keyword) {
    return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
  }

  private long normalizedPage(Long page) {
    return page == null || page < 1 ? 1 : page;
  }

  private long normalizedPageSize(Long pageSize) {
    if (pageSize == null || pageSize < 1) {
      return 10;
    }
    return Math.min(pageSize, 100);
  }

  private <T> List<T> nullToList(List<T> values) {
    return values == null ? List.of() : values;
  }
}
