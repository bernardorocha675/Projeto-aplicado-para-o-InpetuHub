import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/** Executor local: usa somente Java e as ferramentas já instaladas pelo configurador. */
class Testar {
  record Result(String title, String status, String detail) {}

  interface Check {
    void run() throws Exception;
  }

  static class ExpectedFailure extends IOException {
    ExpectedFailure(String message) {
      super(message);
    }
  }

  static final Path ROOT = Path.of("").toAbsolutePath().normalize();
  static final boolean WINDOWS =
      System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
  static final Path JAVA =
      Path.of(System.getProperty("java.home"), "bin", WINDOWS ? "java.exe" : "java");
  static final SecureRandom RANDOM = new SecureRandom();
  static final List<Result> RESULTS = new ArrayList<>();
  static final List<Process> CHILDREN = Collections.synchronizedList(new ArrayList<>());
  static Path work, logs, report, isolated, transientData;
  static String base;
  static Process server;
  static int sequence;

  static String random(int bytes) {
    byte[] b = new byte[bytes];
    RANDOM.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  static void protect(Path p) throws Exception {
    if (Files.isSymbolicLink(p)) throw new IOException("Link simbólico recusado.");
    if (Files.getFileAttributeView(p, PosixFileAttributeView.class) != null)
      Files.setPosixFilePermissions(
          p, PosixFilePermissions.fromString(Files.isDirectory(p) ? "rwx------" : "rw-------"));
    var acl = Files.getFileAttributeView(p, AclFileAttributeView.class);
    if (acl != null)
      acl.setAcl(
          List.of(
              AclEntry.newBuilder()
                  .setType(AclEntryType.ALLOW)
                  .setPrincipal(
                      FileSystems.getDefault()
                          .getUserPrincipalLookupService()
                          .lookupPrincipalByName(System.getProperty("user.name")))
                  .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                  .build()));
  }

  static Path directory(Path p) throws Exception {
    Files.createDirectories(p);
    protect(p);
    return p;
  }

  static void write(Path p, String value) throws Exception {
    if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS)) Files.createFile(p);
    protect(p);
    Files.writeString(p, value, StandardCharsets.UTF_8);
  }

  static void require(boolean condition, String message) throws IOException {
    if (!condition) throw new ExpectedFailure(message);
  }

  static boolean check(String title, Check action) {
    System.out.println("Verificando: " + title);
    try {
      action.run();
      RESULTS.add(new Result(title, "PASSOU", "Verificação concluída."));
      System.out.println("  PASSOU");
      return true;
    } catch (Exception ex) {
      // Não copiar respostas HTML, segredos ou mensagens de processos para o relatório.
      RESULTS.add(
          new Result(
              title,
              "FALHOU",
              ex instanceof ExpectedFailure
                  ? ex.getMessage()
                  : "O resultado esperado não foi confirmado. Consulte o guia TESTAR.md e os logs"
                      + " locais privados."));
      System.out.println("  FALHOU - confira o relatório; nenhuma credencial foi exibida.");
      return false;
    }
  }

  static void stop(Process p) {
    if (p == null) return;
    var descendants = p.descendants().toList();
    for (var child : descendants) child.destroy();
    p.destroy();
    try {
      if (!p.waitFor(15, TimeUnit.SECONDS)) p.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      p.destroyForcibly();
    }
    for (var child : descendants) if (child.isAlive()) child.destroyForcibly();
    CHILDREN.remove(p);
  }

  static Process start(List<String> command, Path cwd, Path log) throws Exception {
    write(log, "");
    var builder =
        new ProcessBuilder(command)
            .directory(cwd.toFile())
            .redirectErrorStream(true)
            .redirectOutput(log.toFile());
    // Nenhuma configuração de banco, conta ou SMTP da instalação normal é herdada.
    builder
        .environment()
        .keySet()
        .removeIf(
            k -> {
              String key = k.toUpperCase(Locale.ROOT);
              return key.startsWith("SPRING_")
                  || key.startsWith("DB_")
                  || key.startsWith("MAIL_")
                  || key.startsWith("APP_")
                  || key.startsWith("TLS_")
                  || key.equals("PORT")
                  || Set.of(
                          "ACCOUNTS_FILE",
                          "MFA_KEY_FILE",
                          "LOCAL_DB_PASSWORD",
                          "SEED_DEMO",
                          "SERVER_ADDRESS",
                          "JAVA_TOOL_OPTIONS",
                          "JDK_JAVA_OPTIONS",
                          "_JAVA_OPTIONS")
                      .contains(key);
            });
    Process p = builder.start();
    CHILDREN.add(p);
    return p;
  }

  static int command(List<String> args, Path cwd, String input, int seconds) throws Exception {
    Process p =
        start(args, cwd, logs.resolve(String.format(Locale.ROOT, "%02d-processo.log", ++sequence)));
    try {
      try (var out = p.outputWriter(StandardCharsets.UTF_8)) {
        if (input != null) out.write(input);
      }
      long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
      while (!p.waitFor(15, TimeUnit.SECONDS)) {
        System.out.println("  Ainda executando; aguarde...");
        if (System.nanoTime() > end) throw new IOException("Prazo de execução excedido.");
      }
      return p.exitValue();
    } finally {
      stop(p);
    }
  }

  static List<String> source(Path script, String... args) {
    var result =
        new ArrayList<String>(
            List.of(JAVA.toString(), "-Dfile.encoding=UTF-8", "--source", "17", script.toString()));
    result.addAll(List.of(args));
    return result;
  }

  static String friendly(String className, String method) {
    String group =
        className.contains("Concorrencia")
            ? "Concorrência"
            : className.contains("Security")
                ? "Segurança"
                : className.contains("Regras") ? "Regras" : "Fluxos";
    String label = method.replaceAll("\\[.*", "");
    label =
        switch (label) {
          case "previousSessionsAreRevoked" -> "Alteração de conta revoga sessões anteriores";
          case "adminRequiresEnrollmentAndRecoveryCannotBeReused" ->
              "MFA obrigatório e recuperação de uso único";
          case "collaboratorCannotReadOrWriteAdministration" ->
              "Colaborador não acessa administração";
          case "badLoginsAreThrottledWithoutTrustingForwardedIp" ->
              "Limite de login e proteção contra IP forjado";
          case "publicIntakeDoesNotPoisonCrmOrReserveQuotaAndIsIdempotent" ->
              "Solicitação pública não altera CRM nem cota";
          case "confirmedStaffReviewAndApprovalRemainFunctional" ->
              "Conferência e aprovação de solicitação";
          case "refusingOrCancellingUnallocatedPendingCannotCreateQuotaCredit" ->
              "Recusa não gera crédito indevido de cota";
          case "emailCodeIsSingleUseAndBounded" -> "Confirmação de email de uso único";
          case "headersCsrfAndEscapingArePreserved" -> "Cabeçalhos HTTP e proteção CSRF";
          case "crudChangesRevokeSessionsAndAuditContainsNoCredentialValues" ->
              "Revogação de sessão e auditoria sem credenciais";
          case "fivePrivateAccountsAreImportedOnceAndLegacyAccountsAreRevoked" ->
              "Importação única de cinco contas sintéticas";
          case "emailConfirmationHasFiveAttemptLimitAndExpiry" ->
              "Limite e expiração de confirmação por email";
          case "encodedInternalPathCannotSkipSecondFactor" -> "URL codificada não contorna MFA";
          case "encodedLoginCannotSkipThrottle" -> "URL codificada não contorna limite de login";
          case "totpMatchesRfc6238AndRejectsReplay" -> "Cálculo TOTP e rejeição de repetição";
          case "rateLimitDoesNotPermanentlyLockVictim" ->
              "Limite de tentativas termina após o prazo";
          case "provisioningValidatesAllEntriesWithoutExposingThem" ->
              "Validação das contas sem expor valores";
          case "expiredSessionsAndPendingMfaAreRevoked" -> "Prazos absolutos de sessão e MFA";
          case "expiredIntakeAndReservationCannotBeAccepted" ->
              "Expiração de solicitação e reserva pendente";
          default -> label.replaceAll("([a-zà-ü])([A-ZÀ-Ü])", "$1 $2");
        };
    return group + ": " + label;
  }

  static void junitResults(long since) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    int count = 0;
    try (var files =
        Files.newDirectoryStream(ROOT.resolve("target/surefire-reports"), "TEST-*.xml")) {
      for (Path file : files) {
        if (Files.getLastModifiedTime(file).toMillis() < since) continue;
        NodeList cases =
            factory.newDocumentBuilder().parse(file.toFile()).getElementsByTagName("testcase");
        for (int i = 0; i < cases.getLength(); i++) {
          Element test = (Element) cases.item(i);
          String state =
              test.getElementsByTagName("failure").getLength()
                          + test.getElementsByTagName("error").getLength()
                      > 0
                  ? "FALHOU"
                  : test.getElementsByTagName("skipped").getLength() > 0
                      ? "NÃO EXECUTADO"
                      : "PASSOU";
          RESULTS.add(
              new Result(
                  friendly(test.getAttribute("classname"), test.getAttribute("name")),
                  state,
                  state.equals("PASSOU")
                      ? "Resultado confirmado no banco de testes."
                      : "Revise o log privado da compilação; não há comprovação de aprovação deste"
                          + " caso."));
          count++;
        }
      }
    }
    require(count > 0, "Nenhum teste foi comprovado.");
    require(RESULTS.stream().noneMatch(r -> !r.status.equals("PASSOU")), "Há casos não aprovados.");
  }

  static HttpClient client() {
    return HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .proxy(
            new ProxySelector() {
              public List<Proxy> select(URI uri) {
                return List.of(Proxy.NO_PROXY);
              }

              public void connectFailed(URI uri, SocketAddress sa, IOException ex) {}
            })
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  static HttpResponse<String> get(HttpClient client, String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build(),
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  static String csrf(String html) throws Exception {
    Matcher m = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
    require(m.find(), "Token CSRF ausente.");
    return m.group(1);
  }

  static HttpResponse<String> post(HttpClient client, String path, Map<String, String> values)
      throws Exception {
    var body = new StringBuilder();
    for (var entry : values.entrySet()) {
      if (!body.isEmpty()) body.append('&');
      body.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
          .append('=')
          .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    return client.send(
        HttpRequest.newBuilder(URI.create(base + path))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build(),
        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
  }

  static HttpResponse<String> login(HttpClient c, String login, String password) throws Exception {
    String token = csrf(get(c, "/login").body());
    return post(c, "/login", Map.of("username", login, "password", password, "_csrf", token));
  }

  static void startServer(Path folder) throws Exception {
    int port;
    try (var socket = new java.net.ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
      port = socket.getLocalPort();
    }
    base = "http://127.0.0.1:" + port;
    var args =
        List.of(
            JAVA.toString(),
            "-Dfile.encoding=UTF-8",
            "-jar",
            ROOT.resolve("target/inpetu-hub-0.2.0.jar").toString(),
            "--spring.profiles.active=local",
            "--server.port=" + port,
            "--spring.config.import=",
            "--app.accounts-file=" + folder.resolve(".secrets/accounts.properties"),
            "--app.security.key-file=" + folder.resolve(".secrets/mfa.key"),
            "--app.seed-demo=true",
            "--spring.mail.username=",
            "--spring.mail.password=");
    // A senha do banco vem somente do arquivo de configuração descartável.
    var safeArgs = new ArrayList<>(args);
    safeArgs.remove("--spring.config.import=");
    safeArgs.add("--spring.config.import=file:./.env.local[.properties]");
    server = start(safeArgs, folder, logs.resolve("servidor-descartavel.log"));
    HttpClient c = client();
    for (int i = 0; i < 120; i++) {
      require(server.isAlive(), "Servidor de testes encerrou durante a inicialização.");
      try {
        if (get(c, "/").statusCode() == 200) return;
      } catch (IOException ignored) {
      }
      Thread.sleep(1000);
      if (i % 15 == 14) System.out.println("  Aguardando o servidor de testes...");
    }
    throw new IOException("Servidor de testes não ficou pronto.");
  }

  static String totp(String key) throws Exception {
    var out = new ByteArrayOutputStream();
    int value = 0, bits = 0;
    for (char ch : key.toCharArray()) {
      int digit = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".indexOf(ch);
      require(digit >= 0, "Chave sintética inválida.");
      value = (value << 5) | digit;
      bits += 5;
      if (bits >= 8) {
        out.write((value >> (bits - 8)) & 255);
        bits -= 8;
      }
    }
    Mac mac = Mac.getInstance("HmacSHA1");
    mac.init(new SecretKeySpec(out.toByteArray(), "HmacSHA1"));
    byte[] hash =
        mac.doFinal(ByteBuffer.allocate(8).putLong(Instant.now().getEpochSecond() / 30).array());
    int o = hash[hash.length - 1] & 15;
    int number =
        ((hash[o] & 127) << 24)
            | ((hash[o + 1] & 255) << 16)
            | ((hash[o + 2] & 255) << 8)
            | (hash[o + 3] & 255);
    return String.format(Locale.ROOT, "%06d", number % 1000000);
  }

  static boolean webTests() throws Exception {
    isolated = directory(transientData.resolve("instalacao-descartavel"));
    directory(isolated.resolve(".secrets"));
    directory(isolated.resolve("data"));
    String adminLogin = "teste-" + random(9),
        employeeLogin = "teste-" + random(9),
        password = random(24);
    String[] effectivePassword = {password};
    write(
        isolated.resolve(".secrets/accounts.properties"),
        "account.1.name=Teste gestor\naccount.1.login="
            + adminLogin
            + "\naccount.1.password="
            + password
            + "\naccount.1.role=ADMIN\naccount.2.name=Teste colaborador\naccount.2.login="
            + employeeLogin
            + "\naccount.2.password="
            + password
            + "\naccount.2.role=COLABORADOR\n");
    write(isolated.resolve(".secrets/mfa.key"), random(32));
    write(isolated.resolve(".env.local"), "LOCAL_DB_PASSWORD=" + random(32) + "\n");
    if (!check("Servidor inicia em instalação descartável", () -> startServer(isolated))) {
      stop(server);
      return false;
    }
    try {
      HttpClient guest = client();
      check(
          "Agenda pública e cabeçalhos de segurança",
          () -> {
            var response = get(guest, "/");
            require(
                response.statusCode() == 200
                    && response
                        .headers()
                        .firstValue("Content-Security-Policy")
                        .orElse("")
                        .contains("script-src 'self'"),
                "Cabeçalho ausente.");
            require(get(guest, "/disponibilidade").statusCode() == 200, "Agenda indisponível.");
          });
      check(
          "Página privada exige login; POST sem CSRF é recusado",
          () -> {
            require(
                get(guest, "/admin").uri().getPath().equals("/login"), "Acesso privado liberado.");
            require(
                post(guest, "/reservas/solicitar", Map.of()).statusCode() == 403,
                "CSRF ausente não foi recusado.");
          });
      HttpClient employee = client();
      check(
          "Login de colaborador e bloqueio da configuração",
          () -> {
            require(
                login(employee, employeeLogin, password).uri().getPath().equals("/admin"),
                "Login não confirmado.");
            require(
                get(employee, "/admin/config").statusCode() == 403
                    && get(employee, "/%61dmin/config").statusCode() == 403,
                "Configuração acessível ao colaborador.");
          });
      HttpClient admin = client();
      if (check(
          "Administrador precisa do segundo fator, inclusive em URL codificada",
          () -> {
            require(
                login(admin, adminLogin, password).uri().getPath().equals("/conta/mfa"),
                "MFA não exigido.");
            require(
                get(admin, "/%61dmin/config").uri().getPath().equals("/conta/mfa"),
                "MFA contornado.");
          })) {
        check(
            "Ativação TOTP e códigos de recuperação no servidor real",
            () -> {
              String page = get(admin, "/conta/mfa").body();
              Matcher m = Pattern.compile("<code>([A-Z2-7]{32})</code>").matcher(page);
              require(m.find(), "Chave não encontrada.");
              String code = totp(m.group(1));
              var response = post(admin, "/conta/mfa", Map.of("code", code, "_csrf", csrf(page)));
              int codes = 0;
              Matcher recovery = Pattern.compile("<code>[^<]+</code>").matcher(response.body());
              while (recovery.find()) codes++;
              require(response.statusCode() == 200 && codes == 8, "Recuperação não confirmada.");
              require(
                  get(admin, "/admin/config").statusCode() == 200,
                  "MFA não liberou administração.");
              require(
                  !get(admin, "/conta/mfa").body().contains("Guarde estes códigos"),
                  "Recuperação foi exibida novamente.");
            });
      }
      check(
          "Login codificado respeita o limite de tentativas",
          () -> {
            HttpClient bad = client();
            String token = csrf(get(bad, "/login").body());
            String unknown = "inexistente-" + random(9);
            for (int i = 0; i < 10; i++)
              require(
                  post(
                          bad,
                          "/l%6fgin",
                          Map.of("username", unknown, "password", "incorreta", "_csrf", token))
                      .uri()
                      .getQuery()
                      .contains("error"),
                  "Falha inesperada de login.");
            require(
                post(
                            bad,
                            "/l%6fgin",
                            Map.of("username", unknown, "password", "incorreta", "_csrf", token))
                        .statusCode()
                    == 429,
                "Limite não aplicado.");
          });
      check(
          "Troca de senha da conta descartável revoga outra sessão",
          () -> {
            HttpClient second = client();
            require(
                login(second, employeeLogin, password).uri().getPath().equals("/admin"),
                "Segunda sessão não abriu.");
            String page = get(employee, "/conta/senha").body(), replacement = random(24);
            require(
                post(
                        employee,
                        "/conta/senha",
                        Map.of(
                            "current", password, "replacement", replacement, "_csrf", csrf(page)))
                    .uri()
                    .getPath()
                    .equals("/login"),
                "Troca não confirmada.");
            require(
                get(second, "/admin").uri().getPath().equals("/login"),
                "Sessão antiga continua válida.");
            require(
                login(client(), employeeLogin, password).uri().getQuery().contains("error"),
                "Senha antiga ainda aceita.");
            require(
                login(client(), employeeLogin, replacement).uri().getPath().equals("/admin"),
                "Senha nova recusada.");
            effectivePassword[0] = replacement;
          });
      check(
          "Backup recusa banco com servidor em execução",
          () -> {
            Path archive = transientData.resolve("nao-deve-existir.inpetu");
            require(
                command(
                            source(
                                ROOT.resolve("scripts/Backup.java"), "backup", archive.toString()),
                            isolated,
                            random(24) + "\n",
                            90)
                        != 0
                    && !Files.exists(archive),
                "Backup em uso foi aceito.");
          });
    } finally {
      stop(server);
      server = null;
    }
    return check(
        "Banco e senha descartável persistem após reiniciar",
        () -> {
          require(
              Files.isRegularFile(isolated.resolve("data/inpetu-v02.mv.db")),
              "Banco H2 não encontrado.");
          try {
            startServer(isolated);
            require(
                login(client(), employeeLogin, effectivePassword[0])
                    .uri()
                    .getPath()
                    .equals("/admin"),
                "Dados não persistiram.");
          } finally {
            stop(server);
            server = null;
          }
        });
  }

  static void backupTests() throws Exception {
    Path archive = transientData.resolve("backup-descartavel.inpetu");
    String password = random(24);
    if (!check(
        "Backup cifrado de banco H2 real e restauração íntegra",
        () -> {
          require(
              command(
                      source(ROOT.resolve("scripts/Backup.java"), "backup", archive.toString()),
                      isolated,
                      password + "\n",
                      90)
                  == 0,
              "Backup não criado.");
          Path target = directory(transientData.resolve("restaurado"));
          require(
              command(
                      source(ROOT.resolve("scripts/Backup.java"), "restore", archive.toString()),
                      target,
                      password + "\n",
                      90)
                  == 0,
              "Restauração recusada.");
          for (String name :
              List.of(
                  ".env.local",
                  ".secrets/mfa.key",
                  ".secrets/accounts.properties",
                  "data/inpetu-v02.mv.db"))
            require(
                Files.mismatch(isolated.resolve(name), target.resolve(name)) == -1,
                "Conteúdo restaurado diferente.");
        })) return;
    check(
        "Backup existente não é apagado nem sobrescrito",
        () -> {
          byte[] before = Files.readAllBytes(archive);
          require(
              command(
                      source(ROOT.resolve("scripts/Backup.java"), "backup", archive.toString()),
                      isolated,
                      password + "\n",
                      90)
                  != 0,
              "Arquivo sobrescrito.");
          require(Arrays.equals(before, Files.readAllBytes(archive)), "Arquivo modificado.");
        });
    check(
        "Senha errada e arquivo adulterado não extraem dados",
        () -> {
          Path tampered = transientData.resolve("adulterado.inpetu");
          byte[] bytes = Files.readAllBytes(archive);
          bytes[bytes.length - 1] ^= 1;
          Files.write(tampered, bytes);
          protect(tampered);
          int i = 0;
          for (Path input : List.of(archive, tampered)) {
            Path folder = directory(transientData.resolve("recusado-" + i));
            String key = i++ == 0 ? password + "-errada" : password;
            require(
                command(
                        source(ROOT.resolve("scripts/Backup.java"), "restore", input.toString()),
                        folder,
                        key + "\n",
                        90)
                    != 0,
                "Arquivo não autenticado aceito.");
            require(
                !Files.exists(folder.resolve("data"))
                    && !Files.exists(folder.resolve(".env.local")),
                "Dados foram extraídos.");
          }
        });
    check(
        "Restauração não substitui dados existentes",
        () -> {
          Path target = transientData.resolve("restaurado"),
              database = target.resolve("data/inpetu-v02.mv.db");
          byte[] before = Files.readAllBytes(database);
          require(
              command(
                      source(ROOT.resolve("scripts/Backup.java"), "restore", archive.toString()),
                      target,
                      password + "\n",
                      90)
                  != 0,
              "Sobrescrita aceita.");
          require(
              Arrays.equals(before, Files.readAllBytes(database)), "Banco existente modificado.");
        });
  }

  static String html(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  static void reports() throws Exception {
    long passed = RESULTS.stream().filter(r -> r.status.equals("PASSOU")).count();
    long failed = RESULTS.stream().filter(r -> r.status.equals("FALHOU")).count();
    long skipped = RESULTS.size() - passed - failed;
    String outcome =
        failed + skipped == 0
            ? "VERIFICAÇÕES AUTOMÁTICAS APROVADAS"
            : "HÁ FALHAS OU TESTES NÃO EXECUTADOS";
    StringBuilder plain =
        new StringBuilder(
            "InPETU Hub - relatório de testes\n"
                + outcome
                + "\nData: "
                + OffsetDateTime.now()
                + "\nSistema: "
                + System.getProperty("os.name")
                + " / "
                + System.getProperty("os.arch")
                + "\nJava: "
                + System.getProperty("java.version")
                + "\n\n");
    StringBuilder body = new StringBuilder();
    for (Result r : RESULTS) {
      plain.append(r.status).append(" | ").append(r.title).append("\n");
      body.append("<tr><td>")
          .append(html(r.title))
          .append("</td><td class='")
          .append(r.status.equals("PASSOU") ? "pass" : "fail")
          .append("'>")
          .append(html(r.status))
          .append("</td><td>")
          .append(html(r.detail))
          .append("</td></tr>");
    }
    String manual =
        "<li>Abra a agenda em navegador de computador e na visualização móvel. Confira filtros,"
            + " textos, campos, botões e mensagens.</li><li>Confira as cinco contas reais e o"
            + " aplicativo autenticador do celular. O executor usa somente contas"
            + " descartáveis.</li><li>Confira os downloads da primeira configuração e a execução em"
            + " cada sistema usado pela equipe.</li><li>Se houver produção, confira certificado"
            + " HTTPS, envio real de email e restauração do MySQL no ambiente de"
            + " destino.</li><li>Confira inatividade real do navegador, mensagens compreensíveis e"
            + " regras de negócio com o responsável pelo projeto.</li>";
    String page =
        "<!doctype html><html lang='pt-BR'><meta charset='utf-8'><meta name='viewport'"
            + " content='width=device-width,initial-scale=1'><title>InPETU -"
            + " testes</title><style>body{font:16px/1.55"
            + " system-ui,sans-serif;color:#18343b;background:#f2f6f7;margin:0}main{max-width:1100px;padding:30px;margin:auto}h1{line-height:1.2}section{background:white;padding:22px;margin:18px"
            + " 0;border-radius:10px}table{width:100%;border-collapse:collapse;font-size:14px}td,th{padding:12px;text-align:left;border-bottom:1px"
            + " solid"
            + " #dde6e8;vertical-align:top}.pass{color:#17603b;font-weight:bold}.fail{color:#a02323;font-weight:bold}.scroll{overflow:auto}code{background:#eef3f3;padding:2px"
            + " 5px}li{margin:10px 0}</style><main><p>INPETU HUB / VERSÃO 0.2</p><h1>"
            + html(outcome)
            + "</h1><p>"
            + passed
            + " aprovações; "
            + failed
            + " falhas; "
            + skipped
            + " não executados.</p><section><h2>O que este resultado significa?</h2><p>Os testes"
            + " abaixo foram executados agora neste computador. Resultados automáticos aprovados"
            + " não significam ausência de bugs. A conferência manual ao fim ainda é"
            + " necessária.</p><p>Banco, contas e backup usados nesta rodada são descartáveis. Os"
            + " cinco acessos privados e os dados da instalação normal não foram usados.</p><p>"
            + html(OffsetDateTime.now().toString())
            + " | "
            + html(
                System.getProperty("os.name")
                    + " / "
                    + System.getProperty("os.arch")
                    + " | Java "
                    + System.getProperty("java.version"))
            + "</p></section><section><h2>Resultados</h2><div"
            + " class='scroll'><table><thead><tr><th>Verificação</th><th>Resultado</th><th>Orientação</th></tr></thead><tbody>"
            + body
            + "</tbody></table></div></section>"
            + "<section><h2>O que a equipe ainda precisa conferir</h2><ul>"
            + manual
            + "</ul><p>Use o PDF privado para os 24 cenários completos. Não anexe o PDF de senhas"
            + " ao relato de um bug.</p></section><section><h2>Se algo falhou</h2><ol><li>Anote o"
            + " nome da verificação e o sistema usado.</li><li>Leia TESTAR.md; confira internet e a"
            + " primeira configuração.</li><li>Repita o comando uma vez se houve falha de"
            + " download.</li><li>Se persistir, informe passos, resultado esperado e observado ao"
            + " responsável.</li></ol><p>Os logs detalhados ficam no diretório privado de trabalho"
            + " indicado no terminal. Revise antes de compartilhar; não envie banco, senhas ou"
            + " chaves.</p></section></main></html>";
    write(report.resolve("ultimo.html"), page);
    write(report.resolve("ultimo.txt"), plain.toString());
    System.out.println("\n" + outcome + "\nAbra no navegador: " + report.resolve("ultimo.html"));
    System.out.println("Logs locais privados: " + logs);
  }

  static void removeWorkData() throws Exception {
    Path temp = transientData;
    require(
        temp != null && temp.startsWith(work) && !temp.equals(work),
        "Diretório temporário inválido.");
    if (!Files.exists(temp)) return;
    try (var stream = Files.walk(temp)) {
      for (Path p : stream.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
    }
    require(!Files.exists(temp), "Os dados temporários não foram removidos completamente.");
  }

  static int run() throws Exception {
    require(
        Files.isRegularFile(ROOT.resolve("pom.xml"))
            && Files.isRegularFile(ROOT.resolve("scripts/Backup.java")),
        "Abra a pasta versão 0.2.");
    directory(ROOT.resolve(".tools"));
    report = directory(ROOT.resolve("relatorios-testes"));
    Path storage = directory(ROOT.resolve(".tools/testes"));
    work = directory(Files.createTempDirectory(storage, "rodada-"));
    logs = directory(work.resolve("logs"));
    transientData = directory(work.resolve("temporarios"));
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  for (Process p : List.copyOf(CHILDREN)) stop(p);
                }));
    System.out.println("InPETU: testes com dados descartáveis. Não feche este terminal.\n");
    try {
      boolean built =
          check(
              "Compilação e testes de regras, fluxos, segurança e concorrência",
              () -> {
                List<String> cmd =
                    WINDOWS
                        ? List.of(
                            "cmd.exe", "/d", "/c", "maven.bat", "-B", "-ntp", "clean", "verify")
                        : List.of("bash", "maven.sh", "-B", "-ntp", "clean", "verify");
                long since = System.currentTimeMillis();
                int exit = command(cmd, ROOT, null, 1200);
                if (Files.isDirectory(ROOT.resolve("target/surefire-reports"))) junitResults(since);
                require(
                    exit == 0,
                    "Maven não concluiu: confira configuração, conexão e os logs locais privados.");
                require(RESULTS.size() > 0, "Nenhum teste de integração foi comprovado.");
              });
      if (built) {
        // Todos os arquivos sensíveis ficam sob temporarios, excluídos ao encerrar.
        if (webTests()) backupTests();
      } else
        RESULTS.add(
            new Result(
                "Servidor real e backup",
                "NÃO EXECUTADO",
                "A compilação precisa passar antes destas etapas."));
    } catch (Exception ex) {
      RESULTS.add(
          new Result(
              "Conclusão do executor",
              "FALHOU",
              "Uma etapa não pôde ser concluída. Consulte os logs privados e TESTAR.md."));
    } finally {
      stop(server);
      if (!check("Limpeza dos bancos e contas descartáveis", Testar::removeWorkData)) {
        System.out.println(
            "Feche processos desta rodada e remova somente a pasta temporarios indicada no guia.");
      }
      reports();
    }
    return RESULTS.stream().allMatch(r -> r.status.equals("PASSOU")) ? 0 : 1;
  }

  public static void main(String[] args) throws Exception {
    Path locks = ROOT.resolve(".tools");
    Files.createDirectories(locks);
    try (var channel =
            FileChannel.open(
                locks.resolve("testar.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        var lock = channel.tryLock()) {
      if (lock == null) {
        System.err.println(
            "Já existe uma rodada de testes. Aguarde a conclusão no outro terminal.");
        System.exit(2);
      }
      System.exit(run());
    }
  }
}
