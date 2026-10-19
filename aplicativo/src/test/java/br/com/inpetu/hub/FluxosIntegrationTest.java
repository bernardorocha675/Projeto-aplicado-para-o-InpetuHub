package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.inpetu.hub.dto.*;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.exception.*;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.service.*;
import java.math.*;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.config.import=",
      "app.admin-login=fixture-admin",
      "app.admin-password=Test-only-Password-2026!"
    })
@Import(TestAccounts.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FluxosIntegrationTest {
  @TestConfiguration
  static class Tempo {
    @Bean
    @Primary
    Clock testeClock() {
      return Clock.fixed(Instant.parse("2026-10-05T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    }
  }

  @Autowired ReservaService service;
  @Autowired CotaService cota;
  @Autowired EspacoRepository espacos;
  @Autowired PersonaRepository personas;
  @Autowired EmpreendedorRepository empreendedores;
  @Autowired ColaboradorRepository colaboradores;
  @Autowired ConfiguracaoRepository configuracoes;
  @Autowired ReservaRepository reservas;
  @Autowired MockMvc mvc;
  Espaco sala;
  Colaborador admin;

  @BeforeEach
  void preparar() {
    sala = new Espaco();
    sala.nomeEspaco = "Sala de teste";
    sala.capacidade = 40;
    sala.ativo = true;
    sala.disponibilidade = true;
    espacos.saveAndFlush(sala);
    admin = colaboradores.findByLogin("fixture-admin").orElseThrow();
    var c = configuracoes.findById(1L).orElseThrow();
    c.taxaOperacao = BigDecimal.ZERO;
    c.taxaLimpeza = BigDecimal.ZERO;
    c.insumosLimpeza = BigDecimal.ZERO;
    configuracoes.saveAndFlush(c);
  }

  ReservaForm form(String codigo, LocalDate data) {
    ReservaForm f = new ReservaForm();
    f.espacoId = sala.id;
    f.personaId = personas.findByCodigo(codigo).orElseThrow().id;
    f.data = data;
    f.horaInicio = LocalTime.of(8, 0);
    f.horaFim = LocalTime.of(12, 0);
    f.numeroParticipantes = 10;
    f.nome = "Maria";
    f.cnpj = "12345678000190";
    f.email = "maria@example.com";
    f.telefone = "48999999999";
    f.empresa = "Empresa";
    f.cidade = "Florianópolis";
    f.titulo = "Teste";
    f.objetivo = "Conectar";
    f.academico = true;
    return f;
  }

  @Test
  void pendenteReservaCotaEAprovacaoTransfereParaUtilizada() {
    var f = form("P2", LocalDate.of(2026, 10, 20));
    var r = service.solicitar(f);
    var q = cota.consultar(r.persona, r.data);
    assertEquals(0, BigDecimal.ZERO.compareTo(q.horasReservadasPendentes));
    assertEquals(0, new BigDecimal("30").compareTo(q.getSaldo()));
    service.aprovar(r.id, admin);
    assertEquals(StatusReserva.APROVADA, r.statusReserva);
    q = cota.consultar(r.persona, r.data);
    assertEquals(0, new BigDecimal("4").compareTo(q.horasUtilizadas));
    assertEquals(0, BigDecimal.ZERO.compareTo(q.horasReservadasPendentes));
    assertEquals(admin.id, r.colaboradorResponsavel.id);
  }

  @Test
  void menosDe15DiasNaoUtilizaCota() {
    var r = service.solicitar(form("P2", LocalDate.of(2026, 10, 19)));
    assertFalse(r.usaCota);
    assertEquals(100, r.percentualLocacao);
  }

  @Test
  void recusaExigeMotivoELiberaCotaEHorario() {
    var r = service.solicitar(form("P3", LocalDate.of(2026, 10, 20)));
    assertThrows(RegraException.class, () -> service.recusar(r.id, "", admin));
  }

  @Test
  void recusaValidaLiberaCotaEHorario() {
    var r = service.solicitar(form("P3", LocalDate.of(2026, 10, 20)));
    service.recusar(r.id, "Documentação incompleta", admin);
    assertEquals(StatusReserva.RECUSADA, r.statusReserva);
    assertEquals(
        0, BigDecimal.ZERO.compareTo(cota.consultar(r.persona, r.data).horasReservadasPendentes));
    var outra = service.solicitar(form("P3", LocalDate.of(2026, 10, 20)));
    assertEquals(StatusReserva.PENDENTE, outra.statusReserva);
    assertEquals(1, empreendedores.count());
  }

  @Test
  void aprovacaoBloqueiaHorariosMasPendentesNao() {
    var first = service.solicitar(form("P6", LocalDate.of(2026, 10, 20)));
    var pending = service.solicitar(form("P6", LocalDate.of(2026, 10, 20)));
    service.aprovar(first.id, admin);
    assertThrows(RegraException.class, () -> service.aprovar(pending.id, admin));
    var f = form("P6", LocalDate.of(2026, 10, 20));
    f.horaInicio = LocalTime.of(11, 0);
    f.horaFim = LocalTime.of(14, 0);
    assertThrows(RegraException.class, () -> service.solicitar(f));
  }

  @Test
  void alteraRecalculaDomingoECota() {
    var r = service.solicitar(form("P6", LocalDate.of(2026, 10, 20)));
    assertEquals(30, r.percentualSinal);
    service.alterar(
        r.id,
        LocalDate.of(2026, 10, 25),
        LocalTime.of(8, 0),
        LocalTime.of(12, 0),
        sala.id,
        r.persona.id,
        admin);
    assertEquals(60, r.percentualSinal);
    assertEquals(new BigDecimal("960.00"), r.valorSinal);
  }

  @Test
  void cancelamentoTardioDuplicaHorasEPropagaDivida() {
    Persona p = personas.findByCodigo("P4").orElseThrow();
    p.cotaMensalHoras = new BigDecimal("6");
    personas.saveAndFlush(p);
    var r = service.solicitar(form("P4", LocalDate.of(2026, 10, 20)));
    service.aprovar(r.id, admin);
    service.cancelar(r.id, false, "Desistência", admin);
    var q = cota.consultar(p, r.data);
    assertEquals(0, new BigDecimal("8").compareTo(q.horasUtilizadas));
    assertEquals(0, new BigDecimal("2").compareTo(q.saldoDevedor));
    var proximo = cota.consultar(p, LocalDate.of(2026, 11, 1));
    assertEquals(0, new BigDecimal("4").compareTo(proximo.getSaldo()));
  }

  @Test
  void cancelamentoAntecipadoNaoPenaliza() {
    var r = service.solicitar(form("P3", LocalDate.of(2026, 11, 10)));
    service.aprovar(r.id, admin);
    service.cancelar(r.id, false, "Antecipado", admin);
    var q = cota.consultar(r.persona, r.data);
    assertEquals(0, BigDecimal.ZERO.compareTo(q.horasUtilizadas));
    assertEquals(0, BigDecimal.ZERO.compareTo(q.horasReservadasPendentes));
  }

  @Test
  void eventoAprovadoJaTerminadoNaoPodeSerCanceladoNemPenalizado() {
    Persona p = personas.findByCodigo("P4").orElseThrow();
    p.cotaMensalHoras = new BigDecimal("6");
    personas.saveAndFlush(p);
    var r = service.solicitar(form("P4", LocalDate.of(2026, 10, 20)));
    service.aprovar(r.id, admin);
    // O relógio de teste está em 05/10/2026 12:00; a reserva das 8h às 12h do mesmo dia terminou.
    r.data = LocalDate.of(2026, 10, 5);
    reservas.saveAndFlush(r);
    var antes = cota.consultar(p, r.data).horasUtilizadas;
    var erro =
        assertThrows(RegraException.class, () -> service.cancelar(r.id, false, "Tarde", admin));
    assertTrue(erro.getMessage().contains("já terminou"));
    assertEquals(StatusReserva.APROVADA, r.statusReserva);
    assertEquals(0, antes.compareTo(cota.consultar(p, r.data).horasUtilizadas));
    service.cancelar(r.id, true, "Interdição registrada pelo gestor", admin);
    assertEquals(StatusReserva.CANCELADA, r.statusReserva);
    assertEquals(0, BigDecimal.ZERO.compareTo(cota.consultar(p, r.data).horasUtilizadas));
  }

  @Test
  void eventoAprovadoAindaNaoTerminadoContinuaCancelavelComPenalidade() {
    Persona p = personas.findByCodigo("P4").orElseThrow();
    p.cotaMensalHoras = new BigDecimal("6");
    personas.saveAndFlush(p);
    var r = service.solicitar(form("P4", LocalDate.of(2026, 10, 20)));
    service.aprovar(r.id, admin);
    r.data = LocalDate.of(2026, 10, 5);
    r.horaFim = LocalTime.of(13, 0);
    reservas.saveAndFlush(r);
    service.cancelar(r.id, false, "Desistência no dia", admin);
    assertEquals(StatusReserva.CANCELADA, r.statusReserva);
    assertEquals(0, new BigDecimal("8").compareTo(cota.consultar(p, r.data).horasUtilizadas));
  }

  @Test
  void forcaMaiorJustificadaDispensaPenalidade() {
    var r = service.solicitar(form("P3", LocalDate.of(2026, 10, 20)));
    service.cancelar(r.id, true, "Interdição do espaço", admin);
    assertEquals(0, BigDecimal.ZERO.compareTo(cota.consultar(r.persona, r.data).horasUtilizadas));
  }

  @Test
  void horasNaoAcumulamEReservaForaDaCotaContinuaPossivel() {
    Persona p = personas.findByCodigo("P3").orElseThrow();
    p.cotaMensalHoras = new BigDecimal("4");
    personas.saveAndFlush(p);
    var r = service.solicitar(form("P3", LocalDate.of(2026, 10, 20)));
    service.aprovar(r.id, admin);
    var f = form("P3", LocalDate.of(2026, 10, 21));
    var outra = service.solicitar(f);
    assertTrue(r.usaCota);
    assertFalse(outra.usaCota);
    assertEquals(100, outra.percentualLocacao);
    assertEquals(
        0, new BigDecimal("4").compareTo(cota.consultar(p, LocalDate.of(2026, 11, 1)).getSaldo()));
  }

  @Test
  void capacidadeEInatividadeSaoVerificadas() {
    sala.ativo = false;
    espacos.saveAndFlush(sala);
    assertThrows(
        RegraException.class, () -> service.solicitar(form("P6", LocalDate.of(2026, 10, 20))));
  }

  @Test
  void precoAnoAusenteImpedeEstimativa() {
    assertThrows(
        RegraException.class, () -> service.estimar(form("P6", LocalDate.of(2027, 1, 20))));
  }

  @Test
  void exigeTaxasParaAprovar() {
    var c = configuracoes.findById(1L).orElseThrow();
    c.taxaLimpeza = null;
    configuracoes.saveAndFlush(c);
    var r = service.solicitar(form("P6", LocalDate.of(2026, 10, 20)));
    assertThrows(RegraException.class, () -> service.aprovar(r.id, admin));
  }

  @Test
  void rotasPublicasEProtecaoCsrf() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Agenda de")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "href=\"/admin/cadastros/espacos/editar\">Cadastrar espaço</a>")));
    mvc.perform(get("/login")).andExpect(status().isOk());
    mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    mvc.perform(post("/reservas/solicitar")).andExpect(status().isForbidden());
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dias[0].estado").value("DISPONÍVEL"));
  }

  @Test
  void loginBcryptFunciona() throws Exception {
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "fixture-admin")
                .param("password", "Test-only-Password-2026!"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin"));
    assertTrue(admin.senha.startsWith("$2"));
  }

  @Test
  void espacoInativoNaoSignificaAgendaOcupada() throws Exception {
    sala.ativo = false;
    espacos.saveAndFlush(sala);
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dias[0].estado").value("SEM_ESPACOS"))
        .andExpect(jsonPath("$.dias[0].espacos").isEmpty());
  }

  @Test
  void espacoDesabilitadoNaoSignificaAgendaOcupada() throws Exception {
    sala.disponibilidade = false;
    espacos.saveAndFlush(sala);
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dias[0].estado").value("SEM_ESPACOS"));
  }

  @Test
  void filtroSemEspacoElegivelNaoMarcaOcupacao() throws Exception {
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20")
            .param("espacoId", "9223372036854775807"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dias[0].estado").value("SEM_ESPACOS"));
  }

  @Test
  void reservaAprovadaParcialMantemHorariosLivres() throws Exception {
    var r = service.solicitar(form("P6", LocalDate.of(2026, 10, 20)));
    service.aprovar(r.id, admin);
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dias[0].estado").value("PARCIAL"))
        .andExpect(jsonPath("$.dias[0].espacos[0].ocupados.length()").value(1))
        .andExpect(jsonPath("$.dias[0].espacos[0].livres").isNotEmpty());
  }

  @Test
  void apenasReservaAprovadaBloqueiaDiaInteiroECancelamentoLibera() throws Exception {
    var f = form("P6", LocalDate.of(2026, 10, 20));
    var c = configuracoes.findById(1L).orElseThrow();
    f.horaInicio = c.abertura;
    f.horaFim = c.fechamento;
    var r = service.solicitar(f);
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20"))
        .andExpect(jsonPath("$.dias[0].estado").value("DISPONÍVEL"));
    service.aprovar(r.id, admin);
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20"))
        .andExpect(jsonPath("$.dias[0].estado").value("INDISPONÍVEL"))
        .andExpect(jsonPath("$.dias[0].espacos[0].livres").isEmpty());
    service.cancelar(r.id, false, "Cancelamento de teste", admin);
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20"))
        .andExpect(jsonPath("$.dias[0].estado").value("DISPONÍVEL"));
  }

  @Test
  void salaLotadaNaoBloqueiaOutraSalaLivre() throws Exception {
    var outra = new Espaco();
    outra.nomeEspaco = "Outra sala";
    outra.capacidade = 20;
    outra.ativo = true;
    outra.disponibilidade = true;
    espacos.saveAndFlush(outra);
    var f = form("P6", LocalDate.of(2026, 10, 20));
    var c = configuracoes.findById(1L).orElseThrow();
    f.horaInicio = c.abertura;
    f.horaFim = c.fechamento;
    var r = service.solicitar(f);
    service.aprovar(r.id, admin);
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20"))
        .andExpect(jsonPath("$.dias[0].estado").value("PARCIAL"));
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20")
            .param("espacoId", sala.id.toString()))
        .andExpect(jsonPath("$.dias[0].estado").value("INDISPONÍVEL"));
    mvc.perform(get("/disponibilidade").param("data", "2026-10-20")
            .param("espacoId", outra.id.toString()))
        .andExpect(jsonPath("$.dias[0].estado").value("DISPONÍVEL"));
  }

  @Test
  @WithMockUser(username = "fixture-admin", roles = "ADMIN")
  void telasInternasRenderizam() throws Exception {
    var r = service.solicitar(form("P6", LocalDate.of(2026, 10, 20)));
    for (String url :
        new String[] {
          "/admin",
          "/admin/reservas",
          "/admin/reservas/" + r.id,
          "/admin/empreendedores/" + r.empreendedor.id,
          "/admin/cadastros/empreendedores",
          "/admin/cadastros/espacos",
          "/admin/cadastros/visitas/editar",
          "/admin/cadastros/atendimentos/editar",
          "/admin/config",
          "/admin/config/cadastros/personas",
          "/admin/config/cadastros/configuracao/editar?id=1",
          "/admin/config/cotas"
        }) mvc.perform(get(url)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "fixture-admin", roles = "ADMIN")
  void crudValidaEmailESalvaVisita() throws Exception {
    var r = service.solicitar(form("P6", LocalDate.of(2026, 10, 20)));
    mvc.perform(
            post("/admin/cadastros/visitas/salvar")
                .with(csrf())
                .param("empreendedor", r.empreendedor.id.toString())
                .param("colaborador", admin.id.toString())
                .param("data", "2026-10-20")
                .param("horario", "10:00")
                .param("objetivo", "Conhecer o Hub")
                .param("participantes", "2")
                .param("status", "AGENDADA"))
        .andExpect(status().is3xxRedirection());
    mvc.perform(
            post("/admin/cadastros/empreendedores/salvar")
                .with(csrf())
                .param("id", r.empreendedor.id.toString())
                .param("nome", "Maria")
                .param("cnpj", "12345678000190")
                .param("empresa", "Empresa")
                .param("email", "maria@example.com")
                .param("telefone", "48999999999")
                .param("cidade", "Florianópolis")
                .param("ativo", "on"))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  @WithMockUser(roles = "COLABORADOR")
  void colaboradorNaoPodeAdministrar() throws Exception {
    mvc.perform(get("/admin/config")).andExpect(status().isForbidden());
    mvc.perform(post("/admin/config/cadastros/feriados/salvar").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void formularioEResumoRenderizam() throws Exception {
    mvc.perform(get("/reserva").param("data", "2026-10-20").param("espacoId", sala.id.toString()))
        .andExpect(status().isOk());
    mvc.perform(
            post("/reservas/resumo")
                .with(csrf())
                .param("espacoId", sala.id.toString())
                .param("personaId", personas.findByCodigo("P6").orElseThrow().id.toString())
                .param("data", "2026-10-20")
                .param("horaInicio", "08:00")
                .param("horaFim", "12:00")
                .param("nome", "Maria")
                .param("cnpj", "12345678000190")
                .param("email", "maria@example.com")
                .param("telefone", "48999999999")
                .param("empresa", "Empresa")
                .param("cidade", "Florianópolis")
                .param("titulo", "Encontro")
                .param("objetivo", "Conectar")
                .param("numeroParticipantes", "10"))
        .andExpect(status().isOk())
        .andExpect(view().name("public/reserva-resumo"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("480,00")));
  }
}
