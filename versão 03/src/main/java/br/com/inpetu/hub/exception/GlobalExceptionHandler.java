package br.com.inpetu.hub.exception;

import jakarta.servlet.http.HttpServletRequest;
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
  public Object regra(Exception ex, Model m, HttpServletRequest req) {
    String msg =
        ex instanceof RegraException
            ? ex.getMessage()
            : ex instanceof DataIntegrityViolationException
                ? "Registro duplicado ou vinculado a um histórico. Confira os dados ou desative o"
                    + " cadastro."
                : "Parâmetro inválido. Confira as datas e opções.";
    if (req.getRequestURI().equals("/disponibilidade"))
      return ResponseEntity.badRequest().body(java.util.Map.of("erro", msg));
    m.addAttribute("mensagem", msg);
    return "erro";
  }
}
