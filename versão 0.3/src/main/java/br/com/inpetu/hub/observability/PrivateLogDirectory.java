package br.com.inpetu.hub.observability;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

/** Deve executar antes do Spring/Logback para proteger arquivos ativos e rotacionados. */
public final class PrivateLogDirectory {
  private PrivateLogDirectory() {}

  private static void protect(Path p, boolean directory) throws Exception {
    if (Files.isSymbolicLink(p)
        || (directory
            ? !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)
            : !Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)))
      throw new IllegalStateException("Caminho de log inválido ou link simbólico.");
    if (Files.getFileAttributeView(p, PosixFileAttributeView.class) != null)
      Files.setPosixFilePermissions(
          p, PosixFilePermissions.fromString(directory ? "rwx------" : "rw-------"));
    var acl = Files.getFileAttributeView(p, AclFileAttributeView.class);
    if (acl != null) {
      var entry =
          AclEntry.newBuilder()
              .setType(AclEntryType.ALLOW)
              .setPrincipal(Files.getOwner(p))
              .setPermissions(EnumSet.allOf(AclEntryPermission.class));
      if (directory) entry.setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT);
      acl.setAcl(List.of(entry.build()));
    }
  }

  public static void prepare() throws Exception {
    prepare(Path.of("logs"));
  }

  static void prepare(Path folder) throws Exception {
    if (!Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) {
      if (Files.getFileAttributeView(
              folder.toAbsolutePath().getParent(), PosixFileAttributeView.class)
          != null)
        Files.createDirectory(
            folder,
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
      else Files.createDirectory(folder);
    }
    protect(folder, true);
    try (var files = Files.list(folder)) {
      for (Path p : files.toList()) protect(p, false);
    }
    Path active = folder.resolve("inpetu.log");
    if (!Files.exists(active, LinkOption.NOFOLLOW_LINKS)) Files.createFile(active);
    protect(active, false);
  }
}
