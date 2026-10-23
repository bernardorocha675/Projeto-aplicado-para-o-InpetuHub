package br.com.inpetu.hub.observability;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.*;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.rolling.*;
import ch.qos.logback.core.util.FileSize;
import jakarta.servlet.ServletException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.*;
import tools.jackson.databind.json.JsonMapper;

class LoggingTest {
  @TempDir Path temp;
  final JsonMapper json = JsonMapper.builder().build();

  @Test
  void everyLogLineCarriesVersionsAndOnlyAValidSessionIdentifier() {
    String[] names = {"inpetu.app.version", "inpetu.launcher.version", "inpetu.session.id"};
    String[] old = Arrays.stream(names).map(System::getProperty).toArray(String[]::new);
    try {
      System.setProperty(names[0], "0.4.2");
      System.setProperty(names[1], "0.5.0");
      System.setProperty(names[2], "a".repeat(32));
      var event = new LoggingEvent();
      event.setTimeStamp(0);
      event.setLevel(Level.ERROR);
      event.setLoggerName("org.example.Framework");
      event.setMessage("never-copy-this-secret");
      event.setMDCPropertyMap(Map.of());
      var encoded = json.readTree(new SafeLogEncoder().encode(event));
      assertEquals("0.4.2", encoded.get("appVersion").asString());
      assertEquals("0.5.0", encoded.get("launcherVersion").asString());
      assertEquals("a".repeat(32), encoded.get("sessionId").asString());
      System.setProperty(names[0], "invalid-version-secret");
      System.setProperty(names[2], "invalid-session-secret");
      String line = new String(new SafeLogEncoder().encode(event), StandardCharsets.UTF_8);
      assertEquals("unknown", json.readTree(line).get("appVersion").asString());
      assertEquals("standalone", json.readTree(line).get("sessionId").asString());
      assertFalse(line.contains("invalid-version-secret"));
      assertFalse(line.contains("invalid-session-secret"));
      assertFalse(line.contains("never-copy-this-secret"));
    } finally {
      for (int i = 0; i < names.length; i++) {
        if (old[i] == null) System.clearProperty(names[i]);
        else System.setProperty(names[i], old[i]);
      }
    }
  }

  @Test
  void actionRoutesPreserveTheOperationWithoutIDsOrQueryData() {
    assertEquals("/login", SafeRoutes.canonical("/login"));
    assertEquals("/admin/reservas/{id}/aprovar", SafeRoutes.canonical("/admin/reservas/123/aprovar"));
    assertEquals("/admin/cadastros/espacos/{id}/excluir", SafeRoutes.canonical("/admin/cadastros/espacos/19/excluir"));
    assertEquals("/admin/solicitacoes/{id}/aceitar", SafeRoutes.canonical("/admin/solicitacoes/22/aceitar"));
    assertEquals("ASSET", SafeRoutes.canonical("/js/anything-secret.js"));
    assertEquals("OTHER", SafeRoutes.canonical("/private-email@example.com"));
    assertEquals("OTHER", SafeRoutes.canonical("/login?password=private-secret"));
  }

  @Test
  void retentionKeepsRecentArchivesAndDeletesOnlyExpiredOrExcessFiles() throws Exception {
    var now = java.time.Instant.parse("2026-10-15T18:00:00Z");
    var clock = java.time.Clock.fixed(now, java.time.ZoneOffset.UTC);
    Path expired = Files.writeString(temp.resolve("inpetu.2026-10-01.0.log.gz"), "old");
    Path older = Files.writeString(temp.resolve("inpetu.2026-10-02.0.log.gz"), "12345678");
    Path newest = Files.writeString(temp.resolve("inpetu.2026-10-15.1.log.gz"), "12345678");
    Path fresh = Files.writeString(temp.resolve("inpetu.2026-10-15.2.log.gz"), "compressing");
    Path active = Files.writeString(temp.resolve("inpetu.log"), "preserve");
    Path other = Files.writeString(temp.resolve("unrelated.log.gz"), "preserve");
    for (Path p : List.of(expired, older, newest))
      Files.setLastModifiedTime(p, java.nio.file.attribute.FileTime.from(now.minusSeconds(120)));
    Files.setLastModifiedTime(fresh, java.nio.file.attribute.FileTime.from(now));
    LogRetention.clean(temp, clock, 16);
    assertFalse(Files.exists(expired));
    assertTrue(Files.exists(older));
    LogRetention.clean(temp, clock, 8);
    assertFalse(Files.exists(older));
    assertTrue(Files.exists(newest));
    assertTrue(Files.exists(fresh));
    assertEquals("preserve", Files.readString(active));
    assertEquals("preserve", Files.readString(other));
  }

  @Test
  void logDirectoryIsPrivateAndRejectsUnexpectedPaths() throws Exception {
    Path logs = temp.resolve("logs");
    Path session = Files.createDirectories(logs.resolve("sessoes/teste01"));
    Path events = Files.writeString(session.resolve("eventos.jsonl"), "preserve");
    PrivateLogDirectory.prepare(logs);
    assertTrue(Files.isRegularFile(logs.resolve("inpetu.log")));
    assertEquals("preserve", Files.readString(events));
    if (Files.getFileAttributeView(logs, java.nio.file.attribute.PosixFileAttributeView.class)
        != null) {
      assertEquals(
          java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"),
          Files.getPosixFilePermissions(logs));
      assertEquals(
          java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"),
          Files.getPosixFilePermissions(logs.resolve("inpetu.log")));
      assertEquals(
          java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"),
          Files.getPosixFilePermissions(session));
      assertEquals(
          java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"),
          Files.getPosixFilePermissions(events));
      Path outside = Files.writeString(temp.resolve("outside"), "preserve");
      Files.createSymbolicLink(logs.resolve("unexpected.log"), outside);
      assertThrows(IllegalStateException.class, () -> PrivateLogDirectory.prepare(logs));
      assertEquals("preserve", Files.readString(outside));
    } else {
      var acl =
          Files.getFileAttributeView(logs, java.nio.file.attribute.AclFileAttributeView.class);
      assertNotNull(acl);
      assertEquals(1, acl.getAcl().size());
      assertEquals(Files.getOwner(logs), acl.getAcl().get(0).principal());
      assertTrue(
          acl.getAcl().get(0).flags().contains(java.nio.file.attribute.AclEntryFlag.FILE_INHERIT));
    }
    Path invalid = Files.writeString(temp.resolve("not-a-folder"), "preserve");
    assertThrows(IllegalStateException.class, () -> PrivateLogDirectory.prepare(invalid));
  }

  @Test
  void freeTextSecretsAndExceptionMessagesAreNotSerialized() {
    var e = new LoggingEvent();
    e.setTimeStamp(0);
    e.setLevel(Level.ERROR);
    e.setLoggerName("org.example.Database");
    e.setMessage("password=synthetic-secret user=private@example.com cookie=session-value");
    e.setMDCPropertyMap(Map.of("requestId", "invalid-secret", "token", "mdc-secret"));
    e.setThrowableProxy(
        new ThrowableProxy(
            new IllegalStateException("exception-secret", new RuntimeException("cause-secret"))));
    String line = new String(new SafeLogEncoder().encode(e), StandardCharsets.UTF_8);
    var node = json.readTree(line);
    assertEquals("FRAMEWORK_EVENT", node.get("message").asString());
    assertEquals("java.lang.IllegalStateException", node.get("exceptionType").asString());
    assertFalse(node.get("frames").isEmpty());
    for (String secret :
        List.of(
            "synthetic-secret",
            "private@example.com",
            "session-value",
            "invalid-secret",
            "mdc-secret",
            "exception-secret",
            "cause-secret")) assertFalse(line.contains(secret), secret);
    assertEquals("-", node.get("requestId").asString());
    assertEquals("a\n\"b", json.readTree(SafeLogEncoder.quote("a\n\"b")).asString());
  }

  private tools.jackson.databind.JsonNode encodeFailure(Throwable failure) {
    var e = new LoggingEvent();
    e.setTimeStamp(0);
    e.setLevel(Level.ERROR);
    e.setLoggerName("org.springframework.boot.SpringApplication");
    e.setMessage("Application run failed");
    e.setMDCPropertyMap(Map.of());
    e.setThrowableProxy(new ThrowableProxy(failure));
    String line = new String(new SafeLogEncoder().encode(e), StandardCharsets.UTF_8);
    // Nada das mensagens da cadeia pode sair: SQL com valores, caminhos com nome de usuário, senhas.
    for (String secret :
        List.of(
            "12345678000190",
            "maria@example.com",
            "Fulano",
            "/home/fulano",
            "senha-secreta",
            "inpetu-v02.mv.db",
            "bean-secret",
            "VALUES"))
      assertFalse(line.contains(secret), secret + " vazou: " + line);
    return json.readTree(line);
  }

  @Test
  void startupFailureKeepsTheRealCauseWithoutMessages() {
    var sql =
        new java.sql.SQLException(
            "Unique index violation: VALUES ('12345678000190', 'maria@example.com')", "23505", 23505);
    var node =
        encodeFailure(
            new IllegalStateException(
                "bean-secret C:\\Users\\Fulano\\InPETU-Hub", new RuntimeException("senha-secreta", sql)));
    assertEquals("java.lang.IllegalStateException", node.get("exceptionType").asString());
    var causes = new ArrayList<String>();
    node.get("causes").forEach(c -> causes.add(c.asString()));
    assertEquals(List.of("java.lang.RuntimeException", "java.sql.SQLException"), causes);
    assertFalse(node.get("rootFrames").isEmpty());
    assertEquals("DATABASE_ERROR", node.get("failure").asString());
    assertEquals("23505", node.get("sqlState").asString());
    assertEquals(23505, node.get("sqlErrorCode").asInt());
  }

  @Test
  void startupFailureIsClassifiedByItsCause() {
    assertEquals(
        "PORT_IN_USE",
        encodeFailure(new RuntimeException(new java.net.BindException("Address in use /home/fulano")))
            .get("failure")
            .asString());
    assertEquals(
        "DATABASE_IN_USE",
        encodeFailure(
                new RuntimeException(
                    new java.sql.SQLException(
                        "Database may be already in use: /home/fulano/inpetu-v02.mv.db", "90020", 90020)))
            .get("failure")
            .asString());
    assertEquals(
        "MIGRATION_FAILED",
        encodeFailure(new RuntimeException(new org.flywaydb.core.api.FlywayException("senha-secreta")))
            .get("failure")
            .asString());
    assertEquals(
        "FILE_ACCESS",
        encodeFailure(new java.nio.file.AccessDeniedException("/home/fulano/.secrets/mfa.key"))
            .get("failure")
            .asString());
    var other = encodeFailure(new IllegalArgumentException("bean-secret"));
    assertEquals("OTHER", other.get("failure").asString());
    assertNull(other.get("causes"));
    assertNull(other.get("sqlState"));
    // Um SQLState fora do padrão não é gravado (pode vir de um driver com texto livre).
    assertNull(
        encodeFailure(new java.sql.SQLException("x", "senha-secreta", 1)).get("sqlState"));
  }

  @Test
  void startupWaitIsOneWarningAndTheReadyEventCountsIt() throws Exception {
    var logger = (Logger) LoggerFactory.getLogger(OperationalEvents.class);
    var capture = new ListAppender<ILoggingEvent>();
    capture.start();
    logger.addAppender(capture);
    try {
      var log = new RequestLogFilter();
      var starting =
          new br.com.inpetu.hub.security.AbuseFilter(null, null, false, () -> false);
      for (int i = 0; i < 3; i++) {
        var response = new MockHttpServletResponse();
        log.doFilter(
            new MockHttpServletRequest("GET", "/login"),
            response,
            (a, b) -> starting.doFilter(a, b, (c, d) -> fail("não deveria passar enquanto inicia")));
        assertEquals(503, response.getStatus());
      }
      OperationalEvents.lifecycle(true);
      var waits = capture.list.stream().filter(e -> e.getFormattedMessage().startsWith("event=SERVER_STARTING_WAIT")).toList();
      assertEquals(1, waits.size());
      assertEquals(Level.WARN, waits.get(0).getLevel());
      assertTrue(capture.list.stream().noneMatch(e -> e.getLevel() == Level.ERROR), capture.list.toString());
      assertTrue(
          capture.list.stream().anyMatch(e -> e.getFormattedMessage().equals("event=SERVER_READY startupWaits=3")),
          capture.list.toString());
      // Um 503 que não é a espera da inicialização continua sendo erro.
      capture.list.clear();
      log.doFilter(
          new MockHttpServletRequest("GET", "/admin"),
          new MockHttpServletResponse(),
          (a, b) -> ((jakarta.servlet.http.HttpServletResponse) b).setStatus(503));
      assertEquals(Level.ERROR, capture.list.get(0).getLevel());
    } finally {
      logger.detachAppender(capture);
      capture.stop();
    }
  }

  @Test
  void causeChainAndRootFramesAreLimited() {
    Throwable deep = new IllegalStateException("bean-secret");
    deep.setStackTrace(
        java.util.stream.IntStream.range(0, 30)
            .mapToObj(i -> new StackTraceElement("example.Deep", "step" + i, "Deep.java", i + 1))
            .toArray(StackTraceElement[]::new));
    Throwable top = deep;
    for (int i = 0; i < 12; i++) top = new RuntimeException("bean-secret", top);
    var node = encodeFailure(top);
    assertEquals(8, node.get("causes").size());
    assertEquals(10, node.get("rootFrames").size());
    // Código de erro negativo (estilo DB2) fica fora: o iniciador e o receptor só aceitam 0 a 1e9.
    assertNull(encodeFailure(new java.sql.SQLException("x", "23505", -803)).get("sqlErrorCode"));
  }

  @Test
  void a503AfterTheServerWasReadyIsStillAnError() throws Exception {
    var logger = (Logger) LoggerFactory.getLogger(OperationalEvents.class);
    var capture = new ListAppender<ILoggingEvent>();
    capture.start();
    logger.addAppender(capture);
    try {
      boolean[] accepting = {true};
      var crypto = new br.com.inpetu.hub.security.SecretCrypto("", br.com.inpetu.hub.security.SecretCrypto.random(32));
      var filter = new br.com.inpetu.hub.security.AbuseFilter(null, crypto, false, () -> accepting[0]);
      var log = new RequestLogFilter();
      log.doFilter(new MockHttpServletRequest("GET", "/agenda"), new MockHttpServletResponse(), (a, b) -> filter.doFilter(a, b, (c, d) -> {}));
      accepting[0] = false; // encerramento: o readiness volta a recusar tráfego
      capture.list.clear();
      var response = new MockHttpServletResponse();
      log.doFilter(new MockHttpServletRequest("GET", "/agenda"), response, (a, b) -> filter.doFilter(a, b, (c, d) -> {}));
      assertEquals(503, response.getStatus());
      assertTrue(capture.list.stream().noneMatch(e -> e.getFormattedMessage().startsWith("event=SERVER_STARTING_WAIT")), capture.list.toString());
      assertEquals(Level.ERROR, capture.list.get(0).getLevel());
    } finally {
      logger.detachAppender(capture);
      capture.stop();
    }
  }

  @Test
  void startFailedEventNamesTheCategory() {
    var logger = (Logger) LoggerFactory.getLogger(OperationalEvents.class);
    var capture = new ListAppender<ILoggingEvent>();
    capture.start();
    logger.addAppender(capture);
    try {
      OperationalEvents.startFailed(new RuntimeException(new java.net.BindException("bean-secret")));
      assertEquals("event=SERVER_START_FAILED failure=PORT_IN_USE", capture.list.get(0).getFormattedMessage());
      assertEquals(Level.ERROR, capture.list.get(0).getLevel());
    } finally {
      logger.detachAppender(capture);
      capture.stop();
    }
  }

  @Test
  void requestIdIsGeneratedAndMdcIsClearedEvenOnFailure() throws Exception {
    var filter = new RequestLogFilter();
    var req = new MockHttpServletRequest("GET", "/private-secret?password=never-log");
    req.addHeader("X-Request-ID", "client-secret");
    req.addHeader("Cookie", "session-secret");
    var response = new MockHttpServletResponse();
    String[] current = new String[1];
    MDC.remove("requestId");
    filter.doFilter(
        req,
        response,
        (a, b) -> {
          current[0] = MDC.get("requestId");
        });
    assertEquals(current[0], response.getHeader("X-Request-ID"));
    assertDoesNotThrow(() -> UUID.fromString(current[0]));
    assertNull(MDC.get("requestId"));
    MDC.put("requestId", "previous");
    try {
      assertThrows(
          ServletException.class,
          () ->
              filter.doFilter(
                  new MockHttpServletRequest("POST", "/"),
                  new MockHttpServletResponse(),
                  (a, b) -> {
                    throw new ServletException("hidden-secret");
                  }));
      assertEquals("previous", MDC.get("requestId"));
    } finally {
      MDC.remove("requestId");
    }
  }

  @Test
  void operationalEventsRejectFreeTextFields() {
    var logger = (Logger) LoggerFactory.getLogger(OperationalEvents.class);
    var capture = new ListAppender<ILoggingEvent>();
    capture.start();
    logger.addAppender(capture);
    try {
      OperationalEvents.audit(7L, "private@example.com", "target-secret", 9L, "result-secret");
      OperationalEvents.request("METHOD-SECRET", 403, 2, null);
      String text = capture.list.toString();
      assertTrue(text.contains("action=OTHER"));
      assertTrue(text.contains("method=OTHER"));
      assertFalse(text.contains("private@example.com"));
      assertFalse(text.contains("target-secret"));
      assertFalse(text.contains("result-secret"));
      assertFalse(text.contains("METHOD-SECRET"));
    } finally {
      logger.detachAppender(capture);
      capture.stop();
    }
  }

  @Test
  void sizeRotationProducesCompressedValidJsonAndPreservesMessages() throws Exception {
    var context = new LoggerContext();
    context.setMDCAdapter(new ch.qos.logback.classic.util.LogbackMDCAdapter());
    context.start();
    var encoder = new SafeLogEncoder();
    encoder.setContext(context);
    encoder.start();
    var appender = new RollingFileAppender<ILoggingEvent>();
    appender.setContext(context);
    appender.setName("FILE");
    appender.setFile(temp.resolve("inpetu.log").toString());
    appender.setEncoder(encoder);
    var policy = new SizeAndTimeBasedRollingPolicy<ILoggingEvent>();
    policy.setContext(context);
    policy.setParent(appender);
    policy.setFileNamePattern(temp.resolve("inpetu.%d{yyyy-MM-dd}.%i.log.gz").toString());
    policy.setMaxFileSize(FileSize.valueOf("1KB"));
    policy.setMaxHistory(0);
    policy.start();
    appender.setRollingPolicy(policy);
    appender.start();
    var logger = context.getLogger(OperationalEvents.class);
    logger.setLevel(Level.INFO);
    logger.addAppender(appender);
    for (int i = 0; i < 160; i++) logger.info("event=HTTP status=200 durationMs={}", i);
    policy
        .getTimeBasedFileNamingAndTriggeringPolicy()
        .setCurrentTime(System.currentTimeMillis() + 86400000L);
    logger.info("event=HTTP status=200 durationMs=161");
    appender.stop();
    policy.stop();
    // A compressão usa mais de um worker; aguardar todos, não somente o último future.
    context.getExecutorService().shutdown();
    assertTrue(
        context.getExecutorService().awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS));
    context.stop();
    int count = 0, archives = 0;
    try (var files = Files.list(temp)) {
      for (Path p : files.toList()) {
        String text;
        if (p.toString().endsWith(".gz")) {
          archives++;
          try (var in = new GZIPInputStream(Files.newInputStream(p))) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          }
        } else text = Files.readString(p);
        for (String line : text.lines().toList()) {
          assertTrue(json.readTree(line).get("message").asString().startsWith("event=HTTP"));
          count++;
        }
      }
    }
    if (archives == 0 || count != 161) ch.qos.logback.core.util.StatusPrinter.print(context);
    assertTrue(archives > 0, "Rotação não ocorreu; linhas=" + count);
    assertEquals(161, count);
  }
}
