package br.com.inpetu.hub.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import br.com.inpetu.hub.observability.OperationalEvents;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler({
    RegraException.class,
    DataIntegrityViolationException.class,
    MethodArgumentTypeMismatchException.class,
    java.time.format.DateTimeParseException.class
  })
  public Object regra(Exception ex, Model m, HttpServletRequest req, HttpServletResponse res) {
    String msg =
        ex instanceof RegraException
            ? ex.getMessage()
            : ex instanceof DataIntegrityViolationException
                ? "Registro duplicado ou vinculado a um histórico. Confira os dados ou desative o"
                    + " cadastro."
                : "Parâmetro inválido. Confira as datas e opções.";
    int status = ex instanceof DataIntegrityViolationException ? 409 : 400;
    res.setStatus(status);
    OperationalEvents.problem(req.getMethod(), req.getRequestURI(), status, ex);
    m.addAttribute("requestId", org.slf4j.MDC.get("requestId"));
    if (req.getRequestURI().equals("/disponibilidade"))
      return ResponseEntity.status(status).body(java.util.Map.of("erro", msg));
    m.addAttribute("mensagem", msg);
    return "erro";
  }

  @ExceptionHandler(Exception.class)
  public Object inesperado(Exception ex, Model m, HttpServletRequest req, HttpServletResponse res)
      throws Exception {
    // Deixar a autenticação e a autorização sob controle do Spring Security.
    if (ex instanceof org.springframework.security.access.AccessDeniedException
        || ex instanceof org.springframework.security.core.AuthenticationException) throw ex;
    int status = ex instanceof org.springframework.web.ErrorResponse error
        ? error.getStatusCode().value() : 500;
    res.setStatus(status);
    if (ex instanceof org.springframework.web.ErrorResponse error)
      error.getHeaders().forEach((name, values) -> values.forEach(value -> res.addHeader(name, value)));
    OperationalEvents.problem(req.getMethod(), req.getRequestURI(), status, ex);
    String msg = status >= 500 ? "Não foi possível concluir. Tente novamente."
        : status == 404 ? "Página ou recurso não encontrado." : "Confira os dados e tente novamente.";
    m.addAttribute("mensagem", msg);
    m.addAttribute("requestId", org.slf4j.MDC.get("requestId"));
    if (req.getRequestURI().equals("/disponibilidade"))
      return ResponseEntity.status(status).body(java.util.Map.of("erro", msg));
    return status >= 500 ? "error/500" : status == 404 ? "error/404" : "erro";
  }
}
