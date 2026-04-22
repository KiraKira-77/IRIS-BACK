package com.iris.back;

import static org.assertj.core.api.Assertions.assertThat;

import com.iris.back.common.model.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VersionRuntimeSecurityTests {

  @Autowired
  private TestRestTemplate restTemplate;

  @Test
  void versionEndpointIsAccessibleWithoutAuthenticationInRunningServer() {
    ResponseEntity<ApiResponse<VersionResponse>> response = restTemplate.exchange(
        "/api/version",
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<>() {
        }
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isSuccess()).isTrue();
    assertThat(response.getBody().getData()).isNotNull();
    assertThat(response.getBody().getData().service()).isEqualTo("iris-back");
  }
}
