package br.com.inpetu.hub.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import org.springframework.web.filter.OncePerRequestFilter;

public class AbuseFilter extends OncePerRequestFilter {
  private final RateLimits limits;
  private final SecretCrypto crypto;
  private final boolean prod;
  private final java.util.function.BooleanSupplier ready;

  public AbuseFilter(
      RateLimits l, SecretCrypto c, boolean p, java.util.function.BooleanSupplier ready) {
    limits = l;
    crypto = c;
    prod = p;
    this.ready = ready;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    if (!ready.getAsBoolean()) {
      res.setHeader("Retry-After", "1");
      res.sendError(503, "Inicialização em andamento.");
      return;
    }
    if (prod && !req.isSecure()) {
      res.sendError(426, "HTTPS obrigatório.");
      return;
    }
    String path = SecurityPaths.path(req);
    boolean post = req.getMethod().equals("POST");
    String ip = crypto.fingerprint("peer:" + req.getRemoteAddr());
    boolean allowed = true;
    if (post && path.equals("/login")) {
      String login = req.getParameter("username");
      if (login == null) login = "";
      if (login.length() > 255) {
        res.sendError(400);
        return;
      }
      allowed =
          limits.allow("login-ip:" + ip, 60, Duration.ofMinutes(15))
              && limits.allow(
                  "login-account:" + crypto.fingerprint(login.trim().toLowerCase(Locale.ROOT)),
                  10,
                  Duration.ofMinutes(15));
    } else if (path.startsWith("/reservas/") || path.equals("/solicitacoes/confirmar"))
      allowed =
          limits.allow((post ? "submit:" : "read:") + ip, post ? 15 : 120, Duration.ofMinutes(15));
    else if (path.equals("/disponibilidade"))
      allowed = limits.allow("calendar:" + ip, 120, Duration.ofMinutes(1));
    else if (post && path.startsWith("/conta/"))
      allowed = limits.allow("account:" + ip, 12, Duration.ofMinutes(5));
    if (!allowed) {
      res.setHeader("Retry-After", "900");
      res.sendError(429, "Muitas tentativas. Aguarde antes de repetir.");
      return;
    }
    chain.doFilter(req, res);
  }
}
