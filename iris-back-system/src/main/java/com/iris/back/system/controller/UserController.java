package com.iris.back.system.controller;

import com.iris.back.common.model.ApiResponse;
import com.iris.back.system.model.dto.UserDto;
import com.iris.back.system.model.request.UserUpsertRequest;
import com.iris.back.system.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public ApiResponse<List<UserDto>> list() {
    return ApiResponse.success(userService.list());
  }

  @GetMapping("/{id}")
  public ApiResponse<UserDto> get(@PathVariable Long id) {
    return ApiResponse.success(userService.get(id));
  }

  @PostMapping
  public ApiResponse<UserDto> create(@Valid @RequestBody UserUpsertRequest request) {
    return ApiResponse.success("user created", userService.create(request));
  }

  @PostMapping("/{id}/reset-password")
  public ApiResponse<Void> resetPassword(@PathVariable Long id) {
    userService.resetPassword(id);
    return ApiResponse.success("password reset", null);
  }

  @PutMapping("/{id}")
  public ApiResponse<UserDto> update(@PathVariable Long id, @Valid @RequestBody UserUpsertRequest request) {
    return ApiResponse.success("user updated", userService.update(id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Long id) {
    userService.delete(id);
    return ApiResponse.success("user deleted", null);
  }
}
