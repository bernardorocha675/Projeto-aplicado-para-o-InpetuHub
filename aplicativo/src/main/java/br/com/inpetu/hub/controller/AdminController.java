package br.com.inpetu.hub.controller;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.service.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
  private final ReservaRepository reservas;
  private final EmpreendedorRepository empreendedores;
  private final EspacoRepository espacos;
  private final PersonaRepository personas;
  private final ColaboradorRepository colaboradores;
  private final ReservaService service;
  private final CotaService cota;
  private final HistoricoReservaRepository historico;
  private final AtendimentoRepository atendimentos;
  private final VisitaRepository visitas;
  private final Clock clock;

  public AdminController(
      ReservaRepository r,
      EmpreendedorRepository e,
      EspacoRepository sp,
      PersonaRepository p,
      ColaboradorRepository co,
      ReservaService s,
      CotaService q,
      HistoricoReservaRepository h,
      AtendimentoRepository a,
      VisitaRepository v,
      Clock clock) {
    reservas = r;
    empreendedores = e;
    espacos = sp;
    personas = p;
    colaboradores = co;
    service = s;
    cota = q;
    historico = h;
    atendimentos = a;
    visitas = v;
    this.clock = clock;
  }

  private Colaborador user(Authentication a) {
    return colaboradores.findByLogin(a.getName()).orElseThrow();
  }

  @GetMapping
  public String dashboard(Model m) {
    var all =
        reservas.findAll().stream()
            .sorted(Comparator.comparing((Reserva r) -> r.dataHoraSolicitacao).reversed())
            .toList();
    LocalDate hoje = LocalDate.now(clock);
    m.addAttribute(
        "pendentes", all.stream().filter(r -> r.statusReserva == StatusReserva.PENDENTE).count());
    m.addAttribute(
        "hoje",
        all.stream()
            .filter(
                r ->
                    r.data.equals(hoje)
                        && (r.statusReserva == StatusReserva.PENDENTE
                            || r.statusReserva == StatusReserva.APROVADA))
            .count());
    m.addAttribute("totalEmpreendedores", empreendedores.count());
    m.addAttribute("ultimas", all.stream().limit(8).toList());
    m.addAttribute(
        "proximas",
        all.stream()
            .filter(
                r ->
                    !r.data.isBefore(hoje)
                        && (r.statusReserva == StatusReserva.APROVADA
                            || r.statusReserva == StatusReserva.PENDENTE))
            .sorted(Comparator.comparing((Reserva r) -> r.data).thenComparing(r -> r.horaInicio))
            .limit(8)
            .toList());
    return "admin/dashboard";
  }

  @GetMapping("/reservas")
  public String reservas(
      @RequestParam(required = false) StatusReserva status,
      @RequestParam(required = false) LocalDate data,
      @RequestParam(required = false) Long espaco,
      @RequestParam(required = false) Long persona,
      @RequestParam(defaultValue = "") String busca,
      Model m) {
    String q = busca.toLowerCase(Locale.ROOT);
    var lista =
        reservas.findAll().stream()
            .filter(r -> status == null || r.statusReserva == status)
            .filter(r -> data == null || r.data.equals(data))
            .filter(r -> espaco == null || r.espaco.id.equals(espaco))
            .filter(r -> persona == null || r.persona.id.equals(persona))
            .filter(
                r ->
                    (r.empreendedor.nome + " " + Objects.toString(r.empreendedor.empresa, "") + " " + Objects.toString(r.empreendedor.cnpj, ""))
                        .toLowerCase(Locale.ROOT)
                        .contains(q))
            .sorted(Comparator.comparing((Reserva r) -> r.data).thenComparing(r -> r.horaInicio))
            .toList();
    m.addAttribute("reservas", lista);
    m.addAttribute("espacos", espacos.findAll());
    m.addAttribute("personas", personas.findAll());
    m.addAttribute("filtroStatus", status);
    m.addAttribute("filtroData", data);
    m.addAttribute("filtroEspaco", espaco);
    m.addAttribute("filtroPersona", persona);
    m.addAttribute("busca", busca);
    return "admin/reservas/lista";
  }

  @GetMapping("/reservas/{id}")
  public String detalhe(@PathVariable Long id, Model m, Authentication a) {
    var r = service.get(id);
    m.addAttribute("r", r);
    m.addAttribute("cota", cota.consultar(r.persona, r.data));
    m.addAttribute(
        "antecedencia", ChronoUnit.DAYS.between(r.dataHoraSolicitacao.toLocalDate(), r.data));
    m.addAttribute("historico", historico.findByReservaIdOrderByDataHoraDesc(id));
    m.addAttribute("espacos", espacos.findAll());
    m.addAttribute("personas", personas.findAll());
    m.addAttribute("gestor", user(a).perfil == Perfil.ADMIN);
    return "admin/reservas/detalhe";
  }

  @PostMapping("/reservas/{id}/aprovar")
  public String aprovar(@PathVariable Long id, Authentication a, RedirectAttributes f) {
    service.aprovar(id, user(a));
    return sucesso(id, f, "Reserva aprovada.");
  }

  @PostMapping("/reservas/{id}/recusar")
  public String recusar(
      @PathVariable Long id, @RequestParam String motivo, Authentication a, RedirectAttributes f) {
    service.recusar(id, motivo, user(a));
    return sucesso(id, f, "Reserva recusada e horário liberado.");
  }

  @PostMapping("/reservas/{id}/alterar")
  public String alterar(
      @PathVariable Long id,
      @RequestParam LocalDate data,
      @RequestParam LocalTime horaInicio,
      @RequestParam LocalTime horaFim,
      @RequestParam Long espacoId,
      @RequestParam Long personaId,
      Authentication a,
      RedirectAttributes f) {
    service.alterar(id, data, horaInicio, horaFim, espacoId, personaId, user(a));
    return sucesso(id, f, "Reserva alterada e valores recalculados.");
  }

  @PostMapping("/reservas/{id}/cancelar")
  public String cancelar(
      @PathVariable Long id,
      @RequestParam(defaultValue = "false") boolean forcaMaior,
      @RequestParam(defaultValue = "") String justificativa,
      Authentication a,
      RedirectAttributes f) {
    if (justificativa.length() > 2000) throw new RegraException("Justificativa muito longa.");
    service.cancelar(id, forcaMaior, justificativa, user(a));
    return sucesso(id, f, "Reserva cancelada. Consulte o histórico para a movimentação de cota.");
  }

  @PostMapping("/reservas/{id}/pagamento")
  public String pagamento(
      @PathVariable Long id,
      @RequestParam StatusPagamento status,
      Authentication a,
      RedirectAttributes f) {
    service.pagamento(id, status, user(a));
    return sucesso(id, f, "Registro de pagamento atualizado.");
  }

  @PostMapping("/reservas/{id}/concluir")
  public String concluir(@PathVariable Long id, Authentication a, RedirectAttributes f) {
    service.concluir(id, user(a));
    return sucesso(id, f, "Reserva concluída.");
  }

  private String sucesso(Long id, RedirectAttributes f, String mensagem) {
    f.addFlashAttribute("sucesso", mensagem);
    return "redirect:/admin/reservas/" + id;
  }

  @GetMapping("/empreendedores/{id}")
  public String perfil(@PathVariable Long id, Model m) {
    m.addAttribute(
        "e",
        empreendedores
            .findById(id)
            .orElseThrow(() -> new RegraException("Empreendedor não encontrado.")));
    m.addAttribute("reservas", reservas.findByEmpreendedorIdOrderByDataDesc(id));
    m.addAttribute("atendimentos", atendimentos.findByEmpreendedorIdOrderByDataDesc(id));
    m.addAttribute("visitas", visitas.findByEmpreendedorIdOrderByDataDesc(id));
    m.addAttribute("historico", historico.findByReservaEmpreendedorIdOrderByDataHoraDesc(id));
    return "admin/empreendedores/perfil";
  }

  @GetMapping("/config")
  public String config(Model m) {
    m.addAttribute("personas", personas.findAll());
    return "admin/config";
  }

  @GetMapping("/config/cotas")
  public String cotas(@RequestParam(required = false) String mes, Model m) {
    YearMonth ym = mes == null ? YearMonth.now(clock) : YearMonth.parse(mes);
    m.addAttribute("mes", ym);
    m.addAttribute(
        "cotas",
        personas.findAll().stream()
            .filter(p -> p.cotaMensalHoras != null)
            .map(p -> cota.consultar(p, ym.atDay(1)))
            .toList());
    return "admin/cotas";
  }
}
