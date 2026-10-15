package br.com.inpetu.hub.maintenance;

import static org.junit.jupiter.api.Assertions.*;

import br.com.inpetu.hub.config.PrivateAccountInitializer;
import br.com.inpetu.hub.entity.Colaborador;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class LocalAccessToolTest {
  @TempDir Path temp;
  final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
  static final String PASSWORD = "-Synthetic-only-Password!";

  Connection database() throws Exception {
    String url =
        "jdbc:h2:mem:access_"
            + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    Flyway.configure().dataSource(url, "sa", "").load().migrate();
    return DriverManager.getConnection(url, "sa", "");
  }

  List<Colaborador> definitions() throws Exception {
    Path file = temp.resolve("accounts.properties");
    Files.writeString(
        file,
        "account.1.name=Gestor sintético\naccount.1.login=synthetic-admin\naccount.1.password="
            + PASSWORD
            + "\n"
            + "account.1.role=ADMIN\n"
            + "account.2.name=Colaborador sintético\n"
            + "account.2.login=synthetic-user\n"
            + "account.2.password="
            + PASSWORD
            + "\naccount.2.role=COLABORADOR\n");
    return PrivateAccountInitializer.read(file, encoder);
  }

  Properties raw() {
    var p = new Properties();
    p.setProperty("account.1.password", PASSWORD);
    p.setProperty("account.2.password", PASSWORD);
    return p;
  }

  @Test
  void importsMissingAccountsWithoutErasingExistingData() throws Exception {
    try (var db = database()) {
      db.createStatement()
          .executeUpdate(
              "insert into colaborador(nome,email,login,senha,perfil,ativo) values('Outra"
                  + " conta','','unrelated-user','untouched','ADMIN',true)");
      var input = definitions();
      var before = LocalAccessTool.inspect(db, input, raw(), encoder);
      assertEquals(2, before.missing());
      db.setAutoCommit(false);
      LocalAccessTool.recover(db, input);
      db.commit();
      assertTrue(LocalAccessTool.inspect(db, input, raw(), encoder).matches());
      try (var rows = db.createStatement().executeQuery("select count(*) from colaborador")) {
        rows.next();
        assertEquals(3, rows.getInt(1));
      }
      try (var rows =
          db.createStatement()
              .executeQuery("select senha from colaborador where login='unrelated-user'")) {
        rows.next();
        assertEquals("untouched", rows.getString(1));
      }
      try (var rows =
          db.createStatement()
              .executeQuery(
                  "select count(*) from auditoria where evento='ACESSO_RECUPERADO_LOCAL'")) {
        rows.next();
        assertEquals(2, rows.getInt(1));
      }
    }
  }

  @Test
  void resetsOnlyRequestedPasswordsAndPreservesExistingRoleAndMfa() throws Exception {
    try (var db = database()) {
      var input = definitions();
      db.setAutoCommit(false);
      LocalAccessTool.recover(db, input);
      db.commit();
      db.createStatement()
          .executeUpdate(
              "update colaborador set"
                  + " perfil='COLABORADOR',ativo=false,mfa_secret='encrypted-fixture',mfa_recovery='hashed-fixture',mfa_counter=123,senha='different'"
                  + " where login='synthetic-admin'");
      var before = LocalAccessTool.inspect(db, input, raw(), encoder);
      assertEquals(1, before.inactive());
      assertEquals(1, before.different());
      LocalAccessTool.recover(db, input);
      db.commit();
      try (var rows =
          db.createStatement()
              .executeQuery("select * from colaborador where login='synthetic-admin'")) {
        rows.next();
        assertTrue(rows.getBoolean("ativo"));
        assertTrue(encoder.matches(PASSWORD, rows.getString("senha")));
        assertEquals("COLABORADOR", rows.getString("perfil"));
        assertEquals("encrypted-fixture", rows.getString("mfa_secret"));
        assertEquals("hashed-fixture", rows.getString("mfa_recovery"));
        assertEquals(123, rows.getLong("mfa_counter"));
        assertEquals(2, rows.getLong("security_version"));
      }
    }
  }

  @Test
  void partialFailureCanBeRolledBackTogetherWithAudit() throws Exception {
    try (var db = database()) {
      var input = definitions();
      db.setAutoCommit(false);
      LocalAccessTool.recover(db, input);
      db.commit();
      db.createStatement()
          .execute(
              "alter table auditoria add constraint forbid_recovery check(evento <>"
                  + " 'ACESSO_RECUPERADO_LOCAL') nocheck");
      assertThrows(SQLException.class, () -> LocalAccessTool.recover(db, input));
      db.rollback();
      try (var rows =
          db.createStatement()
              .executeQuery(
                  "select security_version from colaborador where login='synthetic-admin'")) {
        rows.next();
        assertEquals(1, rows.getLong(1));
      }
    }
  }
}
