package com.iris.back.auth.controller;

import com.iris.back.auth.model.CurrentUserResponse;
import com.iris.back.auth.model.LoginRequest;
import com.iris.back.auth.model.LoginResponse;
import com.iris.back.auth.service.AuthService;
import com.iris.back.common.model.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.success("login success", authService.login(request));
  }

  @GetMapping("/me")
  public ApiResponse<CurrentUserResponse> currentUser() {
    return ApiResponse.success(authService.currentUser());
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    authService.logout();
    return ApiResponse.success("logout success", null);
  }
}
