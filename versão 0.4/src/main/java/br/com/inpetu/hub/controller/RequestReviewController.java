package br.com.inpetu.hub.controller;

import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.service.PublicRequestService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class RequestReviewController {
  private final PublicRequestService service;
  private final SolicitacaoPublicaRepository repo;
  private final ColaboradorRepository users;
  private final AuditoriaRepository audit;

  public RequestReviewController(
      PublicRequestService s,
      SolicitacaoPublicaRepository r,
      ColaboradorRepository u,
      AuditoriaRepository a) {
    service = s;
    repo = r;
    users = u;
    audit = a;
  }

  @GetMapping("/solicitacoes/confirmar")
  public String confirm(HttpSession session, Model model) {
    model.addAttribute("confirmed", Boolean.TRUE.equals(session.getAttribute("request.confirmed")));
    return "public/confirmar";
  }

  @PostMapping("/solicitacoes/confirmar")
  public String confirm(HttpSession session, @RequestParam String code) {
    Object reference = session.getAttribute("request.reference");
    if (!(reference instanceof String r) || !service.confirm(r, code))
      throw new RegraException(
          "Código inválido ou expirado. A equipe também pode conferir sua solicitação.");
    session.setAttribute("request.confirmed", true);
    return "redirect:/solicitacoes/confirmar";
  }

  @GetMapping("/admin/solicitacoes")
  public String list(Model model) {
    model.addAttribute("items", repo.findTop200ByEstadoOrderByCriadaEmAsc("RECEBIDA"));
    return "admin/solicitacoes";
  }

  @PostMapping("/admin/solicitacoes/{id}/aceitar")
  public String accept(
      @PathVariable Long id,
      Authentication a,
      @RequestParam(defaultValue = "false") boolean identity,
      @RequestParam String reason) {
    Long r = service.accept(id, users.findByLogin(a.getName()).orElseThrow(), identity, reason);
    return "redirect:/admin/reservas/" + r;
  }

  @PostMapping("/admin/solicitacoes/{id}/recusar")
  public String reject(@PathVariable Long id, Authentication a) {
    service.reject(id, users.findByLogin(a.getName()).orElseThrow());
    return "redirect:/admin/solicitacoes";
  }

  @GetMapping("/admin/config/auditoria")
  public String audit(Model model) {
    model.addAttribute("items", audit.findTop200ByOrderByInstanteDesc());
    return "admin/auditoria";
  }
}
