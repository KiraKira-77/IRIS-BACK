package com.iris.back.auth.service;

import com.iris.back.auth.model.CurrentUserResponse;
import com.iris.back.auth.model.LoginRequest;
import com.iris.back.auth.model.LoginResponse;
import com.iris.back.framework.security.AuthSession;
import com.iris.back.framework.security.AuthSessionStore;
import com.iris.back.framework.security.CurrentUserContext;
import com.iris.back.framework.security.CurrentUserPrincipal;
import com.iris.back.framework.security.TokenService;
import com.iris.back.system.model.dto.AuthUserView;
import com.iris.back.system.service.AuthUserQueryService;
import java.time.Instant;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AuthUserQueryService authUserQueryService;
  private final AuthSessionStore authSessionStore;
  private final CurrentUserContext currentUserContext;
  private final TokenService tokenService;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      AuthUserQueryService authUserQueryService,
      AuthSessionStore authSessionStore,
      CurrentUserContext currentUserContext,
      TokenService tokenService,
      PasswordEncoder passwordEncoder
  ) {
    this.authUserQueryService = authUserQueryService;
    this.authSessionStore = authSessionStore;
    this.currentUserContext = currentUserContext;
    this.tokenService = tokenService;
    this.passwordEncoder = passwordEncoder;
  }

  public LoginResponse login(LoginRequest request) {
    AuthUserView user = authUserQueryService.findByAccount(request.getAccount().trim())
        .orElseThrow(this::invalidCredentials);
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw invalidCredentials();
    }

    Instant loginTime = Instant.now();
    AuthSession session = new AuthSession(
        tokenService.issueToken(),
        user.getUserId(),
        user.getTenantId(),
        user.getAccount(),
        user.getUsername(),
        user.getTenantName(),
        user.getRoles(),
        loginTime,
        loginTime.plus(authSessionStore.getSessionTtl())
    );
    authSessionStore.save(session);

    return new LoginResponse(
        session.token(),
        user.getUserId(),
        user.getTenantId(),
        user.getUsername(),
        user.getTenantName()
    );
  }

  public CurrentUserResponse currentUser() {
    CurrentUserPrincipal principal = currentUserContext.requireCurrentUser();
    return new CurrentUserResponse(
        principal.userId(),
        principal.tenantId(),
        principal.account(),
        principal.username(),
        principal.tenantName(),
        principal.roles()
    );
  }

  public void logout() {
    authSessionStore.invalidateByToken(currentUserContext.requireToken());
  }

  private BadCredentialsException invalidCredentials() {
    return new BadCredentialsException("invalid account or password");
  }


}
