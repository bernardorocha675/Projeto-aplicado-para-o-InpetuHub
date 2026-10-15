package br.com.inpetu.hub.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLogFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String previous = MDC.get("requestId");
    String id = UUID.randomUUID().toString();
    MDC.put("requestId", id);
    response.setHeader("X-Request-ID", id);
    long start = System.nanoTime();
    Throwable failure = null;
    try {
      chain.doFilter(request, response);
    } catch (IOException | ServletException | RuntimeException | Error ex) {
      failure = ex;
      throw ex;
    } finally {
      try {
        OperationalEvents.request(
            request.getMethod(),
            failure == null ? response.getStatus() : 500,
            (System.nanoTime() - start) / 1_000_000,
            failure);
      } finally {
        if (previous == null) MDC.remove("requestId");
        else MDC.put("requestId", previous);
      }
    }
  }
}
