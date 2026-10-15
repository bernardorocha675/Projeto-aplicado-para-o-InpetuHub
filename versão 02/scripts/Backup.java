import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Backup H2 criptografado. Senha lida do terminal, nunca da linha de comando. */
class Backup {
  static final byte[] MAGIC = "INPETUB2".getBytes(StandardCharsets.US_ASCII);
  static final SecureRandom RANDOM = new SecureRandom();

  static byte[] random(int length) {
    byte[] b = new byte[length];
    RANDOM.nextBytes(b);
    return b;
  }

  static Cipher cipher(int mode, char[] password, byte[] salt, byte[] iv) throws Exception {
    var spec = new PBEKeySpec(password, salt, 310000, 256);
    byte[] key =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    spec.clearPassword();
    var c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
    Arrays.fill(key, (byte) 0);
    return c;
  }

  static void privateFile(Path p) throws Exception {
    if (Files.isSymbolicLink(p)) throw new IOException("Link simbólico recusado.");
    var acl = Files.getFileAttributeView(p, java.nio.file.attribute.AclFileAttributeView.class);
    if (acl != null)
      acl.setAcl(
          List.of(
              java.nio.file.attribute.AclEntry.newBuilder()
                  .setType(java.nio.file.attribute.AclEntryType.ALLOW)
                  .setPrincipal(
                      FileSystems.getDefault()
                          .getUserPrincipalLookupService()
                          .lookupPrincipalByName(System.getProperty("user.name")))
                  .setPermissions(EnumSet.allOf(java.nio.file.attribute.AclEntryPermission.class))
                  .build()));
    if (Files.getFileAttributeView(p, java.nio.file.attribute.PosixFileAttributeView.class) != null)
      Files.setPosixFilePermissions(
          p,
          java.nio.file.attribute.PosixFilePermissions.fromString(
              Files.isDirectory(p) ? "rwx------" : "rw-------"));
  }

  static void backup(Path target, char[] password) throws Exception {
    List<Path> sources = new ArrayList<>();
    for (String name : List.of(".env.local", ".secrets/mfa.key", ".secrets/accounts.properties")) {
      var p = Path.of(name);
      if (Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)) sources.add(p);
    }
    if (!Files.isRegularFile(Path.of("data/inpetu-v02.mv.db")))
      throw new IllegalStateException("Banco local não encontrado.");
    try (var files = Files.list(Path.of("data"))) {
      sources.addAll(
          files
              .filter(
                  p ->
                      p.getFileName().toString().startsWith("inpetu-v02.")
                          && Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
              .toList());
    }
    byte[] salt = random(16), iv = random(12);
    boolean complete = false;
    boolean created = false;
    try (var out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
      created = true;
      privateFile(target);
      out.write(MAGIC);
      out.write(salt);
      out.write(iv);
      try (var zip =
          new ZipOutputStream(
              new CipherOutputStream(out, cipher(Cipher.ENCRYPT_MODE, password, salt, iv)))) {
        for (Path p : sources) {
          if (Files.isSymbolicLink(p)) throw new IllegalStateException("Link simbólico recusado.");
          zip.putNextEntry(new ZipEntry(p.toString().replace('\\', '/')));
          Files.copy(p, zip);
          zip.closeEntry();
        }
      }
      complete = true;
    } finally {
      if (created && !complete) Files.deleteIfExists(target);
    }
  }

  static void restore(Path archive, char[] password) throws Exception {
    Path stage = Files.createTempDirectory("inpetu-restore-");
    privateFile(stage);
    try {
      // Autenticar todo o conteúdo antes de modificar os arquivos de destino.
      Path clear = stage.resolve("archive.zip");
      privateFile(clear = Files.createFile(clear));
      try (var input = Files.newInputStream(archive)) {
        if (!Arrays.equals(input.readNBytes(8), MAGIC))
          throw new IllegalArgumentException("Formato inválido.");
        byte[] salt = input.readNBytes(16), iv = input.readNBytes(12);
        try (var decrypted =
                new CipherInputStream(input, cipher(Cipher.DECRYPT_MODE, password, salt, iv));
            var out = Files.newOutputStream(clear)) {
          byte[] buffer = new byte[65536];
          long count = 0;
          int n;
          while ((n = decrypted.read(buffer)) != -1) {
            count += n;
            if (count > 1073741824L) throw new IOException("Backup excede 1 GiB.");
            out.write(buffer, 0, n);
          }
        }
      }
      List<String> names = new ArrayList<>();
      long total = 0;
      try (var zip = new ZipInputStream(Files.newInputStream(clear))) {
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
          String name = entry.getName();
          if (!Set.of(".env.local", ".secrets/mfa.key", ".secrets/accounts.properties")
                  .contains(name)
              && !name.matches("data/inpetu-v02[.][A-Za-z0-9.]+"))
            throw new IOException("Entrada inesperada.");
          if (names.contains(name) || names.size() > 100)
            throw new IOException("Entradas inválidas.");
          Path dest = stage.resolve("files").resolve(name).normalize();
          if (!dest.startsWith(stage.resolve("files"))) throw new IOException("Caminho inválido.");
          if (Files.exists(Path.of(name), LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Restaure em uma pasta nova: arquivos de destino já existem.");
          Files.createDirectories(dest.getParent());
          try (var out = Files.newOutputStream(dest, StandardOpenOption.CREATE_NEW)) {
            byte[] b = new byte[65536];
            int n;
            while ((n = zip.read(b)) != -1) {
              total += n;
              if (total > 1073741824L) throw new IOException("Conteúdo excede 1 GiB.");
              out.write(b, 0, n);
            }
          }
          privateFile(dest);
          names.add(name);
        }
      }
      if (!names.contains(".secrets/mfa.key")
          || !names.contains("data/inpetu-v02.mv.db")
          || !names.contains(".env.local")) throw new IOException("Backup incompleto.");
      for (String name : names) {
        Path dest = Path.of(name);
        Files.createDirectories(dest.getParent());
        privateFile(dest.getParent());
        Files.move(stage.resolve("files").resolve(name), dest);
        privateFile(dest);
      }
    } finally {
      try (var paths = Files.walk(stage)) {
        for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
      }
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2 || !Set.of("backup", "restore").contains(args[0])) {
      System.err.println("Uso: java scripts/Backup.java backup|restore arquivo.inpetu");
      System.exit(2);
    }
    Files.createDirectories(Path.of(".secrets"));
    privateFile(Path.of(".secrets"));
    try (var channel =
            FileChannel.open(
                Path.of(".secrets/runtime.lock"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        var lock = channel.tryLock()) {
      if (lock == null)
        throw new IllegalStateException("Pare o servidor antes de copiar ou restaurar o banco.");
      char[] password =
          System.console() != null
              ? System.console().readPassword("Senha do backup (mínimo 16 caracteres): ")
              : new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))
                  .readLine()
                  .toCharArray();
      try {
        if (password.length < 16)
          throw new IllegalArgumentException("Use pelo menos 16 caracteres.");
        if (args[0].equals("backup")) backup(Path.of(args[1]), password);
        else restore(Path.of(args[1]), password);
        System.out.println("Operação concluída. Preserve a senha em local privado.");
      } catch (Exception ex) {
        System.err.println(
            "Operação recusada: confira senha, integridade e destino. Nenhuma senha foi"
                + " registrada.");
        System.exit(1);
      } finally {
        Arrays.fill(password, '\0');
      }
    }
  }
}
