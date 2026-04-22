package com.iris.back.framework.security;

import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserContext {

  public CurrentUserPrincipal requireCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
      throw new InsufficientAuthenticationException("authentication required");
    }
    return principal;
  }

  public String requireToken() {
    return requireCurrentUser().token();
  }
}
