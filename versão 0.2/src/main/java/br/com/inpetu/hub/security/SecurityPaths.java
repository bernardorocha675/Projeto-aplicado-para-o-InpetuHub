package br.com.inpetu.hub.security;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UriUtils;

/** Usa o caminho decodificado que o container entrega ao roteador, não o URI bruto do cliente. */
final class SecurityPaths {
  private SecurityPaths() {}

  static String path(HttpServletRequest request) {
    String servlet = request.getServletPath();
    return !servlet.isEmpty()
        ? servlet
        : UriUtils.decode(
            request.getRequestURI().substring(request.getContextPath().length()),
            StandardCharsets.UTF_8);
  }
}
