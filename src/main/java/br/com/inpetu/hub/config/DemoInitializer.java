package br.com.inpetu.hub.config;

import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.service.*;
import java.math.*;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoInitializer implements ApplicationRunner {
  private final ColaboradorRepository colaboradores;
  private final EspacoRepository espacos;
  private final EmpreendedorRepository empreendedores;
  private final PersonaRepository personas;
  private final ReservaRepository reservas;
  private final HistoricoReservaRepository historico;
  private final AtendimentoRepository atendimentos;
  private final VisitaRepository visitas;
  private final PasswordEncoder encoder;
  private final boolean demo;
  private final String login, senha;
  private final Clock clock;
  private final PrecoService preco;

  public DemoInitializer(
      ColaboradorRepository c,
      EspacoRepository s,
      EmpreendedorRepository e,
      PersonaRepository p,
      ReservaRepository r,
      HistoricoReservaRepository h,
      AtendimentoRepository a,
      VisitaRepository v,
      PasswordEncoder b,
      @Value("${app.seed-demo}") boolean d,
      @Value("${app.admin-login}") String l,
      @Value("${app.admin-password}") String pw,
      Clock clock,
      PrecoService preco) {
    colaboradores = c;
    espacos = s;
    empreendedores = e;
    personas = p;
    reservas = r;
    historico = h;
    atendimentos = a;
    visitas = v;
    encoder = b;
    demo = d;
    login = l;
    senha = pw;
    this.clock = clock;
    this.preco = preco;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Colaborador admin =
        colaboradores
            .findByLogin(login)
            .orElseGet(
                () -> {
                  Colaborador c = new Colaborador();
                  c.nome = "Administrador local";
                  c.login = login;
                  c.email = login;
                  c.perfil = Perfil.ADMIN;
                  c.ativo = true;
                  c.senha = encoder.encode(senha);
                  return colaboradores.save(c);
                });
    if (!demo || espacos.count() > 0) return;
    String[] nomes = {"Auditório InPETU", "Sala de Conexões", "Sala de Reuniões"};
    int[] caps = {100, 40, 12};
    for (int i = 0; i < 3; i++) {
      Espaco e = new Espaco();
      e.nomeEspaco = nomes[i];
      e.capacidade = caps[i];
      e.ativo = true;
      e.disponibilidade = true;
      espacos.save(e);
    }
    for (int i = 0; i < 5; i++) {
      Empreendedor e = new Empreendedor();
      e.cnpj = "0000000000000" + i;
      e.nome = "Empreendedor exemplo " + (i + 1);
      e.email = "demo" + i + "@example.com";
      e.empresa = "Instituição exemplo " + (i + 1);
      e.telefone = "48999990000";
      e.cidade = "Florianópolis";
      e.segmento = "Tecnologia";
      e.cargo = "Responsável";
      e.ativo = true;
      empreendedores.save(e);
    }
    LocalDate hoje = LocalDate.now(clock);
    if (hoje.getYear() == 2026)
      for (int i = 0; i < 2; i++) {
        LocalDate data = hoje.plusDays(i + 2);
        if (data.getYear() != 2026) continue;
        Reserva r = new Reserva();
        r.empreendedor = empreendedores.findAll().get(i);
        r.espaco = espacos.findAll().get(i);
        r.persona = personas.findByCodigo(i == 0 ? "P6" : "P1").orElseThrow();
        r.data = data;
        r.horaInicio = LocalTime.of(14, 0);
        r.horaFim = LocalTime.of(16, 0);
        r.dataHoraSolicitacao = LocalDateTime.now(clock);
        r.statusReserva = i == 0 ? StatusReserva.PENDENTE : StatusReserva.APROVADA;
        r.statusPagamento = StatusPagamento.PENDENTE;
        r.titulo = i == 0 ? "Encontro de empreendedores" : "Conexões do ecossistema";
        r.objetivo = "Demonstração do fluxo de reservas";
        r.numeroParticipantes = 10;
        var o = preco.calcular(r, BigDecimal.ZERO);
        r.horasCota = BigDecimal.ZERO;
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
        r.colaboradorResponsavel = i == 1 ? admin : null;
        reservas.save(r);
        HistoricoReserva h = new HistoricoReserva();
        h.reserva = r;
        h.colaborador = admin;
        h.dataHora = LocalDateTime.now(clock);
        h.acao = "EXEMPLO";
        h.descricao = "Dados fictícios de demonstração.";
        historico.save(h);
      }
    Atendimento a = new Atendimento();
    a.empreendedor = empreendedores.findAll().get(0);
    a.colaborador = admin;
    a.data = hoje;
    a.descricao = "Apresentação dos serviços do Hub";
    a.observacoes = "Registro fictício";
    a.encaminhamentos = "Consulta de disponibilidade na agenda";
    atendimentos.save(a);
    Visita v = new Visita();
    v.empreendedor = a.empreendedor;
    v.colaborador = admin;
    v.data = hoje.plusDays(3);
    v.horario = LocalTime.of(10, 0);
    v.objetivo = "Conhecer os espaços";
    v.participantes = 3;
    v.status = "AGENDADA";
    visitas.save(v);
  }
}
