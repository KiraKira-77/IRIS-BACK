package com.iris.back.auth.model;

import jakarta.validation.constraints.NotBlank;
public class LoginRequest {

  @NotBlank(message = "must not be blank")
  private String account;

  @NotBlank(message = "must not be blank")
  private String password;

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
