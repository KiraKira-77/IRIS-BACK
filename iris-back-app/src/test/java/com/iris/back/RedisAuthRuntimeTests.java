package com.iris.back;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.back.auth.model.CurrentUserResponse;
import com.iris.back.auth.model.LoginResponse;
import com.iris.back.common.model.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RedisAuthRuntimeTests {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void meRejectsAnonymousRequest() {
    ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/auth/me", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(response.getHeaders().getContentType()).isNotNull();
    assertThat(response.getHeaders().getContentType().toString()).contains("application/json");
    assertThat(response.getBody()).isNotBlank();
    assertThat(response.getBody()).contains("\"success\":false");
    assertThat(response.getBody()).contains("\"code\":\"UNAUTHORIZED\"");
  }

  @Test
  void loginRejectsInvalidPassword() {
    ResponseEntity<String> response = restTemplate.postForEntity(
        "/api/v1/auth/login",
        jsonBody("""
            {
              "account": "admin",
              "password": "wrong-password"
            }
            """),
        String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void secondLoginInvalidatesOldToken() {
    String firstToken = login("admin", "admin123").getBody().getData().token();
    String secondToken = login("admin", "admin123").getBody().getData().token();

    ResponseEntity<String> oldSession = authorizedGet("/api/v1/auth/me", firstToken);
    ResponseEntity<ApiResponse<CurrentUserResponse>> newSession = authorizedGet(
        "/api/v1/auth/me",
        secondToken,
        new ParameterizedTypeReference<>() {
        }
    );

    assertThat(firstToken).isNotEqualTo(secondToken);
    assertThat(oldSession.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(newSession.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(newSession.getBody()).isNotNull();
    assertThat(newSession.getBody().getData()).isNotNull();
    assertThat(newSession.getBody().getData().userId()).isEqualTo(2001L);
  }

  @Test
  void logoutInvalidatesCurrentToken() {
    String token = login("admin", "admin123").getBody().getData().token();

    ResponseEntity<String> logout = authorizedPost("/api/v1/auth/logout", token);
    ResponseEntity<String> afterLogout = authorizedGet("/api/v1/auth/me", token);

    assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  private ResponseEntity<ApiResponse<LoginResponse>> login(String account, String password) {
    return restTemplate.exchange(
        "/api/v1/auth/login",
        HttpMethod.POST,
        jsonBody("""
            {
              "account": "%s",
              "password": "%s"
            }
            """.formatted(account, password)),
        new ParameterizedTypeReference<>() {
        }
    );
  }

  private HttpEntity<String> jsonBody(String body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }

  private ResponseEntity<String> authorizedGet(String path, String token) {
    return restTemplate.exchange(path, HttpMethod.GET, bearer(token), String.class);
  }

  private <T> ResponseEntity<T> authorizedGet(String path, String token, ParameterizedTypeReference<T> type) {
    return restTemplate.exchange(path, HttpMethod.GET, bearer(token), type);
  }

  private ResponseEntity<String> authorizedPost(String path, String token) {
    return restTemplate.exchange(path, HttpMethod.POST, bearer(token), String.class);
  }

  private HttpEntity<Void> bearer(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    return new HttpEntity<>(headers);
  }
}
