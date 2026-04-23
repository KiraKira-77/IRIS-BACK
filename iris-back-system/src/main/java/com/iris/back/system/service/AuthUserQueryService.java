package com.iris.back.system.service;

import com.iris.back.system.mapper.SysUserMapper;
import com.iris.back.system.model.dto.AuthUserView;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthUserQueryService {

  private final SysUserMapper sysUserMapper;

  public AuthUserQueryService(SysUserMapper sysUserMapper) {
    this.sysUserMapper = sysUserMapper;
  }

  public Optional<AuthUserView> findByAccount(String account) {
    AuthUserView user = sysUserMapper.selectAuthUserByAccount(account);
    if (user == null) {
      return Optional.empty();
    }
    user.setRoles(sysUserMapper.selectRoleCodesByUserId(user.getTenantId(), user.getUserId()));
    return Optional.of(user);
  }

    public static void main(String[] args) {
        System.out.println(new BCryptPasswordEncoder().encode("admin123"));
    }
}
