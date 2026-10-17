package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.inpetu.hub.dto.ReservaForm;
import br.com.inpetu.hub.entity.*;
import br.com.inpetu.hub.enums.*;
import br.com.inpetu.hub.repository.*;
import br.com.inpetu.hub.security.*;
import br.com.inpetu.hub.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.config.import=",
      "spring.datasource.url=jdbc:h2:mem:security_v02;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestAccounts.class, FluxosIntegrationTest.Tempo.class})
@Transactional
class SecurityIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ColaboradorRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired SecretCrypto crypto;
  @Autowired Clock clock;
  @Autowired PublicRequestService requests;
  @Autowired ReservaService reservations;
  @Autowired SolicitacaoPublicaRepository intake;
  @Autowired EspacoRepository spaces;
  @Autowired PersonaRepository personas;
  @Autowired ConfiguracaoRepository config;
  @Autowired CotaService quotas;
  @Autowired EmpreendedorRepository people;
  @Autowired CrudService crud;
  @Autowired AuditoriaRepository audits;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
  @Autowired AuditService auditService;
  @Autowired ExpirationService expiration;
  @org.junit.jupiter.api.io.TempDir java.nio.file.Path temp;
  static final String PASSWORD = "Synthetic-only-Password-2026!";

  Colaborador create(Perfil role) {
    var c = new Colaborador();
    c.nome = "Fixture";
    c.login = "fixture-" + UUID.randomUUID();
    c.email = "fixture@example.com";
    c.perfil = role;
    c.ativo = true;
    c.securityVersion = 1;
    c.senha = encoder.encode(PASSWORD);
    return users.saveAndFlush(c);
  }

  MockHttpSession login(Colaborador c) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/login").with(csrf()).param("username", c.login).param("password", PASSWORD))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin"))
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  @ParameterizedTest
  @ValueSource(strings = {"inactive", "password", "role", "login"})
  void previousSessionsAreRevoked(String change) throws Exception {
    var c = create(Perfil.COLABORADOR);
    var first = login(c);
    var second = login(c);
    mvc.perform(get("/admin").session(first)).andExpect(status().isOk());
    if (change.equals("inactive")) c.ativo = false;
    if (change.equals("password")) c.senha = encoder.encode("Replacement-Only-For-Test!");
    if (change.equals("role")) c.perfil = Perfil.ADMIN;
    if (change.equals("login")) c.login = "altered-" + UUID.randomUUID();
    c.securityVersion++;
    users.saveAndFlush(c);
    mvc.perform(get("/admin").session(first)).andExpect(redirectedUrl("/login?expired"));
    mvc.perform(get("/conta/senha").session(second)).andExpect(redirectedUrl("/login?expired"));
  }

  @Test
  void adminUsesPasswordWithoutSecondFactor() throws Exception {
    var c = create(Perfil.ADMIN);
    var session = login(c);
    mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
    String html =
        mvc.perform(get("/admin/config").session(session))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(html.contains("/conta/mfa"));
    assertFalse(html.contains("/admin/config/mfa-reset"));
    mvc.perform(get("/conta/mfa").session(session)).andExpect(status().isNotFound());
    mvc.perform(post("/conta/mfa").session(session).with(csrf()).param("code", "123456"))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/admin/config/mfa-reset")
                .session(session)
                .with(csrf())
                .param("id", c.id.toString())
                .param("current", PASSWORD))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @EnumSource(Perfil.class)
  void legacySecondFactorDoesNotBlockPasswordLogin(Perfil role) throws Exception {
    var c = create(role);
    c.mfaSecret = "legacy-encrypted-fixture";
    c.mfaRecovery = "legacy-recovery-fixture";
    c.mfaCounter = 123;
    users.saveAndFlush(c);
    var session = login(c);
    mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
    mvc.perform(get("/admin/config").session(session))
        .andExpect(role == Perfil.ADMIN ? status().isOk() : status().isForbidden());
    assertEquals("legacy-encrypted-fixture", users.findById(c.id).orElseThrow().mfaSecret);
    assertEquals("legacy-recovery-fixture", users.findById(c.id).orElseThrow().mfaRecovery);
    assertEquals(123, users.findById(c.id).orElseThrow().mfaCounter);
  }

  @Test
  void collaboratorCannotReadOrWriteAdministration() throws Exception {
    var c = create(Perfil.COLABORADOR);
    var session = login(c);
    mvc.perform(get("/admin/config/auditoria").session(session)).andExpect(status().isForbidden());
    mvc.perform(
            post("/admin/config/cadastros/colaboradores/salvar")
                .session(session)
                .with(csrf())
                .param("id", c.id.toString())
                .param("current", PASSWORD))
        .andExpect(status().isForbidden());
  }

  @Test
  void basicLocalLoginRemainsAvailableAfterPasswordMistakes() throws Exception {
    var c = create(Perfil.COLABORADOR);
    for (int i = 0; i < 12; i++)
      mvc.perform(
              post("/login")
                  .with(csrf())
                  .param("username", c.login)
                  .param("password", "wrong")
                  .header("X-Forwarded-For", "203.0.113." + i))
          .andExpect(redirectedUrl("/login?error"));
    var session = login(c);
    mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
  }

  @ParameterizedTest
  @EnumSource(Perfil.class)
  void basicLoginAcceptsCaseAccentsAndPastedSpaces(Perfil role) throws Exception {
    var c = create(role);
    c.login = "aluno-" + UUID.randomUUID();
    users.saveAndFlush(c);
    var session =
        (MockHttpSession)
            mvc.perform(
                    post("/login")
                        .with(csrf())
                        .param(
                            "username",
                            "  "
                                + c.login.toUpperCase(Locale.ROOT).replace("ALUNO", "ÁLUNÓ")
                                + "\u00a0")
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/admin"))
                .andReturn()
                .getRequest()
                .getSession(false);
    mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
  }

  @Test
  void administrationCreatesBasicAccountWithEightCharacterPassword() throws Exception {
    var actor = users.findByLogin("fixture-admin").orElseThrow();
    crud.salvar(
        "colaboradores",
        null,
        Map.of(
            "nome",
            "Pessoa básica",
            "login",
            "ÁLUNO Básico",
            "email",
            "",
            "senha",
            "Teste123",
            "perfil",
            "COLABORADOR",
            "ativo",
            "on"),
        actor);
    var c = users.findByLogin("alunobasico").orElseThrow();
    assertTrue(encoder.matches("Teste123", c.senha));
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", "ÁLUNO Básico")
                .param("password", "Teste123"))
        .andExpect(redirectedUrl("/admin"));
  }

  @Test
  void passwordChangeAcceptsBasicEightCharacterPassword() throws Exception {
    var c = create(Perfil.COLABORADOR);
    var session = login(c);
    mvc.perform(
            post("/conta/senha")
                .session(session)
                .with(csrf())
                .param("current", PASSWORD)
                .param("replacement", "Nova1234"))
        .andExpect(redirectedUrl("/login?changed"));
    mvc.perform(
            post("/login").with(csrf()).param("username", c.login).param("password", "Nova1234"))
        .andExpect(redirectedUrl("/admin"));
  }

  ReservaForm form() {
    var space = new Espaco();
    space.nomeEspaco = "Security room";
    space.capacidade = 30;
    space.ativo = true;
    space.disponibilidade = true;
    spaces.saveAndFlush(space);
    var c = config.findById(1L).orElseThrow();
    c.taxaOperacao = BigDecimal.ZERO;
    c.taxaLimpeza = BigDecimal.ZERO;
    c.insumosLimpeza = BigDecimal.ZERO;
    config.saveAndFlush(c);
    var f = new ReservaForm();
    f.espacoId = space.id;
    f.personaId = personas.findByCodigo("P3").orElseThrow().id;
    f.data = LocalDate.of(2026, 10, 20);
    f.horaInicio = LocalTime.of(8, 0);
    f.horaFim = LocalTime.of(12, 0);
    f.nome = "Applicant";
    f.email = "applicant@example.com";
    f.cnpj = "23456789000123";
    f.telefone = "00000000000";
    f.empresa = "Example";
    f.cidade = "Example";
    f.titulo = "Event";
    f.objetivo = "Test";
    return f;
  }

  @Test
  void publicIntakeDoesNotPoisonCrmOrReserveQuotaAndIsIdempotent() {
    var f = form();
    long previous = people.count();
    String key = UUID.randomUUID().toString();
    var s = requests.submit(f, key);
    assertEquals(s.id, requests.submit(f, key).id);
    assertEquals(previous, people.count());
    assertNull(s.reservaId);
    assertEquals(
        0,
        quotas
            .consultar(personas.findById(f.personaId).orElseThrow(), f.data)
            .horasReservadasPendentes
            .signum());
    var actor = users.findByLogin("fixture-admin").orElseThrow();
    assertThrows(RuntimeException.class, () -> requests.accept(s.id, actor, false, "unchecked"));
  }

  @Test
  void confirmedStaffReviewAndApprovalRemainFunctional() {
    var f = form();
    var s = requests.submit(f, UUID.randomUUID().toString());
    var actor = users.findByLogin("fixture-admin").orElseThrow();
    Long id = requests.accept(s.id, actor, true, "Confirmed using a trusted institutional contact");
    var r = reservations.get(id);
    assertEquals(StatusReserva.PENDENTE, r.statusReserva);
    assertEquals(0, quotas.consultar(r.persona, r.data).horasReservadasPendentes.signum());
    var alternative = reservations.solicitar(f);
    reservations.aprovar(id, actor);
    assertEquals(StatusReserva.APROVADA, r.statusReserva);
    assertThrows(RuntimeException.class, () -> reservations.aprovar(alternative.id, actor));
  }

  @Test
  void refusingOrCancellingUnallocatedPendingCannotCreateQuotaCredit() {
    var f = form();
    var r = reservations.solicitar(f);
    var actor = users.findByLogin("fixture-admin").orElseThrow();
    reservations.cancelar(r.id, false, "Unapproved", actor);
    var q = quotas.consultar(r.persona, r.data);
    assertEquals(0, q.horasUtilizadas.signum());
    assertEquals(0, q.horasReservadasPendentes.signum());
  }

  @Test
  void emailCodeIsSingleUseAndBounded() {
    var s = requests.submit(form(), UUID.randomUUID().toString());
    s.codigoHash = crypto.fingerprint("public:" + s.referencia + ":12345678");
    intake.saveAndFlush(s);
    assertFalse(requests.confirm(s.referencia, "87654321"));
    assertTrue(requests.confirm(s.referencia, "12345678"));
    assertFalse(requests.confirm(s.referencia, "12345678"));
  }

  @Test
  void headersCsrfAndEscapingArePreserved() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Referrer-Policy", "no-referrer"))
        .andExpect(header().exists("Content-Security-Policy"));
    mvc.perform(post("/reservas/solicitar")).andExpect(status().isForbidden());
  }

  @Test
  void crudChangesRevokeSessionsAndAuditContainsNoCredentialValues() {
    var c = create(Perfil.COLABORADOR);
    long previous = c.securityVersion;
    var actor = users.findByLogin("fixture-admin").orElseThrow();
    crud.salvar(
        "colaboradores",
        c.id,
        Map.of(
            "nome",
            "Changed fixture",
            "login",
            c.login,
            "email",
            "fixture@example.com",
            "senha",
            "",
            "perfil",
            "COLABORADOR",
            "ativo",
            "on"),
        actor);
    assertTrue(c.securityVersion > previous);
    assertTrue(
        audits.findAll().stream()
            .anyMatch(a -> a.evento.equals("CADASTRO_ALTERADO") && c.id.equals(a.alvoId)));
    for (var event : audits.findAll()) {
      String detail = String.valueOf(event.detalhes);
      assertFalse(detail.contains(PASSWORD));
      assertFalse(detail.contains(c.senha));
      assertFalse(detail.contains(c.login));
    }
  }

  @Test
  void fivePrivateAccountsAreImportedOnceAndLegacyAccountsAreRevoked() throws Exception {
    var old = users.findByLogin("fixture-admin").orElseThrow();
    var file = temp.resolve("accounts.properties");
    var data = new StringBuilder();
    for (int i = 1; i <= 5; i++)
      data.append(
          "account."
              + i
              + ".name=Synthetic "
              + i
              + "\naccount."
              + i
              + ".login=private-fixture-"
              + i
              + "\naccount."
              + i
              + ".password=Synthetic-Only-Private-"
              + i
              + "\naccount."
              + i
              + ".role="
              + (i == 1 ? "ADMIN" : "COLABORADOR")
              + "\n");
    java.nio.file.Files.writeString(file, data);
    var initializer =
        new br.com.inpetu.hub.config.PrivateAccountInitializer(
            users, config, jdbc, encoder, auditService, crypto, file.toString());
    initializer.run(new org.springframework.boot.DefaultApplicationArguments());
    assertFalse(old.ativo);
    for (int i = 1; i <= 5; i++) {
      var u = users.findByLogin("private-fixture-" + i).orElseThrow();
      assertTrue(u.ativo);
      assertTrue(encoder.matches("Synthetic-Only-Private-" + i, u.senha));
    }
    var first = users.findByLogin("private-fixture-1").orElseThrow();
    String hash = first.senha;
    first.ativo = false;
    users.saveAndFlush(first);
    initializer.run(new org.springframework.boot.DefaultApplicationArguments());
    assertFalse(first.ativo);
    assertEquals(hash, users.findByLogin("private-fixture-1").orElseThrow().senha);
  }

  @Test
  void latePrivatePackageAddsFiveAccountsAndOnlyChangedEntriesAreApplied() throws Exception {
    var old = create(Perfil.ADMIN);
    long spacesBefore = spaces.count(), peopleBefore = people.count();
    jdbc.update("update bootstrap_seguro set concluido=true where id=1");
    var file = temp.resolve("late-accounts.properties");
    var data = new StringBuilder();
    for (int i = 1; i <= 5; i++)
      data.append(
          "account."
              + i
              + ".name=Pessoa sintética "
              + i
              + "\naccount."
              + i
              + ".login=aluno-pacote-"
              + i
              + "\naccount."
              + i
              + ".password=TesteBasico"
              + i
              + "\naccount."
              + i
              + ".role="
              + (i == 1 ? "ADMIN" : "COLABORADOR")
              + "\n");
    java.nio.file.Files.writeString(file, data);
    var initializer =
        new br.com.inpetu.hub.config.PrivateAccountInitializer(
            users, config, jdbc, encoder, auditService, crypto, file.toString());
    initializer.run(new org.springframework.boot.DefaultApplicationArguments());
    for (int i = 1; i <= 5; i++) {
      var c = users.findByLogin("aluno-pacote-" + i).orElseThrow();
      assertTrue(c.ativo);
      mvc.perform(
              post("/login")
                  .with(csrf())
                  .param("username", c.login)
                  .param("password", "TesteBasico" + i))
          .andExpect(redirectedUrl("/admin"));
    }
    assertTrue(users.findById(old.id).orElseThrow().ativo);
    var changedInPanel = users.findByLogin("aluno-pacote-1").orElseThrow();
    changedInPanel.senha = encoder.encode("SenhaDoPainel!");
    users.saveAndFlush(changedInPanel);
    String preservedHash = changedInPanel.senha;
    java.nio.file.Files.writeString(
        file,
        data.toString().replace("password=TesteBasico2", "password=TesteTeste2222")
            + "\n"
            + "account.6.name=Pessoa nova\n"
            + "account.6.login=aluno-pacote-6\n"
            + "account.6.password=TesteBasico6\n"
            + "account.6.role=COLABORADOR\n");
    initializer.run(new org.springframework.boot.DefaultApplicationArguments());
    assertEquals(preservedHash, users.findByLogin("aluno-pacote-1").orElseThrow().senha);
    assertTrue(
        encoder.matches(
            "TesteTeste2222", users.findByLogin("aluno-pacote-2").orElseThrow().senha));
    assertTrue(users.findByLogin("aluno-pacote-6").orElseThrow().ativo);
    assertEquals(spacesBefore, spaces.count());
    assertEquals(peopleBefore, people.count());
    java.nio.file.Files.delete(file);
    initializer.run(new org.springframework.boot.DefaultApplicationArguments());
    assertEquals(preservedHash, users.findByLogin("aluno-pacote-1").orElseThrow().senha);
  }

  @Test
  void invalidUpdatedPackageDoesNotPartiallyChangeAccounts() throws Exception {
    var c = create(Perfil.ADMIN);
    String hash = c.senha;
    long count = users.count();
    jdbc.update("update bootstrap_seguro set concluido=true where id=1");
    var file = temp.resolve("invalid-update.properties");
    java.nio.file.Files.writeString(
        file,
        "account.1.name=Teste\naccount.1.login="
            + c.login
            + "\n"
            + "account.1.password=NovaSenhaTeste1\n"
            + "account.1.role=ADMIN\n"
            + "account.2.name=Inválido\n"
            + "account.2.login=outra-pessoa\n"
            + "account.2.password=curta\n"
            + "account.2.role=COLABORADOR\n");
    var initializer =
        new br.com.inpetu.hub.config.PrivateAccountInitializer(
            users, config, jdbc, encoder, auditService, crypto, file.toString());
    assertThrows(
        IllegalStateException.class,
        () -> initializer.run(new org.springframework.boot.DefaultApplicationArguments()));
    assertEquals(hash, users.findById(c.id).orElseThrow().senha);
    assertEquals(count, users.count());
  }

  @Test
  void emailConfirmationHasFiveAttemptLimitAndExpiry() {
    var s = requests.submit(form(), UUID.randomUUID().toString());
    s.codigoHash = crypto.fingerprint("public:" + s.referencia + ":12345678");
    intake.saveAndFlush(s);
    for (int i = 0; i < 5; i++) assertFalse(requests.confirm(s.referencia, "00000000"));
    assertFalse(requests.confirm(s.referencia, "12345678"));
    assertFalse(s.emailConfirmado);
    var other = requests.submit(form(), UUID.randomUUID().toString());
    other.codigoHash = crypto.fingerprint("public:" + other.referencia + ":12345678");
    other.codigoExpiraEm = LocalDateTime.now(clock).minusMinutes(1);
    intake.saveAndFlush(other);
    assertFalse(requests.confirm(other.referencia, "12345678"));
  }

  @Test
  void encodedInternalPathPreservesAdministrationPermissions() throws Exception {
    var c = create(Perfil.ADMIN);
    var session = login(c);
    mvc.perform(
            get(java.net.URI.create("/%61dmin/config"))
                .with(
                    req -> {
                      req.setServletPath("/admin/config");
                      return req;
                    })
                .session(session))
        .andExpect(status().isOk());
    var employee = login(create(Perfil.COLABORADOR));
    mvc.perform(
            get(java.net.URI.create("/%61dmin/config"))
                .with(
                    req -> {
                      req.setServletPath("/admin/config");
                      return req;
                    })
                .session(employee))
        .andExpect(status().isForbidden());
  }

  @Test
  void encodedLoginAcceptsBasicCredentials() throws Exception {
    var c = create(Perfil.COLABORADOR);
    mvc.perform(
            post(java.net.URI.create("/l%6fgin"))
                .with(
                    req -> {
                      req.setServletPath("/login");
                      return req;
                    })
                .with(csrf())
                .param("username", " " + c.login.toUpperCase(Locale.ROOT) + " ")
                .param("password", PASSWORD))
        .andExpect(redirectedUrl("/admin"));
  }

  @ParameterizedTest
  @EnumSource(Perfil.class)
  void absoluteSessionExpiryStillAppliesToBothRoles(Perfil role) throws Exception {
    var session = login(create(role));
    session.setAttribute("auth.started", clock.millis() - 300001);
    mvc.perform(get("/admin").session(session)).andExpect(status().isOk());
    session.setAttribute("auth.started", clock.millis() - 7200001);
    mvc.perform(get("/admin").session(session)).andExpect(redirectedUrl("/login?expired"));
  }

  @Test
  void expiredIntakeAndReservationCannotBeAccepted() {
    var f = form();
    var request = requests.submit(f, UUID.randomUUID().toString());
    var reservation = reservations.solicitar(f);
    request.expiraEm = LocalDateTime.now(clock).minusSeconds(1);
    reservation.expiraEm = LocalDateTime.now(clock).minusSeconds(1);
    var actor = users.findByLogin("fixture-admin").orElseThrow();
    assertThrows(RuntimeException.class, () -> requests.accept(request.id, actor, true, "Fixture"));
    assertThrows(RuntimeException.class, () -> reservations.aprovar(reservation.id, actor));
    expiration.expire();
    assertEquals("EXPIRADA", request.estado);
    assertEquals(StatusReserva.CANCELADA, reservation.statusReserva);
    assertEquals(
        0,
        quotas.consultar(reservation.persona, reservation.data).horasReservadasPendentes.signum());
  }
}
