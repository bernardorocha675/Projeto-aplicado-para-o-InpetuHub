package br.com.inpetu.hub;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

// Banco já em uso (0.5.2 ou anterior): a migration só acrescenta os espaços do Hub que faltam.
class EspacosDoHubMigrationTest {
  @Test
  void existingSpacesAndReservationsAreKeptAndOnlyMissingHubSpacesAreAdded() throws Exception {
    String url = "jdbc:h2:mem:migracao_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE";
    Flyway.configure().dataSource(url, "sa", "").target("4").load().migrate();
    try (Connection c = DriverManager.getConnection(url, "sa", "");
        Statement s = c.createStatement()) {
      s.execute(
          "INSERT INTO espaco (nome_espaco, capacidade, disponibilidade, ativo) VALUES"
              + " ('auditório', 100, FALSE, TRUE), ('Sala criada pela equipe', 8, TRUE, TRUE)");
      Flyway.configure().dataSource(url, "sa", "").load().migrate();
      try (ResultSet r =
          s.executeQuery(
              "SELECT capacidade, disponibilidade, mobiliario FROM espaco WHERE LOWER(nome_espaco) = 'auditório'")) {
        assertTrue(r.next());
        assertEquals(100, r.getInt(1));
        assertFalse(r.getBoolean(2));
        assertNull(r.getString(3));
        assertFalse(r.next(), "Auditório duplicado");
      }
      try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM espaco")) {
        r.next();
        assertEquals(11, r.getInt(1));
      }
      try (ResultSet r =
          s.executeQuery("SELECT capacidade, mobiliario FROM espaco WHERE nome_espaco = 'Sala diária de trabalho'")) {
        assertTrue(r.next());
        assertEquals(18, r.getInt(1));
        assertEquals("14 cadeiras e 4 cadeiras brancas na mesa redonda", r.getString(2));
      }
    }
  }
}
