import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.SecureRandom;
import java.util.*;

/** Executado pelo JDK, sem Maven nem bibliotecas externas. Nunca imprime credenciais. */
class PrepareLocal {
  static final SecureRandom random = new SecureRandom();

  static String secret(int bytes) {
    byte[] b = new byte[bytes];
    random.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  static void protect(Path p) throws Exception {
    if (Files.isSymbolicLink(p))
      throw new IllegalStateException("Arquivo privado não pode ser link simbólico.");
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

  static void create(Path p, String value) throws Exception {
    if (!Files.exists(p, LinkOption.NOFOLLOW_LINKS)) {
      try {
        Files.writeString(p, value, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
      } catch (FileAlreadyExistsException ignored) {
      }
    }
    protect(p);
  }

  public static void main(String[] args) throws Exception {
    Path secrets = Path.of(".secrets");
    Files.createDirectories(secrets);
    protect(secrets);
    create(secrets.resolve("mfa.key"), secret(32) + "\n");
    String env = Files.readString(Path.of(".env.local.example"));
    env = env.replace("LOCAL_DB_PASSWORD=", "LOCAL_DB_PASSWORD=" + secret(32));
    create(Path.of(".env.local"), env);
    Files.createDirectories(Path.of("data"));
    protect(Path.of("data"));
    System.out.println(
        "Configuração privada preparada em .secrets/. Nenhuma credencial foi impressa.");
    System.out.println("Contas locais criadas no banco ao iniciar. Consulte ACESSOS.md; nao e necessario arquivo de senhas.");
  }
}
