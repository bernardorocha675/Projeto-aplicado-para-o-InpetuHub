package br.com.inpetu.hub.controller;

import br.com.inpetu.hub.entity.Colaborador;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.ColaboradorRepository;
import br.com.inpetu.hub.security.*;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Controller
public class AccountController {
  private final ColaboradorRepository users;
  private final PasswordEncoder encoder;
  private final AuditService audit;

  public AccountController(ColaboradorRepository u, PasswordEncoder e, AuditService a) {
    users = u;
    encoder = e;
    audit = a;
  }

  private Colaborador user(Authentication a) {
    return users.findByLogin(a.getName()).orElseThrow();
  }

  @GetMapping("/conta/senha")
  public String password() {
    return "auth/password";
  }

  @PostMapping("/conta/senha")
  @Transactional
  public String password(
      Authentication a,
      @RequestParam String current,
      @RequestParam String replacement,
      HttpServletRequest req) {
    var c = users.locked(user(a).id).orElseThrow();
    if (!encoder.matches(current, c.senha)) throw new RegraException("Credencial atual inválida.");
    if (replacement.length() < 16
        || replacement.length() > 64
        || replacement.getBytes(StandardCharsets.UTF_8).length > 72
        || encoder.matches(replacement, c.senha))
      throw new RegraException("Use uma nova senha de 16–64 caracteres (até 72 bytes).");
    c.senha = encoder.encode(replacement);
    c.securityVersion++;
    audit.record(c.id, "SENHA_ALTERADA", "colaboradores", c.id, "OK", null);
    req.getSession().invalidate();
    SecurityContextHolder.clearContext();
    return "redirect:/login?changed";
  }
}
