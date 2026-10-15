package br.com.inpetu.hub.maintenance;

import br.com.inpetu.hub.config.PrivateAccountInitializer;
import br.com.inpetu.hub.entity.Colaborador;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Operação offline, apenas H2 local, sem Spring, HTTP ou impressão de identificadores privados. */
public final class LocalAccessTool {
  private LocalAccessTool() {}

  public record Summary(int total, int missing, int inactive, int different) {
    public boolean matches() {
      return missing == 0 && inactive == 0 && different == 0;
    }
  }

  static Properties properties(Path file) throws IOException {
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > 65536)
      throw new IOException("Arquivo privado ausente ou inválido.");
    var p = new Properties();
    try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      p.load(reader);
    }
    return p;
  }

  static String setting(Properties p, String name, String fallback) {
    String value = System.getProperty(name);
    if (value == null) value = System.getenv(name);
    return value == null ? p.getProperty(name, fallback) : value;
  }

  public static Summary inspect(
      Connection db, List<Colaborador> input, Properties raw, PasswordEncoder encoder)
      throws SQLException {
    int missing = 0, inactive = 0, different = 0;
    try (var q = db.prepareStatement("select senha,ativo from colaborador where login=?")) {
      for (int i = 0; i < input.size(); i++) {
        q.setString(1, input.get(i).login);
        try (var row = q.executeQuery()) {
          if (!row.next()) {
            missing++;
            continue;
          }
          if (!row.getBoolean("ativo")) inactive++;
          if (!encoder.matches(
              raw.getProperty("account." + (i + 1) + ".password"), row.getString("senha")))
            different++;
        }
      }
    }
    return new Summary(input.size(), missing, inactive, different);
  }

  /**
   * Mesma transação para contas e auditoria. Não remove pessoas nem troca perfil/MFA existentes.
   */
  public static void recover(Connection db, List<Colaborador> input) throws SQLException {
    if (db.getAutoCommit()) throw new IllegalStateException("Recuperação exige transação.");
    try (var find = db.prepareStatement("select id_colaborador from colaborador where login=?");
        var update =
            db.prepareStatement(
                "update colaborador set senha=?,ativo=true,security_version=security_version+1"
                    + " where id_colaborador=?");
        var insert =
            db.prepareStatement(
                "insert into"
                    + " colaborador(nome,email,login,senha,perfil,ativo,security_version,mfa_counter)"
                    + " values(?,'',?,?,?,true,1,-1)",
                Statement.RETURN_GENERATED_KEYS);
        var audit =
            db.prepareStatement(
                "insert into auditoria(instante,evento,alvo,alvo_id,resultado,detalhes)"
                    + " values(CURRENT_TIMESTAMP,'ACESSO_RECUPERADO_LOCAL','colaboradores',?,'OK','Recuperação"
                    + " offline confirmada pelo operador; perfil e MFA existentes"
                    + " preservados.')")) {
      for (Colaborador item : input) {
        Long id = null;
        find.setString(1, item.login);
        try (var row = find.executeQuery()) {
          if (row.next()) id = row.getLong(1);
        }
        if (id != null) {
          update.setString(1, item.senha);
          update.setLong(2, id);
          update.executeUpdate();
        } else {
          insert.setString(1, item.nome);
          insert.setString(2, item.login);
          insert.setString(3, item.senha);
          insert.setString(4, item.perfil.name());
          insert.executeUpdate();
          try (var keys = insert.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("ID ausente.");
            id = keys.getLong(1);
          }
        }
        audit.setLong(1, id);
        audit.executeUpdate();
      }
      try (var mark =
          db.prepareStatement("update bootstrap_seguro set concluido=true where id=1")) {
        if (mark.executeUpdate() != 1) throw new SQLException("Marcador ausente.");
      }
    }
  }

  public static int run(String mode) {
    try {
      Path data = Path.of("data"), database = data.resolve("inpetu-v02.mv.db");
      if (Files.isSymbolicLink(data) || !Files.isRegularFile(database, LinkOption.NOFOLLOW_LINKS)) {
        System.out.println(
            "Banco local não encontrado. Abra a pasta versão 03 já utilizada. Não apague data/."
                + " Leia ACESSOS.md.");
        return 1;
      }
      Properties env = properties(Path.of(".env.local"));
      Path file = Path.of(setting(env, "ACCOUNTS_FILE", ".secrets/accounts.properties"));
      var encoder = new BCryptPasswordEncoder(12);
      var input = PrivateAccountInitializer.read(file, encoder);
      var raw = properties(file);
      boolean recover = mode.equals("--recuperar-acessos");
      Class.forName("org.h2.Driver");
      String url =
          "jdbc:h2:file:./data/inpetu-v02;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE;IFEXISTS=TRUE"
              + (recover ? "" : ";ACCESS_MODE_DATA=r");
      try (var db = DriverManager.getConnection(url, "sa", setting(env, "LOCAL_DB_PASSWORD", ""))) {
        db.setAutoCommit(false);
        try {
          Summary s = inspect(db, input, raw, encoder);
          System.out.printf(
              "Contas no arquivo privado: %d%nAusentes no banco: %d%nInativas: %d%nSenhas"
                  + " diferentes: %d%n",
              s.total(), s.missing(), s.inactive(), s.different());
          if (!recover) {
            System.out.println(
                s.matches()
                    ? "Os acessos do arquivo conferem com este banco. Use o login exato do arquivo,"
                          + " não o nome da pessoa. Confira espaços, maiúsculas e minúsculas."
                    : "O arquivo privado não corresponde aos acessos ativos deste banco. Consulte"
                          + " ACESSOS.md para recuperar sem apagar dados.");
            db.rollback();
            return s.matches() ? 0 : 2;
          }
          System.out.println(
              "Esta operação importa contas ausentes e redefine a senha/reativa as contas do"
                  + " arquivo privado. Não apaga cadastros ou reservas. Perfil e MFA de contas"
                  + " existentes permanecem. Sessões antigas serão revogadas.");
          System.out.println(
              "Faça antes um backup cifrado. Para confirmar que fez backup e deseja continuar,"
                  + " digite: RECUPERAR COM BACKUP");
          Console console = System.console();
          String answer =
              console != null
                  ? console.readLine()
                  : new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))
                      .readLine();
          if (!"RECUPERAR COM BACKUP".equals(answer)) {
            db.rollback();
            System.out.println("Cancelado. Nenhuma conta foi alterada.");
            return 3;
          }
          recover(db, input);
          db.commit();
          System.out.println(
              "Acessos recuperados e operação auditada. Inicie normalmente e use os logins e senhas"
                  + " do arquivo privado. Nenhuma credencial foi impressa.");
          return 0;
        } catch (Exception ex) {
          db.rollback();
          throw ex;
        }
      }
    } catch (Exception ex) {
      System.out.println(
          "Não foi possível conferir/recuperar os acessos. Pare o servidor, confira a pasta,"
              + " .env.local e o arquivo privado conforme ACESSOS.md. Detalhes sensíveis não foram"
              + " impressos.");
      return 1;
    }
  }
}
