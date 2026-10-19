package br.com.inpetu.hub.controller;

import br.com.inpetu.hub.repository.SolicitacaoPublicaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalModel {
  private final SolicitacaoPublicaRepository solicitacoes;

  public GlobalModel(SolicitacaoPublicaRepository solicitacoes) {
    this.solicitacoes = solicitacoes;
  }

  @ModelAttribute("gestorGlobal")
  public boolean gestor() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a != null
        && a.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
  }

  @ModelAttribute("equipeLogada")
  public boolean equipe() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken);
  }

  // Item do menu principal destacado conforme a página atual.
  @ModelAttribute("secaoAtual")
  public String secao(HttpServletRequest req) {
    String path = req.getRequestURI();
    if (path.equals("/")) return "inicio";
    if (path.startsWith("/admin") || path.startsWith("/conta") || path.startsWith("/login"))
      return "interna";
    return "agenda";
  }

  // Consulta somente nas páginas internas, para o aviso do menu lateral.
  @ModelAttribute("solicitacoesPendentes")
  public boolean pendentes(HttpServletRequest req) {
    return req.getRequestURI().startsWith("/admin")
        && equipe()
        && solicitacoes.existsByEstado("RECEBIDA");
  }
}
