package com.iris.back.framework.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

  private final AuthSessionStore authSessionStore;

  public TokenAuthenticationFilter(AuthSessionStore authSessionStore) {
    this.authSessionStore = authSessionStore;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    String token = resolveToken(request);
    if (token != null) {
      authSessionStore.findByToken(token).ifPresent(session -> {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
            session.token(),
            session.userId(),
            session.tenantId(),
            session.account(),
            session.username(),
            session.tenantName(),
            session.roles()
        );
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            session.token(),
            session.roles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
      });
    }

    filterChain.doFilter(request, response);
  }

  private String resolveToken(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return null;
    }
    String token = authorization.substring("Bearer ".length()).trim();
    return token.isEmpty() ? null : token;
  }
}
