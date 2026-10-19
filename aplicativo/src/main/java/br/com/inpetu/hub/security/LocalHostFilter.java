package br.com.inpetu.hub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A instalação local só escuta no próprio computador. Recusar outros nomes de host impede que
 * uma página externa redirecione seu domínio para 127.0.0.1 (DNS rebinding) e use as contas
 * públicas de demonstração.
 */
@Component
@Profile("local")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalHostFilter extends OncePerRequestFilter {
  static final Set<String> LOCAL_NAMES = Set.of("127.0.0.1", "localhost", "[::1]", "::1");

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String host = req.getServerName();
    if (host == null || !LOCAL_NAMES.contains(host.toLowerCase(java.util.Locale.ROOT))) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.setContentType("text/plain;charset=UTF-8");
      res.getWriter().write("Endereço recusado. Abra o sistema pelo iniciador.");
      return;
    }
    chain.doFilter(req, res);
  }
}
