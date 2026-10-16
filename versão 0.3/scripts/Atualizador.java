import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

/** Atualização local antes do servidor. Não depende de Maven, Python nem bibliotecas externas. */
class Atualizador {
  static final String REPO = "bernardorocha675/Projeto-aplicado-para-o-InpetuHub";
  static final URI API = URI.create("https://api.github.com/repos/" + REPO + "/releases");
  static final String MANIFEST = "scripts/update-files.sha256";
  static final String PREFIX = "versão 0.3/";
  static final Pattern VERSION = Pattern.compile("(?:v)?0\\.3\\.(0|[1-9][0-9]*)(?:fix)?");
  static final Set<String> PRIVATE =
      Set.of(
          ".secrets", ".tools", "data", "logs", "backups", "target", "relatorios-testes", ".git");
  static final int ZIP_LIMIT = 20 * 1024 * 1024;

  record Manifest(String version, Map<String, String> files) {}

  record Release(String tag, String zip, String sums) {}

  static void require(boolean ok, String text) throws IOException {
    if (!ok) throw new IOException(text);
  }

  static void safeName(String name) throws IOException {
    require(
        !name.isEmpty()
            && name.length() < 500
            && !name.contains("\\")
            && !name.contains(":")
            && !name.startsWith("/")
            && !name.endsWith("/")
            && name.chars().noneMatch(c -> c < 32),
        "Caminho inválido no pacote.");
    for (String part : name.split("/", -1)) {
      String lower = part.toLowerCase(Locale.ROOT);
      require(
          !part.isBlank()
              && !part.equals(".")
              && !part.equals("..")
              && !part.endsWith(".")
              && !part.endsWith(" ")
              && !PRIVATE.contains(lower)
              && !lower.contains("acessos-privados")
              && !lower.equals("accounts.properties")
              && !(lower.equals(".env") || lower.equals(".env.local"))
              && !lower.matches(".*\\.(key|pem|p12|jks|db|inpetu|zip)$")
              && !lower.contains(".db.")
              && !lower.matches("(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?"),
          "Arquivo privado ou caminho incompatível no pacote.");
    }
  }

  static Path safe(Path root, String name) throws IOException {
    safeName(name);
    Path result = root.resolve(name).normalize();
    require(result.startsWith(root), "Caminho fora da instalação.");
    for (Path p = result; p != null && p.startsWith(root); p = p.getParent())
      require(!Files.isSymbolicLink(p), "Link simbólico recusado na atualização.");
    return result;
  }

  static Path privateDir(Path p) throws IOException {
    require(!Files.isSymbolicLink(p), "Pasta privada não pode ser um link.");
    Files.createDirectories(p);
    protect(p);
    return p;
  }

  static void protect(Path p) throws IOException {
    if (Files.getFileAttributeView(p, PosixFileAttributeView.class) != null)
      Files.setPosixFilePermissions(
          p, PosixFilePermissions.fromString(Files.isDirectory(p) ? "rwx------" : "rw-------"));
    var acl = Files.getFileAttributeView(p, AclFileAttributeView.class);
    if (acl != null)
      acl.setAcl(
          List.of(
              AclEntry.newBuilder()
                  .setType(AclEntryType.ALLOW)
                  .setPrincipal(Files.getOwner(p))
                  .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                  .build()));
  }

  static String hash(byte[] data) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
  }

  static String fileHash(Path path, String name) throws Exception {
    byte[] data = Files.readAllBytes(path);
    // O checkout Git usa CRLF em .bat no Windows; o conteúdo gerenciado é o mesmo.
    if (name.endsWith(".bat"))
      data =
          new String(data, StandardCharsets.UTF_8)
              .replace("\r\n", "\n")
              .getBytes(StandardCharsets.UTF_8);
    return hash(data);
  }

  static Manifest manifest(byte[] data) throws IOException {
    String[] lines = new String(data, StandardCharsets.UTF_8).split("\\R");
    require(
        lines.length > 1 && lines[0].startsWith("# InPETU Hub "), "Manifesto ausente ou inválido.");
    String version = lines[0].substring(13);
    require(VERSION.matcher(version).matches(), "O pacote não pertence à linha 0.3.x.");
    Map<String, String> files = new TreeMap<>();
    Set<String> portable = new HashSet<>();
    for (int i = 1; i < lines.length; i++) {
      String line = lines[i];
      require(line.matches("[0-9a-f]{64}  .+"), "Manifesto inválido.");
      String name = line.substring(66);
      safeName(name);
      require(
          !name.equals(MANIFEST)
              && files.put(name, line.substring(0, 64)) == null
              && portable.add(name.toLowerCase(Locale.ROOT)),
          "Arquivo duplicado no manifesto.");
    }
    for (String name : files.keySet()) {
      Path parent = Path.of(name).getParent();
      while (parent != null) {
        require(
            !portable.contains(parent.toString().replace('\\', '/').toLowerCase(Locale.ROOT)),
            "Arquivo e diretório com o mesmo caminho.");
        parent = parent.getParent();
      }
    }
    require(
        files
            .keySet()
            .containsAll(
                Set.of(
                    "pom.xml",
                    "iniciar.sh",
                    "iniciar.ps1",
                    "iniciar.bat",
                    "scripts/Atualizador.java")),
        "Pacote incompleto.");
    return new Manifest(version, files);
  }

  static long patch(String version) {
    Matcher m = VERSION.matcher(version);
    return m.matches() ? Long.parseLong(m.group(1)) : -1;
  }

  static String pomVersion(Path pom) throws Exception {
    var factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    var nodes =
        factory.newDocumentBuilder().parse(pom.toFile()).getDocumentElement().getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++)
      if (nodes.item(i).getNodeName().equals("version"))
        return nodes.item(i).getTextContent().trim();
    throw new IOException("Versão ausente no pom.xml.");
  }

  static void cleanLocal(Path root, Manifest old, Manifest next) throws Exception {
    for (var file : old.files.entrySet()) {
      Path p = safe(root, file.getKey());
      require(
          Files.isRegularFile(p) && file.getValue().equals(fileHash(p, file.getKey())),
          "Código local alterado: atualização automática cancelada; consulte ATUALIZACOES.md.");
    }
    for (String name : next.files.keySet()) {
      Path p = safe(root, name);
      require(
          old.files.containsKey(name) || !Files.exists(p, LinkOption.NOFOLLOW_LINKS),
          "Um arquivo local ocupa um caminho novo. Atualização cancelada.");
    }
  }

  static boolean testApi(URI api) {
    return api.getScheme().equals("http") && api.getHost().equals("127.0.0.1");
  }

  static void networkUri(URI uri, URI api, boolean initialAsset) throws IOException {
    if (testApi(api)) {
      require(
          uri.getScheme().equals("http")
              && uri.getHost().equals("127.0.0.1")
              && uri.getPort() == api.getPort(),
          "Destino de teste inválido.");
      return;
    }
    require(
        uri.getScheme().equals("https")
            && uri.getUserInfo() == null
            && (uri.getPort() == -1 || uri.getPort() == 443),
        "Download sem HTTPS recusado.");
    String host = uri.getHost();
    require(
        Set.of(
                "api.github.com",
                "github.com",
                "release-assets.githubusercontent.com",
                "objects.githubusercontent.com")
            .contains(host),
        "Destino de download recusado.");
    if (initialAsset)
      require(
          host.equals("github.com") && uri.getPath().startsWith("/" + REPO + "/releases/download/"),
          "O arquivo não pertence às releases do projeto.");
  }

  static byte[] fetch(URI uri, int limit, URI api, boolean asset) throws IOException {
    networkUri(uri, api, asset);
    for (int redirects = 0; redirects < 6; redirects++) {
      networkUri(uri, api, false);
      Proxy proxy = Proxy.NO_PROXY;
      String setting = System.getenv("HTTPS_PROXY");
      if (!testApi(api) && setting != null && !setting.isBlank()) {
        URI value = URI.create(setting);
        proxy =
            new Proxy(
                Proxy.Type.HTTP,
                new InetSocketAddress(
                    value.getHost(), value.getPort() == -1 ? 80 : value.getPort()));
      }
      HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection(proxy);
      connection.setConnectTimeout(6000);
      connection.setReadTimeout(10000);
      connection.setInstanceFollowRedirects(false);
      connection.setRequestProperty("User-Agent", "InPETU-Hub-Atualizador-0.3");
      try {
        int status = connection.getResponseCode();
        if (Set.of(301, 302, 303, 307, 308).contains(status)) {
          String location = connection.getHeaderField("Location");
          require(location != null, "Redirecionamento inválido.");
          uri = uri.resolve(location);
          continue;
        }
        require(status == 200, "Consulta ou download indisponível.");
        require(connection.getContentLengthLong() <= limit, "Resposta maior que o limite.");
        try (var input = connection.getInputStream()) {
          byte[] bytes = input.readNBytes(limit + 1);
          require(bytes.length <= limit, "Resposta maior que o limite.");
          return bytes;
        }
      } finally {
        connection.disconnect();
      }
    }
    throw new IOException("Excesso de redirecionamentos.");
  }

  static Release latest(URI api) throws Exception {
    Release best = null;
    for (int page = 1; page <= 10; page++) {
      Object value =
          new Json(
                  new String(
                      fetch(
                          URI.create(api + "?per_page=100&page=" + page),
                          4 * 1024 * 1024,
                          api,
                          false),
                      StandardCharsets.UTF_8))
              .parse();
      require(value instanceof List<?>, "Lista de releases inválida.");
      List<?> releases = (List<?>) value;
      for (Object item : releases) {
        if (!(item instanceof Map<?, ?> r)
            || !Boolean.FALSE.equals(r.get("draft"))
            || !Boolean.FALSE.equals(r.get("prerelease"))
            || !(r.get("tag_name") instanceof String tag)
            || !VERSION.matcher(tag).matches()) continue;
        String zip = null, sums = null;
        if (r.get("assets") instanceof List<?> assets)
          for (Object entry : assets) {
            if (!(entry instanceof Map<?, ?> a) || !"uploaded".equals(a.get("state"))) continue;
            if (("inpetu-hub-" + tag + ".zip").equals(a.get("name")))
              zip = (String) a.get("browser_download_url");
            if ("SHA256SUMS.txt".equals(a.get("name")))
              sums = (String) a.get("browser_download_url");
          }
        if (best == null || patch(tag) > patch(best.tag)) best = new Release(tag, zip, sums);
      }
      if (releases.size() < 100) return best;
    }
    throw new IOException("Não foi possível conferir todas as releases.");
  }

  static Path unpack(byte[] archive, Path stage) throws Exception {
    Path code = privateDir(stage.resolve("code"));
    Set<String> names = new HashSet<>();
    long total = 0;
    try (var zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
      ZipEntry entry;
      int count = 0;
      while ((entry = zip.getNextEntry()) != null) {
        require(++count <= 5000, "Pacote com arquivos em excesso.");
        String name = entry.getName();
        String portable = entry.isDirectory() ? name.replaceAll("/$", "") : name;
        // Validar até entradas fora da pasta selecionada; nunca extraí-las.
        safeName(portable);
        require(names.add(portable.toLowerCase(Locale.ROOT)), "Entrada ZIP duplicada.");
        if (entry.isDirectory()) continue;
        byte[] data = zip.readNBytes(8 * 1024 * 1024 + 1);
        total += data.length;
        require(
            data.length <= 8 * 1024 * 1024 && total <= 80 * 1024 * 1024,
            "Pacote descompactado maior que o limite.");
        if (!name.startsWith(PREFIX)) continue;
        String relative = name.substring(PREFIX.length());
        Path destination = safe(code, relative);
        Files.createDirectories(destination.getParent());
        Files.write(destination, data, StandardOpenOption.CREATE_NEW);
      }
    }
    Manifest next = manifest(Files.readAllBytes(code.resolve(MANIFEST)));
    Set<String> expected = new HashSet<>(next.files.keySet());
    expected.add(MANIFEST);
    try (var walk = Files.walk(code)) {
      Set<String> actual = new HashSet<>();
      for (Path p : walk.filter(Files::isRegularFile).toList())
        actual.add(code.relativize(p).toString().replace('\\', '/'));
      require(expected.equals(actual), "Arquivos extras ou ausentes no pacote.");
    }
    for (var f : next.files.entrySet())
      require(
          f.getValue().equals(fileHash(code.resolve(f.getKey()), f.getKey())),
          "Arquivo do pacote não confere com o manifesto.");
    require(
        next.version.equals(pomVersion(code.resolve("pom.xml"))), "Versões do pacote divergem.");
    return code;
  }

  static void copyPrivate(Path source, Path destination) throws IOException {
    require(!Files.isSymbolicLink(source), "Link recusado no backup privado.");
    if (Files.isDirectory(source)) {
      privateDir(destination);
      try (var children = Files.list(source)) {
        for (Path child : children.toList()) {
          if (child.getFileName().toString().equals("runtime.lock")) continue;
          copyPrivate(child, destination.resolve(child.getFileName()));
        }
      }
    } else {
      require(Files.isRegularFile(source), "Arquivo especial recusado no backup.");
      privateDir(destination.getParent());
      Files.createFile(destination);
      protect(destination);
      Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
      protect(destination);
    }
  }

  static void replace(Path source, Path destination) throws IOException {
    Files.createDirectories(destination.getParent());
    Path tmp = Files.createTempFile(destination.getParent(), ".update-", ".tmp");
    try {
      Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING);
      try {
        Files.move(
            tmp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException ex) {
        Files.move(tmp, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(tmp);
    }
  }

  static void removeTree(Path root) throws IOException {
    if (!Files.exists(root)) return;
    try (var walk = Files.walk(root)) {
      for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
    }
  }

  static boolean recover(Path root) throws Exception {
    Path journal = root.resolve(".tools/update-pending");
    if (!Files.exists(journal)) return false;
    require(!Files.isSymbolicLink(journal), "Registro de atualização inválido.");
    if (!Files.exists(journal.resolve("READY"))) {
      removeTree(journal);
      return false;
    }
    String id = Files.readString(journal.resolve("READY")).trim();
    require(id.matches("[0-9a-f-]{36}"), "Referência de backup inválida.");
    Path backup = root.resolve("backups/atualizacoes/" + id);
    require(
        !Files.isSymbolicLink(root.resolve("backups"))
            && !Files.isSymbolicLink(root.resolve("backups/atualizacoes"))
            && !Files.isSymbolicLink(backup),
        "Link recusado na recuperação.");
    Manifest old = manifest(Files.readAllBytes(backup.resolve("codigo/" + MANIFEST)));
    Manifest next = manifest(Files.readAllBytes(journal.resolve("next-manifest")));
    for (var file : old.files.entrySet())
      require(
          file.getValue()
              .equals(fileHash(safe(backup.resolve("codigo"), file.getKey()), file.getKey())),
          "Backup de código inválido; recuperação manual necessária.");
    for (String name : next.files.keySet())
      if (!old.files.containsKey(name)) Files.deleteIfExists(safe(root, name));
    for (String name : old.files.keySet())
      replace(safe(backup.resolve("codigo"), name), safe(root, name));
    replace(backup.resolve("codigo/" + MANIFEST), safe(root, MANIFEST));
    removeTree(journal);
    System.out.println("Atualização interrompida recuperada; o código anterior foi restaurado.");
    return true;
  }

  static void apply(Path root, Path code, Manifest old, Manifest next) throws Exception {
    cleanLocal(root, old, next);
    Path backup =
        privateDir(
            privateDir(privateDir(root.resolve("backups")).resolve("atualizacoes"))
                .resolve(UUID.randomUUID().toString()));
    Path oldCode = privateDir(backup.resolve("codigo"));
    for (String name : old.files.keySet()) copyPrivate(safe(root, name), oldCode.resolve(name));
    copyPrivate(safe(root, MANIFEST), oldCode.resolve(MANIFEST));
    for (String name : List.of("data", ".secrets", ".env.local", ".env")) {
      Path source = root.resolve(name);
      if (Files.exists(source, LinkOption.NOFOLLOW_LINKS))
        copyPrivate(source, backup.resolve("privado/" + name));
    }
    // O registro READY só é criado depois que toda a cópia anterior está completa.
    Path journal = privateDir(root.resolve(".tools/update-pending"));
    Files.copy(code.resolve(MANIFEST), journal.resolve("next-manifest"));
    Files.writeString(journal.resolve("READY"), backup.getFileName().toString());
    try {
      for (String name : old.files.keySet())
        if (!next.files.containsKey(name)) Files.delete(safe(root, name));
      for (String name : next.files.keySet()) replace(safe(code, name), safe(root, name));
      replace(code.resolve(MANIFEST), safe(root, MANIFEST));
      // Um marcador removido por último confirma a troca. Recuperação nunca altera o banco.
      Files.delete(journal.resolve("READY"));
    } catch (Exception ex) {
      recover(root);
      throw ex;
    }
    removeTree(journal);
    System.out.println(
        "Código atualizado para "
            + next.version
            + ". Backup privado: backups/atualizacoes/"
            + backup.getFileName());
  }

  /** URI substituível apenas pela chamada de teste; o comando público usa sempre API. */
  static int run(Path root, URI api, boolean enabled) throws Exception {
    root = root.toAbsolutePath().normalize();
    require(!Files.isSymbolicLink(root), "Instalação não pode ser link simbólico.");
    privateDir(root.resolve(".tools"));
    privateDir(root.resolve(".secrets"));
    Path updateLock = root.resolve(".tools/update.lock"),
        runtimeLock = root.resolve(".secrets/runtime.lock");
    require(
        !Files.isSymbolicLink(updateLock) && !Files.isSymbolicLink(runtimeLock), "Lock inválido.");
    try (var updates =
            FileChannel.open(updateLock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        var update = updates.tryLock();
        var runtime =
            FileChannel.open(runtimeLock, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        var lock = runtime.tryLock()) {
      require(
          update != null && lock != null, "Pare o servidor e aguarde outra atualização terminar.");
      if (recover(root)) return 10;
      if (!enabled) return 0;
      Manifest old = manifest(Files.readAllBytes(safe(root, MANIFEST)));
      require(
          old.version.equals(pomVersion(root.resolve("pom.xml"))),
          "Versão local e manifesto divergem.");
      Release next;
      try {
        next = latest(api);
      } catch (Exception ex) {
        System.out.println("Não foi possível consultar o GitHub. Usando a versão instalada.");
        return 0;
      }
      if (next == null || patch(next.tag) <= patch(old.version)) return 0;
      require(
          next.zip != null && next.sums != null,
          "A release nova está incompleta. Tente novamente depois.");
      Path stage = Files.createTempDirectory(root.resolve(".tools"), "update-stage-");
      protect(stage);
      try {
        System.out.println("Verificando correção " + next.tag + " da linha 0.3.x...");
        byte[] archive, sums;
        try {
          archive = fetch(URI.create(next.zip), ZIP_LIMIT, api, true);
          sums = fetch(URI.create(next.sums), 16384, api, true);
        } catch (IOException ex) {
          System.out.println("Download indisponível. Usando a versão instalada.");
          return 0;
        }
        String expectedName = "inpetu-hub-" + next.tag + ".zip";
        String expected = null;
        for (String line : new String(sums, StandardCharsets.UTF_8).split("\\R"))
          if (line.matches("[0-9a-f]{64}  " + Pattern.quote(expectedName))) {
            require(expected == null, "Checksum duplicado.");
            expected = line.substring(0, 64);
          }
        require(
            hash(archive).equals(expected), "Integridade inválida. Nenhum arquivo foi atualizado.");
        Path code = unpack(archive, stage);
        Manifest nextManifest = manifest(Files.readAllBytes(code.resolve(MANIFEST)));
        require(
            ("v" + nextManifest.version).equals(next.tag), "Versão do ZIP diferente da release.");
        apply(root, code, old, nextManifest);
        return 10;
      } finally {
        removeTree(stage);
      }
    }
  }

  public static void main(String[] args) {
    try {
      require(args.length == 0, "Execute pelo comando de iniciar.");
      System.exit(run(Path.of(""), API, !"0".equals(System.getenv("INPETU_AUTO_UPDATE"))));
    } catch (Exception ex) {
      // Nunca imprimir respostas remotas ou caminhos/valores privados arbitrários.
      System.err.println(
          "Atualização não concluída. Consulte ATUALIZACOES.md; os dados privados foram"
              + " preservados.");
      System.exit(2);
    }
  }

  /** Leitor JSON limitado à consulta pública; rejeita duplicatas e profundidade excessiva. */
  static class Json {
    final String text;
    int position, depth;

    Json(String text) {
      this.text = text;
    }

    IOException bad() {
      return new IOException("Resposta JSON inválida.");
    }

    void space() {
      while (position < text.length() && " \t\r\n".indexOf(text.charAt(position)) >= 0) position++;
    }

    char take() throws IOException {
      if (position == text.length()) throw bad();
      return text.charAt(position++);
    }

    boolean consume(char c) {
      space();
      if (position < text.length() && text.charAt(position) == c) {
        position++;
        return true;
      }
      return false;
    }

    Object parse() throws IOException {
      Object v = value();
      space();
      if (position != text.length()) throw bad();
      return v;
    }

    Object value() throws IOException {
      space();
      if (++depth > 64) throw bad();
      try {
        char c = take();
        if (c == '"') return string();
        if (c == '[') {
          List<Object> items = new ArrayList<>();
          if (consume(']')) return items;
          do {
            items.add(value());
          } while (consume(','));
          if (!consume(']')) throw bad();
          return items;
        }
        if (c == '{') {
          Map<String, Object> items = new HashMap<>();
          if (consume('}')) return items;
          do {
            space();
            if (take() != '"') throw bad();
            String key = string();
            if (!consume(':') || items.containsKey(key)) throw bad();
            items.put(key, value());
          } while (consume(','));
          if (!consume('}')) throw bad();
          return items;
        }
        position--;
        for (String literal : List.of("true", "false", "null"))
          if (text.startsWith(literal, position)) {
            position += literal.length();
            return literal.equals("null") ? null : literal.equals("true");
          }
        Matcher number =
            Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?").matcher(text);
        number.region(position, text.length());
        if (!number.lookingAt()) throw bad();
        position = number.end();
        return number.group();
      } finally {
        depth--;
      }
    }

    String string() throws IOException {
      StringBuilder s = new StringBuilder();
      while (true) {
        char c = take();
        if (c == '"') return s.toString();
        if (c < 32) throw bad();
        if (c == '\\') {
          char e = take();
          c =
              switch (e) {
                case '"', '\\', '/' -> e;
                case 'b' -> '\b';
                case 'f' -> '\f';
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case 'u' -> {
                  int code = 0;
                  for (int i = 0; i < 4; i++) {
                    int digit = Character.digit(take(), 16);
                    if (digit < 0) throw bad();
                    code = code * 16 + digit;
                  }
                  yield (char) code;
                }
                default -> throw bad();
              };
        }
        s.append(c);
      }
    }
  }
}
