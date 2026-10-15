package br.com.inpetu.hub.controller;

import br.com.inpetu.hub.entity.Colaborador;
import br.com.inpetu.hub.enums.Perfil;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.ColaboradorRepository;
import br.com.inpetu.hub.security.*;
import jakarta.servlet.http.*;
import java.nio.charset.StandardCharsets;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AccountController {
  private final ColaboradorRepository users;
  private final TotpService totp;
  private final MfaService mfa;
  private final PasswordEncoder encoder;
  private final AuditService audit;

  public AccountController(
      ColaboradorRepository u, TotpService t, MfaService m, PasswordEncoder e, AuditService a) {
    users = u;
    totp = t;
    mfa = m;
    encoder = e;
    audit = a;
  }

  private Colaborador user(Authentication a) {
    return users.findByLogin(a.getName()).orElseThrow();
  }

  @GetMapping("/conta/mfa")
  public String mfa(Authentication a, HttpSession session, Model model) {
    var c = user(a);
    model.addAttribute("configured", c.mfaSecret != null);
    if (c.mfaSecret == null) {
      String secret = (String) session.getAttribute("mfa.enrollment");
      if (secret == null) {
        secret = totp.generate();
        session.setAttribute("mfa.enrollment", secret);
      }
      model.addAttribute("secret", secret);
    }
    model.addAttribute("recovery", session.getAttribute("mfa.recovery"));
    session.removeAttribute("mfa.recovery");
    return "auth/mfa";
  }

  @PostMapping("/conta/mfa")
  public String verify(Authentication a, @RequestParam String code, HttpServletRequest req) {
    var c = user(a);
    var session = req.getSession();
    if (c.mfaSecret == null) {
      String secret = (String) session.getAttribute("mfa.enrollment");
      if (secret == null) throw new RegraException("Inicie a configuração do segundo fator.");
      var codes = mfa.enroll(c.id, secret, code);
      session.removeAttribute("mfa.enrollment");
      session.setAttribute("mfa.recovery", codes);
      var fresh = new AccountPrincipal(users.findById(c.id).orElseThrow());
      var auth =
          UsernamePasswordAuthenticationToken.authenticated(fresh, null, fresh.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(auth);
      session.setAttribute(
          HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
          SecurityContextHolder.getContext());
      req.changeSessionId();
      session.setAttribute("mfa.verified", true);
      audit.security(c.id, "MFA_LOGIN", "OK");
      return "redirect:/conta/mfa";
    }
    if (!mfa.verify(c.id, code)) {
      audit.security(c.id, "MFA_LOGIN", "NEGADO");
      throw new RegraException("Código inválido, expirado ou já utilizado.");
    }
    req.changeSessionId();
    session.setAttribute("mfa.verified", true);
    audit.security(c.id, "MFA_LOGIN", "OK");
    return "redirect:/admin";
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

  @PostMapping("/admin/config/mfa-reset")
  @Transactional
  public String reset(Authentication a, @RequestParam Long id, @RequestParam String current) {
    var actor = user(a);
    if (actor.perfil != Perfil.ADMIN || !encoder.matches(current, actor.senha))
      throw new RegraException("Reautenticação inválida.");
    var c = users.locked(id).orElseThrow();
    c.mfaSecret = null;
    c.mfaRecovery = null;
    c.mfaCounter = -1;
    c.securityVersion++;
    audit.record(actor.id, "MFA_RESET", "colaboradores", c.id, "OK", null);
    return "redirect:/admin/config/cadastros/colaboradores";
  }
}
