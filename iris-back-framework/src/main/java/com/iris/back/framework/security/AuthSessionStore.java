package com.iris.back.framework.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;

@Service
public class AuthSessionStore {

  private static final String TOKEN_KEY_PREFIX = "iris:auth:token:";
  private static final String USER_KEY_PREFIX = "iris:auth:user:";

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final Duration sessionTtl;

  public AuthSessionStore(
      StringRedisTemplate stringRedisTemplate,
      ObjectMapper objectMapper,
      @Value("${iris.security.session-ttl-hours:12}") long sessionTtlHours
  ) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
    this.sessionTtl = Duration.ofHours(sessionTtlHours);
  }

  public Duration getSessionTtl() {
    return sessionTtl;
  }

  public Optional<String> findActiveToken(Long tenantId, String account) {
    return Optional.ofNullable(stringRedisTemplate.opsForValue().get(userKey(tenantId, account)));
  }

  public Optional<AuthSession> findByToken(String token) {
    String payload = stringRedisTemplate.opsForValue().get(tokenKey(token));
    if (payload == null || payload.isBlank()) {
      return Optional.empty();
    }

    AuthSession session = deserialize(payload);
    if (session.expireAt().isBefore(Instant.now())) {
      invalidateByToken(token);
      return Optional.empty();
    }

    String activeToken = stringRedisTemplate.opsForValue().get(userKey(session.tenantId(), session.account()));
    if (!token.equals(activeToken)) {
      invalidateByToken(token);
      return Optional.empty();
    }

    return Optional.of(session);
  }

  public void save(AuthSession session) {
    Duration ttl = Duration.between(Instant.now(), session.expireAt());
    if (ttl.isNegative() || ttl.isZero()) {
      throw new IllegalArgumentException("session expireAt must be in the future");
    }

    String oldToken = stringRedisTemplate.opsForValue().get(userKey(session.tenantId(), session.account()));
    if (oldToken != null && !oldToken.equals(session.token())) {
      stringRedisTemplate.delete(tokenKey(oldToken));
    }

    String payload = serialize(session);
    stringRedisTemplate.opsForValue().set(tokenKey(session.token()), payload, ttl);
    stringRedisTemplate.opsForValue().set(userKey(session.tenantId(), session.account()), session.token(), ttl);
  }

  public void invalidateByToken(String token) {
    AuthSession session = readRawSession(token).orElse(null);
    stringRedisTemplate.delete(tokenKey(token));
    if (session == null) {
      return;
    }

    String userKey = userKey(session.tenantId(), session.account());
    String activeToken = stringRedisTemplate.opsForValue().get(userKey);
    if (token.equals(activeToken)) {
      stringRedisTemplate.delete(userKey);
    }
  }

  private Optional<AuthSession> readRawSession(String token) {
    String payload = stringRedisTemplate.opsForValue().get(tokenKey(token));
    if (payload == null || payload.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(deserialize(payload));
  }

  private AuthSession deserialize(String payload) {
    try {
      return objectMapper.readValue(payload, AuthSession.class);
    } catch (IOException ex) {
      throw new IllegalStateException("failed to deserialize auth session", ex);
    }
  }

  private String serialize(AuthSession session) {
    try {
      return objectMapper.writeValueAsString(session);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("failed to serialize auth session", ex);
    }
  }

  private String tokenKey(String token) {
    return TOKEN_KEY_PREFIX + token;
  }

  private String userKey(Long tenantId, String account) {
    return USER_KEY_PREFIX + tenantId + ":" + account;
  }
}
