package br.com.inpetu.hub.controller;

import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.ColaboradorRepository;
import br.com.inpetu.hub.service.CrudService;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CrudController {
  private final CrudService service;
  private final ColaboradorRepository colaboradores;

  public CrudController(CrudService s, ColaboradorRepository c) {
    service = s;
    colaboradores = c;
  }

  private void permitido(String tipo, boolean admin) {
    if (admin
            && !Set.of("personas", "precos", "feriados", "colaboradores", "configuracao")
                .contains(tipo)
        || !admin && !Set.of("empreendedores", "espacos", "atendimentos", "visitas").contains(tipo))
      throw new RegraException("Seção não encontrada.");
  }

  @GetMapping({"/admin/cadastros/{tipo}", "/admin/config/cadastros/{tipo}"})
  public String lista(
      @PathVariable String tipo,
      @RequestParam(defaultValue = "") String busca,
      jakarta.servlet.http.HttpServletRequest req,
      Model m) {
    boolean admin = req.getRequestURI().startsWith("/admin/config/");
    permitido(tipo, admin);
    String rota = admin ? "/admin/config/cadastros/" : "/admin/cadastros/";
    m.addAttribute("secao", service.secao(tipo));
    m.addAttribute("tipo", tipo);
    m.addAttribute("rota", rota + tipo);
    m.addAttribute("registros", service.listar(tipo, busca));
    m.addAttribute("busca", busca);
    m.addAttribute("crud", service);
    return "admin/crud/lista";
  }

  @GetMapping({"/admin/cadastros/{tipo}/editar", "/admin/config/cadastros/{tipo}/editar"})
  public String editar(
      @PathVariable String tipo,
      @RequestParam(required = false) Long id,
      jakarta.servlet.http.HttpServletRequest req,
      Model m) {
    boolean admin = req.getRequestURI().startsWith("/admin/config/");
    permitido(tipo, admin);
    m.addAttribute("secao", service.secao(tipo));
    m.addAttribute("tipo", tipo);
    m.addAttribute("id", id);
    m.addAttribute("registro", id == null ? null : service.obter(tipo, id));
    m.addAttribute("crud", service);
    m.addAttribute("rota", (admin ? "/admin/config/cadastros/" : "/admin/cadastros/") + tipo);
    return "admin/crud/form";
  }

  @PostMapping({"/admin/cadastros/{tipo}/salvar", "/admin/config/cadastros/{tipo}/salvar"})
  public String salvar(
      @PathVariable String tipo,
      @RequestParam(required = false) Long id,
      @RequestParam Map<String, String> dados,
      jakarta.servlet.http.HttpServletRequest req,
      Authentication a,
      RedirectAttributes f) {
    boolean admin = req.getRequestURI().startsWith("/admin/config/");
    permitido(tipo, admin);
    service.salvar(tipo, id, dados, colaboradores.findByLogin(a.getName()).orElseThrow());
    f.addFlashAttribute("sucesso", "Cadastro salvo.");
    return "redirect:" + (admin ? "/admin/config/cadastros/" : "/admin/cadastros/") + tipo;
  }

  @PostMapping({
    "/admin/cadastros/{tipo}/{id}/excluir",
    "/admin/config/cadastros/{tipo}/{id}/excluir"
  })
  public String excluir(
      @PathVariable String tipo,
      @PathVariable Long id,
      jakarta.servlet.http.HttpServletRequest req,
      RedirectAttributes f) {
    boolean admin = req.getRequestURI().startsWith("/admin/config/");
    permitido(tipo, admin);
    service.excluir(tipo, id);
    f.addFlashAttribute("sucesso", "Registro excluído.");
    return "redirect:" + (admin ? "/admin/config/cadastros/" : "/admin/cadastros/") + tipo;
  }
}
