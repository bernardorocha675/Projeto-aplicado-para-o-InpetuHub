package br.com.inpetu.hub.security;

import br.com.inpetu.hub.enums.Perfil;
import br.com.inpetu.hub.repository.ColaboradorRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Clock;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionGuardFilter extends OncePerRequestFilter {
  private final ColaboradorRepository users;
  private final AuditService audit;
  private final Clock clock;

  public SessionGuardFilter(ColaboradorRepository u, AuditService a, Clock c) {
    users = u;
    audit = a;
    clock = c;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof AccountPrincipal p) {
      var c = users.findById(p.id()).orElse(null);
      var session = req.getSession(false);
      Long started = session == null ? null : (Long) session.getAttribute("auth.started");
      if (c == null
          || !c.ativo
          || c.securityVersion != p.version()
          || !c.login.equals(p.getUsername())
          || !c.senha.equals(p.getPassword())
          || p.getAuthorities().stream()
              .noneMatch(g -> g.getAuthority().equals("ROLE_" + c.perfil.name()))
          || started == null
          || clock.millis() - started > 7200000) {
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        audit.security(p.id(), "SESSAO_REVOGADA", "NEGADO");
        res.sendRedirect("/login?expired");
        return;
      }
      String path = SecurityPaths.path(req);
      boolean internal =
          !(path.startsWith("/css/")
              || path.startsWith("/js/")
              || path.startsWith("/images/")
              || java.util.Set.of("/conta/mfa", "/logout", "/error", "/login").contains(path));
      boolean needs = c.perfil == Perfil.ADMIN || c.mfaSecret != null;
      boolean verified = Boolean.TRUE.equals(session.getAttribute("mfa.verified"));
      if (needs && !verified && clock.millis() - started > 300000) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        res.sendRedirect("/login?expired");
        return;
      }
      if (needs && !verified && internal) {
        if (clock.millis() - started > 300000) {
          session.invalidate();
          SecurityContextHolder.clearContext();
          res.sendRedirect("/login?expired");
        } else res.sendRedirect("/conta/mfa");
        return;
      }
    }
    chain.doFilter(req, res);
  }
}
