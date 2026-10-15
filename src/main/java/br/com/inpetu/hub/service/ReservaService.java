package br.com.inpetu.hub.service;

import br.com.inpetu.hub.dto.*;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.exception.RegraException;
import br.com.inpetu.hub.repository.*;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaService {
  private final ReservaRepository repo;
  private final PersonaRepository personas;
  private final ConfiguracaoRepository config;
  private final DisponibilidadeService disponibilidade;
  private final PrecoService preco;
  private final CotaService cota;
  private final EmpreendedorService empreendedores;
  private final HistoricoReservaRepository historico;
  private final EmailService email;
  private final Clock clock;

  public ReservaService(
      ReservaRepository r,
      PersonaRepository p,
      ConfiguracaoRepository cf,
      DisponibilidadeService d,
      PrecoService pr,
      CotaService c,
      EmpreendedorService e,
      HistoricoReservaRepository h,
      EmailService mail,
      Clock clock) {
    repo = r;
    personas = p;
    config = cf;
    disponibilidade = d;
    preco = pr;
    cota = c;
    empreendedores = e;
    historico = h;
    email = mail;
    this.clock = clock;
  }

  public Reserva get(Long id) {
    return repo.findById(id).orElseThrow(() -> new RegraException("Reserva não encontrada."));
  }

  private Reserva montar(ReservaForm f) {
    Reserva r = new Reserva();
    r.persona =
        personas.findById(f.personaId).orElseThrow(() -> new RegraException("Persona inválida."));
    r.data = f.data;
    r.horaInicio = f.horaInicio;
    r.horaFim = f.horaFim;
    r.espaco =
        disponibilidade.validar(
            f.espacoId, f.data, f.horaInicio, f.horaFim, f.numeroParticipantes, null);
    r.dataHoraSolicitacao = LocalDateTime.now(clock);
    r.titulo = f.titulo;
    r.objetivo = f.objetivo;
    r.observacoes = f.observacoes;
    r.numeroParticipantes = f.numeroParticipantes;
    r.inscricaoPaga = f.inscricaoPaga;
    r.financiado = f.financiado;
    r.academico = f.academico;
    return r;
  }

  @Transactional(readOnly = true)
  public Orcamento estimar(ReservaForm f) {
    return preco.calcular(montar(f), BigDecimal.ZERO);
  }

  @Transactional
  public Reserva solicitar(ReservaForm f) {
    config.bloquear();
    Reserva r = montar(f);
    Orcamento o = preco.calcular(r, BigDecimal.ZERO);
    r.empreendedor = empreendedores.vincular(f);
    if (!r.empreendedor.ativo)
      throw new RegraException("Cadastro inativo. Entre em contato com a equipe.");
    r.statusReserva = StatusReserva.PENDENTE;
    r.statusPagamento = StatusPagamento.PENDENTE;
    aplicar(r, o);
    repo.saveAndFlush(r);
    reservarCota(r);
    registrar(
        r, null, "SOLICITAÇÃO", "Solicitação recebida; persona declarada sujeita à conferência.");
    email.enviarDepoisCommit(r, "Solicitação recebida");
    return r;
  }

  private void aplicar(Reserva r, Orcamento o) {
    r.usaCota = o.usaCota();
    r.horasCota = r.usaCota ? o.horas() : BigDecimal.ZERO;
    r.valorBase = o.valorBase();
    r.valorFinal = o.valorFinal();
    r.percentualSinal = o.percentualSinal();
    r.valorSinal = o.valorSinal();
    r.valorRestante = o.valorRestante();
    r.percentualLocacao = o.locacao();
    r.percentualOperacao = o.operacao();
    r.percentualLimpeza = o.limpeza();
    r.valorOperacao = o.valorOperacao();
    r.valorLimpeza = o.valorLimpeza();
    r.taxasConfiguradas = o.taxasConfiguradas();
  }

  private void reservarCota(Reserva r) {
    if (r.usaCota)
      cota.movimentar(
          r,
          r.statusReserva == StatusReserva.PENDENTE
              ? TipoMovimentacao.RESERVA_PENDENTE
              : TipoMovimentacao.UTILIZACAO,
          r.horasCota,
          "Alocação da reserva #" + r.id);
  }

  public void registrar(Reserva r, Colaborador c, String acao, String texto) {
    HistoricoReserva h = new HistoricoReserva();
    h.reserva = r;
    h.colaborador = c;
    h.acao = acao;
    h.descricao = texto;
    h.dataHora = LocalDateTime.now(clock);
    historico.save(h);
  }

  private void ativa(Reserva r) {
    if (r.statusReserva != StatusReserva.PENDENTE && r.statusReserva != StatusReserva.APROVADA)
      throw new RegraException("Esta reserva não permite mais esta ação.");
  }

  @Transactional
  public void aprovar(Long id, Colaborador colaborador) {
    config.bloquear();
    Reserva r = get(id);
    if (r.statusReserva != StatusReserva.PENDENTE)
      throw new RegraException("Somente reservas pendentes podem ser aprovadas.");
    disponibilidade.validar(
        r.espaco.id, r.data, r.horaInicio, r.horaFim, r.numeroParticipantes, r.id);
    Orcamento o = preco.calcular(r, r.usaCota ? r.horasCota : BigDecimal.ZERO);
    if (!o.taxasConfiguradas())
      throw new RegraException(
          "Configure os valores de operação/limpeza na Administração antes de aprovar.");
    cota.liberar(r);
    aplicar(r, o);
    r.statusReserva = StatusReserva.APROVADA;
    r.colaboradorResponsavel = colaborador;
    reservarCota(r);
    registrar(r, colaborador, "APROVAÇÃO", "Disponibilidade, persona, cota e valores conferidos.");
    email.enviarDepoisCommit(r, "Reserva aprovada");
  }

  @Transactional
  public void recusar(Long id, String motivo, Colaborador c) {
    config.bloquear();
    Reserva r = get(id);
    if (r.statusReserva != StatusReserva.PENDENTE)
      throw new RegraException("Somente reservas pendentes podem ser recusadas.");
    if (motivo == null || motivo.isBlank() || motivo.length() > 2000)
      throw new RegraException("Informe um motivo de recusa de até 2000 caracteres.");
    cota.liberar(r);
    r.statusReserva = StatusReserva.RECUSADA;
    r.motivoRecusa = motivo;
    r.colaboradorResponsavel = c;
    registrar(r, c, "RECUSA", motivo);
    email.enviarDepoisCommit(r, "Reserva recusada");
  }

  @Transactional
  public void alterar(
      Long id,
      LocalDate data,
      LocalTime inicio,
      LocalTime fim,
      Long espacoId,
      Long personaId,
      Colaborador c) {
    config.bloquear();
    Reserva r = get(id);
    ativa(r);
    if (r.statusPagamento != StatusPagamento.PENDENTE)
      throw new RegraException(
          "Regularize o registro financeiro antes de alterar uma reserva com pagamento"
              + " registrado.");
    String antes =
        r.data
            + " "
            + r.horaInicio
            + "–"
            + r.horaFim
            + " / "
            + r.espaco.nomeEspaco
            + " / "
            + r.persona.codigo;
    cota.liberar(r);
    r.data = data;
    r.horaInicio = inicio;
    r.horaFim = fim;
    r.espaco = disponibilidade.validar(espacoId, data, inicio, fim, r.numeroParticipantes, r.id);
    r.persona =
        personas.findById(personaId).orElseThrow(() -> new RegraException("Persona inválida."));
    Orcamento o = preco.calcular(r, BigDecimal.ZERO);
    if (r.statusReserva == StatusReserva.APROVADA && !o.taxasConfiguradas())
      throw new RegraException("Configure as taxas antes de alterar uma reserva aprovada.");
    aplicar(r, o);
    reservarCota(r);
    r.colaboradorResponsavel = c;
    registrar(
        r,
        c,
        "ALTERAÇÃO",
        antes
            + " → "
            + data
            + " "
            + inicio
            + "–"
            + fim
            + " / "
            + r.espaco.nomeEspaco
            + " / "
            + r.persona.codigo);
    email.enviarDepoisCommit(r, "Reserva alterada");
  }

  @Transactional
  public void cancelar(Long id, boolean forcaMaior, String justificativa, Colaborador c) {
    config.bloquear();
    Reserva r = get(id);
    ativa(r);
    if (forcaMaior
        && (c.perfil != Perfil.ADMIN || justificativa == null || justificativa.isBlank()))
      throw new RegraException("A exceção exige um gestor e justificativa.");
    cota.liberar(r);
    boolean penalizar =
        r.usaCota && !forcaMaior && ChronoUnit.DAYS.between(LocalDate.now(clock), r.data) < 30;
    if (penalizar)
      cota.movimentar(
          r,
          TipoMovimentacao.PENALIDADE,
          r.horasCota.multiply(BigDecimal.valueOf(2)),
          "Cancelamento fora do prazo");
    r.statusReserva = StatusReserva.CANCELADA;
    r.colaboradorResponsavel = c;
    registrar(
        r,
        c,
        "CANCELAMENTO",
        (penalizar
                ? "Penalidade: " + r.horasCota.multiply(BigDecimal.valueOf(2)) + "h. "
                : "Sem penalidade. ")
            + (justificativa == null ? "" : justificativa));
    email.enviarDepoisCommit(r, "Reserva cancelada");
  }

  @Transactional
  public void pagamento(Long id, StatusPagamento status, Colaborador c) {
    config.bloquear();
    Reserva r = get(id);
    if (r.statusReserva != StatusReserva.APROVADA && r.statusReserva != StatusReserva.CONCLUIDA)
      throw new RegraException("Registre pagamentos somente em reservas aprovadas ou concluídas.");
    r.statusPagamento = status;
    registrar(r, c, "PAGAMENTO", status.name());
  }

  @Transactional
  public void concluir(Long id, Colaborador c) {
    config.bloquear();
    Reserva r = get(id);
    if (r.statusReserva != StatusReserva.APROVADA
        || LocalDateTime.of(r.data, r.horaFim).isAfter(LocalDateTime.now(clock)))
      throw new RegraException("Só é possível concluir uma reserva aprovada após seu término.");
    r.statusReserva = StatusReserva.CONCLUIDA;
    registrar(r, c, "CONCLUSÃO", "Evento concluído.");
  }
}
