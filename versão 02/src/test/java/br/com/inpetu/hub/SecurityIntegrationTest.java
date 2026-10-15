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
  @Autowired TotpService totp;
  @Autowired MfaService mfa;
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
  void adminRequiresEnrollmentAndRecoveryCannotBeReused() throws Exception {
    var c = create(Perfil.ADMIN);
    var session = login(c);
    mvc.perform(get("/admin/config").session(session)).andExpect(redirectedUrl("/conta/mfa"));
    mvc.perform(get("/conta/mfa").session(session)).andExpect(status().isOk());
    String secret = (String) session.getAttribute("mfa.enrollment");
    String code = totp.code(secret, clock.instant().getEpochSecond() / 30);
    mvc.perform(post("/conta/mfa").session(session).with(csrf()).param("code", code))
        .andExpect(redirectedUrl("/conta/mfa"));
    c = users.findById(c.id).orElseThrow();
    assertNotEquals(secret, c.mfaSecret);
    assertEquals(secret, crypto.decrypt(c.mfaSecret));
    @SuppressWarnings("unchecked")
    var recovery = (List<String>) session.getAttribute("mfa.recovery");
    assertEquals(8, recovery.size());
    mvc.perform(get("/admin/config").session(session)).andExpect(status().isOk());
    assertFalse(mfa.verify(c.id, code));
    assertTrue(mfa.verify(c.id, recovery.get(0)));
    assertFalse(mfa.verify(c.id, recovery.get(0)));
  }

  @Test
  void collaboratorCannotReadOrWriteAdministration() throws Exception {
    var c = create(Perfil.COLABORADOR);
    var session = login(c);
    mvc.perform(get("/admin/config/auditoria").session(session)).andExpect(status().isForbidden());
    mvc.perform(
            post("/admin/config/mfa-reset")
                .session(session)
                .with(csrf())
                .param("id", c.id.toString())
                .param("current", PASSWORD))
        .andExpect(status().isForbidden());
  }

  @Test
  void badLoginsAreThrottledWithoutTrustingForwardedIp() throws Exception {
    var c = create(Perfil.COLABORADOR);
    for (int i = 0; i < 10; i++)
      mvc.perform(
              post("/login")
                  .with(csrf())
                  .param("username", c.login)
                  .param("password", "wrong")
                  .header("X-Forwarded-For", "203.0.113." + i)
                  .with(
                      req -> {
                        req.setRemoteAddr("198.51.100.44");
                        return req;
                      }))
          .andExpect(status().is3xxRedirection());
    mvc.perform(
            post("/login")
                .with(csrf())
                .param("username", c.login)
                .param("password", "wrong")
                .header("X-Forwarded-For", "203.0.113.200")
                .with(
                    req -> {
                      req.setRemoteAddr("198.51.100.44");
                      return req;
                    }))
        .andExpect(status().isTooManyRequests());
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
            users, config, jdbc, encoder, auditService, file.toString());
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
  void encodedInternalPathCannotSkipSecondFactor() throws Exception {
    var c = create(Perfil.ADMIN);
    var session = login(c);
    mvc.perform(get(java.net.URI.create("/%61dmin/config")).with(req->{req.setServletPath("/admin/config");return req;}).session(session))
        .andExpect(redirectedUrl("/conta/mfa"));
  }

  @Test
  void encodedLoginCannotSkipThrottle() throws Exception {
    var c = create(Perfil.COLABORADOR);
    for (int i = 0; i < 10; i++)
      mvc.perform(
              post(java.net.URI.create("/l%6fgin")).with(req->{req.setServletPath("/login");return req;})
                  .with(csrf())
                  .param("username", c.login)
                  .param("password", "wrong")
                  .with(
                      req -> {
                        req.setRemoteAddr("198.51.100.71");
                        return req;
                      }))
          .andExpect(status().is3xxRedirection());
    mvc.perform(
            post(java.net.URI.create("/l%6fgin")).with(req->{req.setServletPath("/login");return req;})
                .with(csrf())
                .param("username", c.login)
                .param("password", "wrong")
                .with(
                    req -> {
                      req.setRemoteAddr("198.51.100.71");
                      return req;
                    }))
        .andExpect(status().isTooManyRequests());
  }
}
