package br.com.inpetu.hub.security;

import br.com.inpetu.hub.entity.Colaborador;
import java.util.*;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AccountPrincipal implements UserDetails {
  private final Long id;
  private final long version;
  private final String login, hash, role;
  private final boolean active;

  public AccountPrincipal(Colaborador c) {
    id = c.id;
    version = c.securityVersion;
    login = c.login;
    hash = c.senha;
    role = c.perfil.name();
    active = c.ativo;
  }

  public Long id() {
    return id;
  }

  public long version() {
    return version;
  }

  public String getUsername() {
    return login;
  }

  public String getPassword() {
    return hash;
  }

  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role));
  }

  public boolean isEnabled() {
    return active;
  }
}
