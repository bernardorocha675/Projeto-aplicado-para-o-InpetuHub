package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/** Usa releases fictícias em loopback; nunca consulta GitHub nem usa contas reais. */
class AutomaticUpdateTest {
  static final String MANIFEST = "scripts/update-files.sha256";
  @TempDir static Path classes;
  @TempDir Path temp;
  static URLClassLoader loader;
  static Class<?> updater;
  HttpServer server;
  URI api;
  final Map<String, byte[]> responses = new HashMap<>();
  final Map<String, Integer> calls = new HashMap<>();

  @BeforeAll
  static void compileUpdater() throws Exception {
    assertEquals(
        0,
        ToolProvider.getSystemJavaCompiler()
            .run(
                null,
                null,
                null,
                "-encoding",
                "UTF-8",
                "--release",
                "17",
                "-d",
                classes.toString(),
                "scripts/Atualizador.java"));
    loader =
        new URLClassLoader(
            new URL[] {classes.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    updater = loader.loadClass("Atualizador");
  }

  @AfterAll
  static void closeLoader() throws Exception {
    loader.close();
  }

  @BeforeEach
  void startServer() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().toString();
          calls.merge(path, 1, Integer::sum);
          byte[] body = responses.get(path);
          exchange.sendResponseHeaders(body == null ? 503 : 200, body == null ? -1 : body.length);
          if (body != null) exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    api = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/releases");
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
    Method method = updater.getDeclaredMethod(name, types);
    method.setAccessible(true);
    try {
      return method.invoke(null, args);
    } catch (InvocationTargetException ex) {
      if (ex.getCause() instanceof Exception e) throw e;
      throw ex;
    }
  }

  int run(Path root, boolean enabled) throws Exception {
    return (int)
        invoke("run", new Class<?>[] {Path.class, URI.class, boolean.class}, root, api, enabled);
  }

  static String hash(byte[] value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
  }

  static byte[] bytes(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  Map<String, byte[]> files(String version) {
    Map<String, byte[]> files = new TreeMap<>();
    files.put("pom.xml", bytes("<project><version>" + version + "</version></project>"));
    files.put("iniciar.sh", bytes("#!/bin/bash\nexit 0\n"));
    files.put("iniciar.ps1", bytes("exit 0\n"));
    files.put("iniciar.bat", bytes("@echo off\nexit /b 0\n"));
    files.put("scripts/Atualizador.java", bytes("// fixture " + version));
    files.put("src/main/resources/mensagem.txt", bytes(version));
    return files;
  }

  byte[] manifest(String version, Map<String, byte[]> files) throws Exception {
    StringBuilder content = new StringBuilder("# InPETU Hub " + version + "\n");
    for (var entry : files.entrySet())
      if (!entry.getKey().equals(MANIFEST))
        content
            .append(
                hash(
                    entry.getKey().endsWith(".bat")
                        ? bytes(
                            new String(entry.getValue(), StandardCharsets.UTF_8)
                                .replace("\r\n", "\n"))
                        : entry.getValue()))
            .append("  ")
            .append(entry.getKey())
            .append('\n');
    return bytes(content.toString());
  }

  static void write(Path root, String name, byte[] content) throws IOException {
    Path file = root.resolve(name);
    Files.createDirectories(file.getParent());
    Files.write(file, content);
  }

  Path install(String version, Map<String, byte[]> files) throws Exception {
    Path root = Files.createDirectory(temp.resolve("instalação com espaços"));
    for (var entry : files.entrySet()) write(root, entry.getKey(), entry.getValue());
    write(root, MANIFEST, manifest(version, files));
    return root;
  }

  byte[] zip(Map<String, byte[]> entries) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(stream, StandardCharsets.UTF_8)) {
      for (var entry : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(entry.getKey()));
        zip.write(entry.getValue());
        zip.closeEntry();
      }
    }
    return stream.toByteArray();
  }

  byte[] packageZip(String version, Map<String, byte[]> files, Map<String, byte[]> extra)
      throws Exception {
    Map<String, byte[]> entries = new TreeMap<>();
    for (var entry : files.entrySet())
      entries.put("versão 0.3/" + entry.getKey(), entry.getValue());
    entries.put("versão 0.3/" + MANIFEST, manifest(version, files));
    entries.put("README.md", bytes("documentação pública"));
    entries.putAll(extra);
    return zip(entries);
  }

  String release(String tag, boolean draft, boolean prerelease) {
    String base = "http://127.0.0.1:" + server.getAddress().getPort();
    return "{\"tag_name\":\""
        + tag
        + "\",\"draft\":"
        + draft
        + ",\"prerelease\":"
        + prerelease
        + ",\"assets\":[{\"name\":\"inpetu-hub-"
        + tag
        + ".zip\",\"state\":\"uploaded\",\"browser_download_url\":\""
        + base
        + "/"
        + tag
        + ".zip\"},{\"name\":\"SHA256SUMS.txt\",\"state\":\"uploaded\",\"browser_download_url\":\""
        + base
        + "/"
        + tag
        + ".sha\"}],\"body\":\"Acentos: \\u00e3 / aspas: \\\" / linha: \\n\",\"id\":123} ";
  }

  void offer(String version, byte[] archive) throws Exception {
    String tag = "v" + version;
    responses.put("/releases?per_page=100&page=1", bytes("[" + release(tag, false, false) + "]"));
    responses.put("/" + tag + ".zip", archive);
    responses.put("/" + tag + ".sha", bytes(hash(archive) + "  inpetu-hub-" + tag + ".zip\n"));
  }

  @Test
  void updatesOnlyHighestPatchPreservingPrivateDataAndRemovingObsoleteCode() throws Exception {
    var old = files("0.3.3");
    old.put("src/obsolete.txt", bytes("antigo"));
    Path root = install("0.3.3", old);
    Map<String, byte[]> privateFiles =
        Map.of(
            "data/inpetu-v02.mv.db",
            bytes("banco de teste"),
            ".secrets/mfa.key",
            bytes("chave de teste"),
            ".secrets/accounts.properties",
            bytes("contas de teste"),
            ".env.local",
            bytes("ambiente de teste"),
            "logs/inpetu.log",
            bytes("log de teste"),
            "ACESSOS-PRIVADOS/contas.properties",
            bytes("pacote privado"),
            "meu-arquivo.txt",
            bytes("arquivo do usuário"));
    for (var entry : privateFiles.entrySet()) write(root, entry.getKey(), entry.getValue());
    var next = files("0.3.10");
    next.put("src/novo.txt", bytes("novo"));
    offer("0.3.10", packageZip("0.3.10", next, Map.of()));
    responses.put(
        "/releases?per_page=100&page=1",
        bytes(
            "["
                + release("v0.4.0", false, false)
                + ","
                + release("v0.3.99", false, true)
                + ","
                + release("v0.3.100", true, false)
                + ","
                + release("v0.3.9", false, false)
                + ","
                + release("v0.3.10", false, false)
                + ","
                + release("v0.3.1fix", false, false)
                + "]"));
    assertEquals(10, run(root, true));
    assertEquals("0.3.10", Files.readString(root.resolve("src/main/resources/mensagem.txt")));
    assertFalse(Files.exists(root.resolve("src/obsolete.txt")));
    assertEquals("novo", Files.readString(root.resolve("src/novo.txt")));
    for (var entry : privateFiles.entrySet())
      assertArrayEquals(entry.getValue(), Files.readAllBytes(root.resolve(entry.getKey())));
    assertFalse(Files.exists(root.resolve("README.md"))); // não altera documentação fora da versão
    Path backup;
    try (var dirs = Files.list(root.resolve("backups/atualizacoes"))) {
      backup = dirs.findFirst().orElseThrow();
    }
    assertEquals(
        "banco de teste", Files.readString(backup.resolve("privado/data/inpetu-v02.mv.db")));
    assertEquals(
        "contas de teste",
        Files.readString(backup.resolve("privado/.secrets/accounts.properties")));
    assertTrue(Files.exists(backup.resolve("codigo/src/obsolete.txt")));
    if (Files.getFileStore(backup).supportsFileAttributeView("posix")) {
      assertEquals(
          PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(backup));
      assertEquals(
          PosixFilePermissions.fromString("rw-------"),
          Files.getPosixFilePermissions(backup.resolve("privado/data/inpetu-v02.mv.db")));
    }
    assertEquals(0, run(root, true));
  }

  @Test
  void paginatesReleaseListInsteadOfUsingGlobalLatest() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    offer("0.3.10", packageZip("0.3.10", files("0.3.10"), Map.of()));
    String page = String.join(",", Collections.nCopies(99, release("v0.4.0", false, false)));
    responses.put(
        "/releases?per_page=100&page=1",
        bytes("[" + page + "," + release("v0.3.4", false, false) + "]"));
    responses.put(
        "/releases?per_page=100&page=2", bytes("[" + release("v0.3.10", false, false) + "]"));
    assertEquals(10, run(root, true));
    assertEquals(1, calls.get("/releases?per_page=100&page=2"));
  }

  @Test
  void offlineApiAndDownloadUseInstalledVersion() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    assertEquals(0, run(root, true));
    offer("0.3.4", packageZip("0.3.4", files("0.3.4"), Map.of()));
    responses.remove("/v0.3.4.zip");
    assertEquals(0, run(root, true));
    assertEquals("0.3.3", Files.readString(root.resolve("src/main/resources/mensagem.txt")));
    assertFalse(Files.exists(root.resolve("backups")));
  }

  @Test
  void checksumFailureStopsWithoutChangingInstallation() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    offer("0.3.4", packageZip("0.3.4", files("0.3.4"), Map.of()));
    responses.put("/v0.3.4.zip", bytes("corrompido"));
    assertThrows(IOException.class, () -> run(root, true));
    assertEquals("0.3.3", Files.readString(root.resolve("src/main/resources/mensagem.txt")));
    assertFalse(Files.exists(root.resolve("backups")));
  }

  @Test
  void localChangesAreNotOverwritten() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    write(root, "src/main/resources/mensagem.txt", bytes("edição local"));
    offer("0.3.4", packageZip("0.3.4", files("0.3.4"), Map.of()));
    assertThrows(IOException.class, () -> run(root, true));
    assertEquals("edição local", Files.readString(root.resolve("src/main/resources/mensagem.txt")));
    assertFalse(Files.exists(root.resolve("backups")));
  }

  @Test
  void unmanagedNewPathCollisionIsNotOverwritten() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    write(root, "src/novo.txt", bytes("meu arquivo"));
    var next = files("0.3.4");
    next.put("src/novo.txt", bytes("release"));
    offer("0.3.4", packageZip("0.3.4", next, Map.of()));
    assertThrows(IOException.class, () -> run(root, true));
    assertEquals("meu arquivo", Files.readString(root.resolve("src/novo.txt")));
  }

  @Test
  void zipTraversalPrivatePathsAndUnexpectedFilesAreRejected() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    for (String name :
        List.of(
            "../escaped.txt",
            "versão 0.3/../../escaped.txt",
            "versão 0.3/.secrets/accounts.properties",
            "versão 0.3/.env.local",
            "versão 0.3/data/inpetu-v02.mv.db",
            "versão 0.3/scripts/extra.txt",
            "versão 0.3/src/NUL.txt",
            "versão 0.3/src/arquivo.",
            "versão 0.3/src\\outro.txt")) {
      offer("0.3.4", packageZip("0.3.4", files("0.3.4"), Map.of(name, bytes("inválido"))));
      assertThrows(Exception.class, () -> run(root, true), name);
    }
    assertEquals("0.3.3", Files.readString(root.resolve("src/main/resources/mensagem.txt")));
    assertFalse(Files.exists(temp.resolve("escaped.txt")));
  }

  @Test
  void tamperedManifestOrVersionMismatchIsRejected() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    Map<String, byte[]> extra =
        Map.of("versão 0.3/src/main/resources/mensagem.txt", bytes("adulterado"));
    offer("0.3.4", packageZip("0.3.4", files("0.3.4"), extra));
    assertThrows(IOException.class, () -> run(root, true));
    offer("0.3.4", packageZip("0.3.5", files("0.3.5"), Map.of()));
    assertThrows(IOException.class, () -> run(root, true));
    assertEquals("0.3.3", Files.readString(root.resolve("src/main/resources/mensagem.txt")));
  }

  @Test
  void runningServerPreventsUpdateEvenWhenDisabled() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    Files.createDirectories(root.resolve(".secrets"));
    try (var channel =
            FileChannel.open(
                root.resolve(".secrets/runtime.lock"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        var lock = channel.lock()) {
      assertThrows(Exception.class, () -> run(root, false));
    }
    assertTrue(calls.isEmpty());
  }

  @Test
  void disabledUpdateDoesNotConsultNetworkAndNoDowngradeOccurs() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    assertEquals(0, run(root, false));
    assertTrue(calls.isEmpty());
    offer("0.3.1fix", packageZip("0.3.1fix", files("0.3.1fix"), Map.of()));
    assertEquals(0, run(root, true));
    assertFalse(calls.containsKey("/v0.3.1fix.zip"));
  }

  @Test
  void recoversInterruptedCodeReplacementWithoutTouchingCurrentData() throws Exception {
    var old = files("0.3.3");
    old.put("src/old.txt", bytes("anterior"));
    Path root = install("0.3.3", old);
    String id = UUID.randomUUID().toString();
    Path backup = root.resolve("backups/atualizacoes/" + id + "/codigo");
    for (var entry : old.entrySet()) write(backup, entry.getKey(), entry.getValue());
    write(backup, MANIFEST, manifest("0.3.3", old));
    var next = files("0.3.4");
    next.put("src/new.txt", bytes("novo"));
    write(root, "src/new.txt", bytes("novo"));
    write(root, "pom.xml", next.get("pom.xml"));
    Files.delete(root.resolve("src/old.txt"));
    write(root, "data/inpetu-v02.mv.db", bytes("dados atuais"));
    write(root, ".tools/update-pending/next-manifest", manifest("0.3.4", next));
    write(root, ".tools/update-pending/READY", bytes(id));
    assertEquals(10, run(root, false));
    assertEquals("anterior", Files.readString(root.resolve("src/old.txt")));
    assertArrayEquals(old.get("pom.xml"), Files.readAllBytes(root.resolve("pom.xml")));
    assertFalse(Files.exists(root.resolve("src/new.txt")));
    assertEquals("dados atuais", Files.readString(root.resolve("data/inpetu-v02.mv.db")));
    assertFalse(Files.exists(root.resolve(".tools/update-pending")));
  }

  @Test
  void windowsBatchLineEndingsAreAccepted() throws Exception {
    Path root = install("0.3.3", files("0.3.3"));
    write(root, "iniciar.bat", bytes("@echo off\r\nexit /b 0\r\n"));
    offer("0.3.4", packageZip("0.3.4", files("0.3.4"), Map.of()));
    assertEquals(10, run(root, true));
  }

  @Test
  void nativeStartCommandReloadsUpdatedLauncherAndPreservesArguments() throws Exception {
    boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    var old = files("0.3.3");
    for (String name :
        List.of(
            "iniciar.sh",
            "iniciar.ps1",
            "iniciar.bat",
            "scripts/runtime.sh",
            "scripts/runtime.ps1")) old.put(name, Files.readAllBytes(Path.of(name)));
    String source = Files.readString(Path.of("scripts/Atualizador.java"));
    String productionApi = "\"https://api.github.com/repos/\" + REPO + \"/releases\"";
    assertTrue(source.contains(productionApi));
    old.put("scripts/Atualizador.java", bytes(source.replace(productionApi, "\"" + api + "\"")));
    Path root = install("0.3.3", old);
    write(root, ".tools/ready", bytes("ready"));
    write(root, ".env.local", bytes("fixture"));
    write(
        root,
        ".tools/apache-maven-3.9.9/bin/" + (windows ? "mvn.cmd" : "mvn"),
        bytes("not executed"));
    Path maven = root.resolve(".tools/apache-maven-3.9.9/bin/mvn");
    if (!windows) maven.toFile().setExecutable(true);
    var next = files("0.3.4");
    next.put(
        "iniciar.sh", bytes("#!/usr/bin/env bash\nprintf '%s' \"$1\" > argumento.txt\nexit 42\n"));
    next.put(
        "iniciar.ps1",
        bytes(
            "[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'argumento.txt'), $args[0])\n"
                + "exit 42\n"));
    next.put("iniciar.bat", old.get("iniciar.bat"));
    offer("0.3.4", packageZip("0.3.4", next, Map.of()));
    var command =
        new ProcessBuilder(
            windows
                ? List.of("cmd.exe", "/d", "/c", "iniciar.bat", "argumento-teste")
                : List.of("bash", "iniciar.sh", "argumento-teste"));
    command.directory(root.toFile());
    command.environment().remove("INPETU_AUTO_UPDATE");
    command.environment().put("JAVA_HOME", System.getProperty("java.home"));
    if (!windows && !Files.isExecutable(Path.of(System.getProperty("java.home"), "bin/javac"))) {
      Path bin = root.resolve(".tools/jdk/bin");
      Files.createDirectories(bin);
      Files.createSymbolicLink(
          bin.resolve("java"), Path.of(System.getProperty("java.home"), "bin/java"));
      write(
          root,
          ".tools/jdk/bin/javac",
          bytes(
              "#!/bin/sh\nexec '"
                  + Path.of(System.getProperty("java.home"), "bin/java")
                  + "' -m jdk.compiler/com.sun.tools.javac.Main \"$@\"\n"));
      bin.resolve("javac").toFile().setExecutable(true);
    }
    Path output = temp.resolve("native-start.log");
    Process process = command.redirectErrorStream(true).redirectOutput(output.toFile()).start();
    try {
      assertTrue(
          process.waitFor(90, java.util.concurrent.TimeUnit.SECONDS),
          "Comando de iniciar não terminou.");
      assertEquals(
          42,
          process.exitValue(),
          () -> {
            try {
              return Files.readString(output);
            } catch (IOException e) {
              return "Log indisponível.";
            }
          });
      assertEquals("argumento-teste", Files.readString(root.resolve("argumento.txt")));
      assertEquals(1, calls.get("/releases?per_page=100&page=1"));
    } finally {
      process.destroyForcibly();
    }
  }

  @Test
  void productionDownloadDestinationsRequireProjectHttpsAssets() throws Exception {
    URI real = URI.create("https://api.github.com/repos/test/releases");
    for (String value :
        List.of(
            "http://github.com/file",
            "https://example.com/file",
            "https://github.com/other/repo/releases/download/v0.3.4/file"))
      assertThrows(
          IOException.class,
          () ->
              invoke(
                  "networkUri",
                  new Class<?>[] {URI.class, URI.class, boolean.class},
                  URI.create(value),
                  real,
                  true));
  }
}
