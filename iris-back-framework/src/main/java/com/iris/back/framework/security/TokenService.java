package com.iris.back.framework.security;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

  public String issueToken() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
