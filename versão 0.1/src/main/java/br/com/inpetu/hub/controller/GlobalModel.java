package br.com.inpetu.hub.controller;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalModel {
  @ModelAttribute("gestorGlobal")
  public boolean gestor() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a != null
        && a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
  }
}
