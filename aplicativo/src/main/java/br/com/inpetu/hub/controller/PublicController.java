package br.com.inpetu.hub.controller;

import br.com.inpetu.hub.dto.*;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.validation.annotation.Validated;
import java.time.*;
import java.util.*;
import br.com.inpetu.hub.validation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PublicController {
  private final EspacoRepository espacos;
  private final PersonaRepository personas;
  private final ReservaRepository reservas;
  private final ConfiguracaoRepository config;
  private final FeriadoRepository feriados;
  private final ReservaService service;
  private final Clock clock;
  private final PublicRequestService requests;

  public PublicController(
      EspacoRepository e,
      PersonaRepository p,
      ReservaRepository r,
      ConfiguracaoRepository c,
      FeriadoRepository f,
      ReservaService s,
      Clock clock,
      PublicRequestService requests) {
    this.requests = requests;
    espacos = e;
    personas = p;
    reservas = r;
    config = c;
    feriados = f;
    service = s;
    this.clock = clock;
  }

  @GetMapping("/")
  public String home() {
    return "public/home";
  }

  @GetMapping("/agenda")
  public String agenda(Model m) {
    m.addAttribute("today", LocalDate.now(clock));
    m.addAttribute("maxDate", ReservationDates.lastDay(clock));
    m.addAttribute("espacos", espacos.findByAtivoTrueAndDisponibilidadeTrueOrderByNomeEspaco());
    return "public/agenda";
  }

  @GetMapping("/login")
  public String login() {
    return "auth/login";
  }

  @GetMapping("/reserva")
  public String form(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
      @RequestParam(required = false) Long espacoId,
      @RequestParam(required = false) LocalTime horaInicio,
      @RequestParam(required = false) LocalTime horaFim,
      @RequestParam(defaultValue = "false") boolean editar,
      HttpSession session,
      Model m) {
    if (editar && session.getAttribute("solicitacao") instanceof ReservaForm draft) {
      formModel(m,draft);
      return "public/reserva-form";
    }
    ReservaForm f = new ReservaForm();
    f.data = data;
    f.espacoId = espacoId;
    f.horaInicio = horaInicio;
    f.horaFim = horaFim;
    formModel(m, f);
    return "public/reserva-form";
  }

  private void formModel(Model m, ReservaForm f) {
    m.addAttribute("minDate", LocalDate.now(clock));
    m.addAttribute("maxDate", ReservationDates.lastDay(clock));
    m.addAttribute("contactPatterns", ContactRules.patterns());
    m.addAttribute("form", f);
    m.addAttribute("espacos", espacos.findByAtivoTrueAndDisponibilidadeTrueOrderByNomeEspaco());
    m.addAttribute("personas", personas.findAll());
  }

  @PostMapping("/reservas/resumo")
  public String resumo(
      @Validated({jakarta.validation.groups.Default.class, ContactRules.Input.class})
      @ModelAttribute("form") ReservaForm f,
      BindingResult erros,
      Model m,
      HttpSession session,
      jakarta.servlet.http.HttpServletResponse response) {
    formModel(m, f);
    if (!erros.hasFieldErrors("telefone")) {
      try {
        f.telefone = Telefones.normalizar(f.telefonePais, f.telefone);
      } catch (RegraException ex) {
        erros.rejectValue("telefone", "telefone", ex.getMessage());
      }
    }
    if (erros.hasErrors()) {
      response.setStatus(400);
      br.com.inpetu.hub.observability.OperationalEvents.validation("/reservas/resumo",
          erros.getFieldErrors().stream().anyMatch(e -> e.isBindingFailure()));
      m.addAttribute("erro", "Confira os campos indicados. A data deve estar entre hoje e o fim do próximo ano.");
      m.addAttribute("fieldErrors", erros.getFieldErrors().stream().map(e ->
          e.isBindingFailure() ? "Confira a data, o horário ou o número informado." : e.getDefaultMessage()).distinct().toList());
      return "public/reserva-form";
    }
    try {
      Orcamento o = service.estimar(f);
      m.addAttribute("orcamento", o);
      m.addAttribute("espaco", espacos.findById(f.espacoId).orElseThrow());
      m.addAttribute("persona", personas.findById(f.personaId).orElseThrow());
      session.setAttribute("solicitacao", f);
      session.setAttribute("solicitacao.key", java.util.UUID.randomUUID().toString());
      return "public/reserva-resumo";
    } catch (RegraException ex) {
      response.setStatus(400);
      br.com.inpetu.hub.observability.OperationalEvents.problem("POST", "/reservas/resumo", 400, ex);
      m.addAttribute("erro", ex.getMessage());
      return "public/reserva-form";
    }
  }

  @PostMapping("/reservas/solicitar")
  public String solicitar(HttpSession session, RedirectAttributes flash) {
    ReservaForm f = (ReservaForm) session.getAttribute("solicitacao");
    if (f == null) throw new RegraException("Preencha o formulário antes de confirmar.");
    var s = requests.submit(f, (String) session.getAttribute("solicitacao.key"));
    session.setAttribute("request.reference", s.referencia);
    session.removeAttribute("request.confirmed");
    session.removeAttribute("solicitacao");
    return "redirect:/solicitacoes/confirmar";
  }

  @GetMapping("/reservas/sucesso")
  public String sucesso() {
    return "public/reserva-sucesso";
  }

  @GetMapping("/disponibilidade")
  @ResponseBody
  public Map<String, Object> disponibilidade(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
      @RequestParam(required = false) String mes,
      @RequestParam(required = false) Long espacoId) {
    Configuracao c = config.findById(1L).orElseThrow();
    var esp =
        espacos.findByAtivoTrueAndDisponibilidadeTrueOrderByNomeEspaco().stream()
            .filter(e -> espacoId == null || e.id.equals(espacoId))
            .toList();
    YearMonth ym = mes == null ? YearMonth.now(clock) : YearMonth.parse(mes);
    LocalDate inicio = data == null ? ym.atDay(1) : data,
        fim = data == null ? ym.atEndOfMonth() : data;
    var rows =
        reservas.findByDataBetweenOrderByDataAscHoraInicioAsc(inicio, fim).stream()
            .filter(r -> r.statusReserva == StatusReserva.APROVADA)
            .toList();
    List<Map<String, Object>> dias = new ArrayList<>();
    for (LocalDate d = inicio; !d.isAfter(fim); d = d.plusDays(1)) {
      var dia = d;
      List<Map<String, Object>> salas = new ArrayList<>();
      boolean temReserva = false, temLivre = false;
      for (Espaco e : esp) {
        var ocupados =
            rows.stream()
                .filter(r -> r.data.equals(dia) && r.espaco.id.equals(e.id))
                .sorted(Comparator.comparing(r -> r.horaInicio))
                .toList();
        List<Map<String, String>> livres = new ArrayList<>(), eventos = new ArrayList<>();
        LocalTime cursor = c.abertura;
        for (Reserva r : ocupados) {
          LocalTime ri = r.horaInicio.isBefore(c.abertura) ? c.abertura : r.horaInicio;
          LocalTime rf = r.horaFim.isAfter(c.fechamento) ? c.fechamento : r.horaFim;
          if (ri.isAfter(cursor))
            livres.add(Map.of("inicio", cursor.toString(), "fim", ri.toString()));
          if (rf.isAfter(cursor)) cursor = rf;
          eventos.add(
              Map.of(
                  "inicio",
                  r.horaInicio.toString(),
                  "fim",
                  r.horaFim.toString(),
                  "titulo",
                  r.statusReserva == StatusReserva.APROVADA ? r.titulo : "Solicitação em análise",
                  "status",
                  r.statusReserva.name()));
        }
        if (cursor.isBefore(c.fechamento))
          livres.add(Map.of("inicio", cursor.toString(), "fim", c.fechamento.toString()));
        temReserva |= !ocupados.isEmpty();
        temLivre |= !livres.isEmpty();
        salas.add(
            Map.of(
                "id",
                e.id,
                "nome",
                e.nomeEspaco,
                "capacidade",
                e.capacidade,
                "mobiliario",
                e.mobiliario == null ? "" : e.mobiliario,
                "livres",
                livres,
                "ocupados",
                eventos));
      }
      String estado =
          esp.isEmpty()
              ? "SEM_ESPACOS"
              : !temLivre ? "INDISPONÍVEL" : temReserva ? "PARCIAL" : "DISPONÍVEL";
      dias.add(
          Map.of(
              "data",
              d.toString(),
              "estado",
              estado,
              "feriado",
              feriados.existsByDataAndAtivoTrue(d),
              "espacos",
              salas));
    }
    return Map.of(
        "dias", dias, "abertura", c.abertura.toString(), "fechamento", c.fechamento.toString());
  }
}
